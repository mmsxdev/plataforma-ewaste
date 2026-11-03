package br.com.ewaste.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.shared.events.AlertasEnviadosEvent;
import br.com.ewaste.shared.events.SolicitacaoAceitaEvent;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.dominio.TipoResiduo;
import br.com.ewaste.solicitacao.servico.AceiteService;
import br.com.ewaste.solicitacao.servico.ResultadoAceite;
import br.com.ewaste.solicitacao.servico.SolicitacaoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Regras lógicas do plano do projeto, com o fluxo real (serviço -> evento -> roteamento) e o
 * PostgreSQL real.
 *
 * <ul>
 *   <li>b = solicitação criada · h = coletora homologada (apta) dentro do raio · c = coletora
 *       alertada
 *   <li>d = coletora aceita · i = rastreio iniciado (gatilho: SolicitacaoAceitaEvent, ouvido pelo
 *       Dev 4)
 * </ul>
 *
 * Quem é apto vem de um mock de ColetorasAptas; a posição de cada coletora vem da tabela empresa.
 * Assim o teste não depende do seed de outros módulos.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, EventosCapturados.class})
class RegrasR1R2IT {

  @Autowired JdbcTemplate jdbc;
  @Autowired SolicitacaoService solicitacoes;
  @Autowired AceiteService aceite;
  @Autowired EventosCapturados eventos;
  @MockitoBean ColetorasAptas aptas;

  Long geradora;
  Long aptaNoRaio; // Mooca: ~5,8 km do local, raio 50 km, homologada
  Long naoHomologadaNoRaio; // Bela Vista: ~1 km do local, raio 50 km, NÃO homologada
  Long aptaForaDoRaio; // Rio de Janeiro: ~356 km do local, raio 40 km, homologada

  @BeforeEach
  void prepararBanco() {
    jdbc.execute(
        "truncate alerta_solicitacao, solicitacao_item, solicitacao, usuario, empresa"
            + " restart identity cascade");
    eventos.limpar();
    geradora = inserirEmpresa("GERADORA", "11222333000181", -23.5614, -46.6559, null);
    aptaNoRaio = inserirEmpresa("COLETORA", "22333444000181", -23.5505, -46.6000, 50);
    naoHomologadaNoRaio = inserirEmpresa("COLETORA", "44555666000181", -23.5560, -46.6480, 50);
    aptaForaDoRaio = inserirEmpresa("COLETORA", "33444555000181", -22.8800, -43.2500, 40);

    Set<Long> homologadas = Set.of(aptaNoRaio, aptaForaDoRaio);
    when(aptas.listarAptas()).thenReturn(List.copyOf(homologadas));
    when(aptas.isApta(anyLong())).thenAnswer(i -> homologadas.contains(i.<Long>getArgument(0)));
  }

  /** R1: (b ∧ h) → c. Toda coletora apta dentro do raio é alertada. */
  @Nested
  class R1 {

    @Test
    void solicitacaoCriadaEAptaNoRaioEntaoAlertada() {
      Long id = criarSolicitacao(); // b

      // c: o alerta fica gravado e o evento leva a coletora
      assertThat(alertaGravado(id, aptaNoRaio)).isTrue();
      assertThat(eventos.doTipo(AlertasEnviadosEvent.class))
          .singleElement()
          .satisfies(e -> assertThat(e.coletoraIds()).contains(aptaNoRaio));
    }

    @Test
    void naoHomologadaDentroDoRaioNaoEhAlertada() {
      Long id = criarSolicitacao(); // b ∧ ¬h

      assertThat(alertaGravado(id, naoHomologadaNoRaio)).isFalse();
      assertThat(eventos.doTipo(AlertasEnviadosEvent.class))
          .singleElement()
          .satisfies(e -> assertThat(e.coletoraIds()).doesNotContain(naoHomologadaNoRaio));
    }

    @Test
    void aptaForaDoRaioNaoEhAlertada() {
      Long id = criarSolicitacao(); // b ∧ ¬h (fora da região)

      assertThat(alertaGravado(id, aptaForaDoRaio)).isFalse();
      assertThat(eventos.doTipo(AlertasEnviadosEvent.class))
          .singleElement()
          .satisfies(e -> assertThat(e.coletoraIds()).containsExactly(aptaNoRaio));
    }
  }

  /** R2: (c ∧ d) → i. Coletora alertada que aceita dispara o início do rastreio. */
  @Nested
  class R2 {

    @Test
    void alertadaQueAceitaDisparaORastreio() {
      Long id = criarSolicitacao();
      assertThat(alertaGravado(id, aptaNoRaio)).isTrue(); // c

      assertThat(aceite.aceitar(id, aptaNoRaio)).isEqualTo(ResultadoAceite.ACEITO); // d

      // i: o evento que o rastreio (Dev 4) ouve para registrar o início da coleta
      assertThat(eventos.doTipo(SolicitacaoAceitaEvent.class))
          .containsExactly(new SolicitacaoAceitaEvent(id, geradora, aptaNoRaio));
    }

    @Test
    void semAlertaNaoHaAceiteNemRastreio() {
      Long id = criarSolicitacao(); // ¬c para a não homologada

      assertThatThrownBy(() -> aceite.aceitar(id, naoHomologadaNoRaio))
          .isInstanceOf(SolicitacaoNaoEncontradaException.class);
      assertThat(eventos.doTipo(SolicitacaoAceitaEvent.class)).isEmpty();
    }
  }

  // ---- Auxiliares ----

  /** b: a geradora cria a solicitação pelo serviço real (Av. Paulista). */
  private Long criarSolicitacao() {
    Solicitacao s =
        Solicitacao.abrir(
            geradora,
            "HDs",
            new BigDecimal("250"),
            true,
            5,
            "Av. Paulista, 1000",
            "São Paulo",
            "SP",
            -23.5614,
            -46.6559);
    s.adicionarItem(TipoResiduo.MIDIA_ARMAZENAMENTO, 5, new BigDecimal("10"));
    return solicitacoes.criar(s);
  }

  private boolean alertaGravado(Long solicitacao, Long coletora) {
    return jdbc.queryForObject(
        "select exists (select 1 from alerta_solicitacao"
            + " where solicitacao_id = ? and coletora_id = ?)",
        Boolean.class,
        solicitacao,
        coletora);
  }

  private Long inserirEmpresa(String tipo, String cnpj, double lat, double lon, Integer raio) {
    return jdbc.queryForObject(
        "insert into empresa (tipo, razao_social, cnpj, cidade, uf, latitude, longitude,"
            + " raio_atendimento_km) values (?, 'Empresa ' || ?, ?, 'São Paulo', 'SP', ?, ?, ?)"
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
