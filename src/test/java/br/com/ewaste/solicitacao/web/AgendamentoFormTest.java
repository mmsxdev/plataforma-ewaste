package br.com.ewaste.solicitacao.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ewaste.solicitacao.dominio.Solicitacao;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * "No futuro" é decidido no horário de São Paulo (o mesmo usado para gravar), não no fuso da JVM:
 * em servidor/CI a JVM costuma estar em UTC, 3 horas à frente.
 */
class AgendamentoFormTest {

  static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

  TimeZone fusoOriginal;

  @BeforeEach
  void jvmEmUtc() {
    fusoOriginal = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @AfterEach
  void restaurarFuso() {
    TimeZone.setDefault(fusoOriginal);
  }

  @Test
  void umaHoraDepoisNoHorarioDeSaoPauloEhAceita() {
    assertThat(validar(LocalDateTime.now(Solicitacao.FUSO).plusHours(1))).isEmpty();
  }

  @Test
  void umaHoraAntesNoHorarioDeSaoPauloEhRecusada() {
    assertThat(validar(LocalDateTime.now(Solicitacao.FUSO).minusHours(1)))
        .extracting(ConstraintViolation::getMessage)
        .containsExactly("A data da retirada deve estar no futuro.");
  }

  private static Set<ConstraintViolation<AgendamentoForm>> validar(LocalDateTime dataHora) {
    var form = new AgendamentoForm();
    form.setAgendadaPara(dataHora);
    return VALIDADOR.validate(form);
  }
}
