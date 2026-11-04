package br.com.ewaste.solicitacao.web;

import br.com.ewaste.solicitacao.dominio.Solicitacao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Data e hora da retirada, no horário de São Paulo (campo datetime-local, sem fuso). */
public class AgendamentoForm {

  @NotNull(message = "Informe a data e a hora da retirada.")
  @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
  private LocalDateTime agendadaPara;

  public AgendamentoForm() {}

  /**
   * Compara no horário de São Paulo, o mesmo usado para gravar. Um {@code @Future} aqui usaria o
   * fuso da JVM, que no servidor costuma ser UTC (3 horas à frente).
   */
  @AssertTrue(message = "A data da retirada deve estar no futuro.")
  public boolean isNoFuturo() {
    return agendadaPara == null || paraInstant().isAfter(Instant.now());
  }

  /** Preenche com a data atual do agendamento (reagendar). */
  public AgendamentoForm(Instant atual) {
    this.agendadaPara = atual == null ? null : LocalDateTime.ofInstant(atual, Solicitacao.FUSO);
  }

  public Instant paraInstant() {
    return agendadaPara.atZone(Solicitacao.FUSO).toInstant();
  }

  public LocalDateTime getAgendadaPara() {
    return agendadaPara;
  }

  public void setAgendadaPara(LocalDateTime agendadaPara) {
    this.agendadaPara = agendadaPara;
  }
}
