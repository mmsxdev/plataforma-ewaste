package br.com.ewaste.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.shared.events.SolicitacaoAceitaEvent;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.servico.AceiteService;
import br.com.ewaste.solicitacao.servico.ResultadoAceite;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Aceite atômico (RF04 / RN04) contra o PostgreSQL real.
 *
 * <p>SEM {@code @Transactional} na classe: cada thread precisa da sua própria transação, como duas
 * requisições de verdade. Os eventos são contados pelo {@link EventosCapturados}, que enxerga as
 * threads do pool (o {@code @RecordApplicationEvents} só veria a thread do teste).
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, EventosCapturados.class})
class AceiteConcorrenteIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired AceiteService aceite;
  @Autowired EventosCapturados eventos;
  @MockitoBean ColetorasAptas aptas;

  Long geradora;
  Long coletoraA;
  Long coletoraB;
  Long solicitacao;

  @BeforeEach
  void prepararBanco() {
    jdbc.execute(
        "truncate alerta_solicitacao, solicitacao_item, solicitacao, usuario, empresa"
            + " restart identity cascade");
    eventos.limpar();
    when(aptas.isApta(anyLong())).thenReturn(true);
    geradora = inserirEmpresa("GERADORA", "11222333000181");
    coletoraA = inserirEmpresa("COLETORA", "22333444000181");
    coletoraB = inserirEmpresa("COLETORA", "33444555000181");
    solicitacao =
        jdbc.queryForObject(
            "insert into solicitacao (geradora_id, peso_estimado_kg, cidade, uf, latitude,"
                + " longitude) values (?, 250, 'São Paulo', 'SP', -23.56, -46.65) returning id",
            Long.class,
            geradora);
    alertar(coletoraA);
    alertar(coletoraB);
  }

  // ---- Concorrência ----

  @RepeatedTest(10)
  void duasColetorasAoMesmoTempoExatamenteUmaVence() throws Exception {
    var largada = new CountDownLatch(1);
    Map<Long, ResultadoAceite> resultados;
    try (var pool = Executors.newFixedThreadPool(2)) {
      Future<ResultadoAceite> a = pool.submit(() -> aceitarNaLargada(largada, coletoraA));
      Future<ResultadoAceite> b = pool.submit(() -> aceitarNaLargada(largada, coletoraB));
      largada.countDown(); // solta as duas threads juntas
      resultados = Map.of(coletoraA, a.get(), coletoraB, b.get());
    }

    assertThat(resultados.values())
        .containsExactlyInAnyOrder(ResultadoAceite.ACEITO, ResultadoAceite.JA_ACEITA);
    Long vencedora = resultados.get(coletoraA) == ResultadoAceite.ACEITO ? coletoraA : coletoraB;

    Map<String, Object> linha =
        jdbc.queryForMap(
            "select status, coletora_id, aceita_em, version from solicitacao where id = ?",
            solicitacao);
    assertThat(linha.get("status")).isEqualTo("ACEITA");
    assertThat(linha.get("coletora_id")).isEqualTo(vencedora);
    assertThat(linha.get("aceita_em")).isNotNull();
    assertThat(linha.get("version")).isEqualTo(1L);
    assertThat(eventos.doTipo(SolicitacaoAceitaEvent.class))
        .containsExactly(new SolicitacaoAceitaEvent(solicitacao, geradora, vencedora));
  }

  // ---- Regras do aceite ----

  @Test
  void quemChegaDepoisVeJaAceitaENaoPublicaEvento() {
    assertThat(aceite.aceitar(solicitacao, coletoraA)).isEqualTo(ResultadoAceite.ACEITO);
    assertThat(aceite.aceitar(solicitacao, coletoraB)).isEqualTo(ResultadoAceite.JA_ACEITA);

    assertThat(coletoraNoBanco()).isEqualTo(coletoraA);
    assertThat(eventos.doTipo(SolicitacaoAceitaEvent.class)).hasSize(1);
  }

  @Test
  void coletoraNaoAlertadaNaoAceita() {
    Long semAlerta = inserirEmpresa("COLETORA", "44555666000181");

    assertThatThrownBy(() -> aceite.aceitar(solicitacao, semAlerta))
        .isInstanceOf(SolicitacaoNaoEncontradaException.class);
    assertThat(statusNoBanco()).isEqualTo("ABERTA");
  }

  @Test
  void coletoraComHomologacaoInativaNaoAceita() {
    when(aptas.isApta(coletoraA)).thenReturn(false);

    assertThat(aceite.aceitar(solicitacao, coletoraA))
        .isEqualTo(ResultadoAceite.HOMOLOGACAO_INATIVA);
    assertThat(statusNoBanco()).isEqualTo("ABERTA");
    assertThat(eventos.doTipo(SolicitacaoAceitaEvent.class)).isEmpty();
  }

  @Test
  void solicitacaoCanceladaNaoPodeSerAceita() {
    jdbc.update("update solicitacao set status = 'CANCELADA' where id = ?", solicitacao);

    assertThat(aceite.aceitar(solicitacao, coletoraA)).isEqualTo(ResultadoAceite.JA_ACEITA);
    assertThat(statusNoBanco()).isEqualTo("CANCELADA");
  }

  // ---- Auxiliares ----

  private ResultadoAceite aceitarNaLargada(CountDownLatch largada, Long coletora)
      throws InterruptedException {
    largada.await();
    return aceite.aceitar(solicitacao, coletora);
  }

  private void alertar(Long coletora) {
    jdbc.update(
        "insert into alerta_solicitacao (solicitacao_id, coletora_id, distancia_km)"
            + " values (?, ?, 5)",
        solicitacao,
        coletora);
  }

  private String statusNoBanco() {
    return jdbc.queryForObject(
        "select status from solicitacao where id = ?", String.class, solicitacao);
  }

  private Long coletoraNoBanco() {
    return jdbc.queryForObject(
        "select coletora_id from solicitacao where id = ?", Long.class, solicitacao);
  }

  private Long inserirEmpresa(String tipo, String cnpj) {
    return jdbc.queryForObject(
        "insert into empresa (tipo, razao_social, cnpj, cidade, uf)"
            + " values (?, 'Empresa ' || ?, ?, 'São Paulo', 'SP') returning id",
        Long.class,
        tipo,
        cnpj,
        cnpj);
  }
}
