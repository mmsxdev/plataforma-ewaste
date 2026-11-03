package br.com.ewaste.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.shared.events.SolicitacaoAgendadaEvent;
import br.com.ewaste.solicitacao.api.SolicitacaoConsulta;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.servico.AgendamentoService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Agendamento da retirada pela coletora vencedora, contra o PostgreSQL real. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, EventosCapturados.class})
class AgendamentoIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired AgendamentoService agendamento;
  @Autowired SolicitacaoConsulta consulta;
  @Autowired EventosCapturados eventos;

  static final Instant AMANHA =
      Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);

  Long geradora;
  Long vencedora;
  Long outra;
  Long solicitacao;

  @BeforeEach
  void prepararBanco() {
    jdbc.execute(
        "truncate alerta_solicitacao, solicitacao_item, solicitacao, usuario, empresa"
            + " restart identity cascade");
    eventos.limpar();
    geradora = inserirEmpresa("GERADORA", "11222333000181");
    vencedora = inserirEmpresa("COLETORA", "22333444000181");
    outra = inserirEmpresa("COLETORA", "33444555000181");
    solicitacao =
        jdbc.queryForObject(
            "insert into solicitacao (geradora_id, peso_estimado_kg, cidade, uf, latitude,"
                + " longitude, status, coletora_id, aceita_em)"
                + " values (?, 250, 'São Paulo', 'SP', -23.5, -46.6, 'ACEITA', ?, now())"
                + " returning id",
            Long.class,
            geradora,
            vencedora);
  }

  @Test
  void vencedoraAgendaEAGeradoraVeAData() {
    agendamento.agendar(solicitacao, vencedora, AMANHA);

    assertThat(statusNoBanco()).isEqualTo("AGENDADA");
    var resumo = consulta.buscar(solicitacao).orElseThrow();
    assertThat(resumo.agendadaPara().toInstant()).isEqualTo(AMANHA);
    assertThat(eventos.doTipo(SolicitacaoAgendadaEvent.class))
        .containsExactly(
            new SolicitacaoAgendadaEvent(
                solicitacao, geradora, AMANHA.atZone(Solicitacao.FUSO).toOffsetDateTime()));
    assertThat(agendamento.listarAceitas(vencedora)).hasSize(1);
  }

  @Test
  void reagendarMantemAgendadaETrocaAData() {
    agendamento.agendar(solicitacao, vencedora, AMANHA);
    Instant depois = AMANHA.plus(2, ChronoUnit.DAYS);

    agendamento.agendar(solicitacao, vencedora, depois);

    assertThat(statusNoBanco()).isEqualTo("AGENDADA");
    assertThat(consulta.buscar(solicitacao).orElseThrow().agendadaPara().toInstant())
        .isEqualTo(depois);
    assertThat(eventos.doTipo(SolicitacaoAgendadaEvent.class)).hasSize(2);
  }

  @Test
  void outraColetoraNaoAgenda() {
    assertThatThrownBy(() -> agendamento.agendar(solicitacao, outra, AMANHA))
        .isInstanceOf(SolicitacaoNaoEncontradaException.class);
    assertThat(statusNoBanco()).isEqualTo("ACEITA");
    assertThat(agendamento.listarAceitas(outra)).isEmpty();
  }

  @Test
  void dataNoPassadoERecusada() {
    Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);

    assertThatThrownBy(() -> agendamento.agendar(solicitacao, vencedora, ontem))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(statusNoBanco()).isEqualTo("ACEITA");
    assertThat(eventos.doTipo(SolicitacaoAgendadaEvent.class)).isEmpty();
  }

  private String statusNoBanco() {
    return jdbc.queryForObject(
        "select status from solicitacao where id = ?", String.class, solicitacao);
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
