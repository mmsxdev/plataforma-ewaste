package br.com.ewaste.roteamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.roteamento.api.AlertaConsulta;
import br.com.ewaste.roteamento.api.AlertaResumo;
import br.com.ewaste.shared.events.AlertasEnviadosEvent;
import br.com.ewaste.shared.events.SemColetoraNaRegiaoEvent;
import br.com.ewaste.shared.events.SolicitacaoCriadaEvent;
import br.com.ewaste.solicitacao.EventosCapturados;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * RoteamentoListener contra o PostgreSQL real: grava os alertas e publica o evento certo. Quem é
 * apto vem de um mock de ColetorasAptas; a posição de cada coletora vem da tabela empresa.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, EventosCapturados.class})
class RoteamentoIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transacao;
  @Autowired ApplicationEventPublisher publicador;
  @Autowired AlertaConsulta alertas;
  @Autowired EventosCapturados eventos;
  @MockitoBean ColetorasAptas aptas;

  Long geradora;
  Long mooca; // ~5,8 km da solicitação, raio 50
  Long rio; // ~356 km da solicitação, raio 40

  @BeforeEach
  void prepararBanco() {
    jdbc.execute(
        "truncate alerta_solicitacao, solicitacao_item, solicitacao, usuario, empresa"
            + " restart identity cascade");
    eventos.limpar();
    geradora = inserirEmpresa("GERADORA", "11222333000181", -23.5614, -46.6559, null);
    mooca = inserirEmpresa("COLETORA", "22333444000181", -23.5505, -46.6000, 50);
    rio = inserirEmpresa("COLETORA", "33444555000181", -22.8800, -43.2500, 40);
  }

  @Test
  void alertaSoAsAptasNoRaioEGravaADistancia() {
    when(aptas.listarAptas()).thenReturn(List.of(mooca, rio));
    Long id = criarSolicitacaoNaPaulista();

    assertThat(alertas.listarDaColetora(mooca)).hasSize(1);
    AlertaResumo alerta = alertas.buscar(id, mooca).orElseThrow();
    assertThat(alerta.distanciaKm()).isBetween(new BigDecimal("5.70"), new BigDecimal("5.95"));
    assertThat(alerta.enviadoEm()).isNotNull();
    assertThat(alertas.foiAlertada(id, rio)).isFalse();
    assertThat(alertas.contarAlertas(id)).isEqualTo(1);
    assertThat(eventos.doTipo(AlertasEnviadosEvent.class))
        .containsExactly(new AlertasEnviadosEvent(id, List.of(mooca)));
    assertThat(eventos.doTipo(SemColetoraNaRegiaoEvent.class)).isEmpty();
    assertThat(statusDa(id)).isEqualTo("ABERTA");
  }

  @Test
  void semColetoraAptaNoRaioPublicaSemColetoraEContinuaAberta() {
    when(aptas.listarAptas()).thenReturn(List.of(rio)); // a única apta está fora do raio
    Long id = criarSolicitacaoNaPaulista();

    assertThat(alertas.contarAlertas(id)).isZero();
    assertThat(eventos.doTipo(SemColetoraNaRegiaoEvent.class))
        .containsExactly(new SemColetoraNaRegiaoEvent(id, geradora));
    assertThat(eventos.doTipo(AlertasEnviadosEvent.class)).isEmpty();
    assertThat(statusDa(id)).isEqualTo("ABERTA");
  }

  @Test
  void eventoRepetidoNaoDuplicaAlertas() {
    when(aptas.listarAptas()).thenReturn(List.of(mooca));
    Long id = criarSolicitacaoNaPaulista();

    publicarDentroDeTransacao(new SolicitacaoCriadaEvent(id, geradora));

    assertThat(alertas.contarAlertas(id)).isEqualTo(1);
    assertThat(eventos.doTipo(AlertasEnviadosEvent.class)).hasSize(1);
  }

  @Test
  void solicitacaoQueNaoEstaAbertaNaoEhRoteada() {
    when(aptas.listarAptas()).thenReturn(List.of(mooca));
    Long id = inserirSolicitacao("CANCELADA");

    publicarDentroDeTransacao(new SolicitacaoCriadaEvent(id, geradora));

    assertThat(alertas.contarAlertas(id)).isZero();
    assertThat(eventos.doTipo(AlertasEnviadosEvent.class)).isEmpty();
  }

  // ---- Auxiliares ----

  /** Simula a criação: insere a solicitação e publica o evento na mesma transação, que confirma. */
  private Long criarSolicitacaoNaPaulista() {
    return transacao.execute(
        status -> {
          Long id = inserirSolicitacao("ABERTA");
          publicador.publishEvent(new SolicitacaoCriadaEvent(id, geradora));
          return id;
        });
  }

  private void publicarDentroDeTransacao(Object evento) {
    transacao.executeWithoutResult(status -> publicador.publishEvent(evento));
  }

  private Long inserirSolicitacao(String status) {
    return jdbc.queryForObject(
        "insert into solicitacao (geradora_id, peso_estimado_kg, cidade, uf, latitude, longitude,"
            + " status) values (?, 250, 'São Paulo', 'SP', -23.5614, -46.6559, ?) returning id",
        Long.class,
        geradora,
        status);
  }

  private String statusDa(Long id) {
    return jdbc.queryForObject("select status from solicitacao where id = ?", String.class, id);
  }

  private Long inserirEmpresa(String tipo, String cnpj, double lat, double lon, Integer raio) {
    return jdbc.queryForObject(
        "insert into empresa (tipo, razao_social, cnpj, cidade, uf, latitude, longitude,"
            + " raio_atendimento_km) values (?, 'Empresa ' || ?, ?, 'Cidade', 'SP', ?, ?, ?)"
            + " returning id",
        Long.class,
        tipo,
        cnpj,
        cnpj,
        lat,
        lon,
        raio);
  }
}
