package br.com.ewaste.solicitacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SolicitacaoTest {

  static final Instant AMANHA = Instant.now().plus(1, ChronoUnit.DAYS);
  static final Instant DEPOIS = Instant.now().plus(3, ChronoUnit.DAYS);

  @Test
  void nasceAbertaSemColetora() {
    Solicitacao s = nova();

    assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.ABERTA);
    assertThat(s.getColetoraId()).isNull();
    assertThat(s.getCriadaEm()).isNotNull();
  }

  @Test
  void pesoZeroOuNegativoNaoCriaSolicitacao() {
    assertThatThrownBy(() -> comPeso("0")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> comPeso("-1")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void agendarUmaAceitaPassaParaAgendada() {
    Solicitacao s = nova();
    colocarEm(s, StatusSolicitacao.ACEITA);

    s.agendar(AMANHA);

    assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.AGENDADA);
    assertThat(s.getAgendadaPara()).isEqualTo(AMANHA);
  }

  @Test
  void reagendarSoTrocaAData() {
    Solicitacao s = nova();
    colocarEm(s, StatusSolicitacao.ACEITA);
    s.agendar(AMANHA);

    s.agendar(DEPOIS);

    assertThat(s.getStatus()).isEqualTo(StatusSolicitacao.AGENDADA);
    assertThat(s.getAgendadaPara()).isEqualTo(DEPOIS);
  }

  @Test
  void naoAgendaAntesDoAceiteNemDepoisDaColeta() {
    Solicitacao aberta = nova();
    assertThatThrownBy(() -> aberta.agendar(AMANHA)).isInstanceOf(TransicaoInvalidaException.class);

    Solicitacao coletada = nova();
    colocarEm(coletada, StatusSolicitacao.COLETADA);
    assertThatThrownBy(() -> coletada.agendar(AMANHA))
        .isInstanceOf(TransicaoInvalidaException.class);
    assertThat(coletada.getAgendadaPara()).isNull();
  }

  private static Solicitacao nova() {
    return comPeso("100");
  }

  private static Solicitacao comPeso(String peso) {
    return Solicitacao.abrir(
        1L, null, new BigDecimal(peso), false, 0, null, "São Paulo", "SP", -23.5, -46.6);
  }

  /** O aceite é feito por SQL no repositório; aqui o status é posicionado direto. */
  private static void colocarEm(Solicitacao s, StatusSolicitacao status) {
    ReflectionTestUtils.setField(s, "status", status);
  }
}
