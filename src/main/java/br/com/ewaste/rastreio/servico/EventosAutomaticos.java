package br.com.ewaste.rastreio.servico;

import br.com.ewaste.shared.events.AlertasEnviadosEvent;
import br.com.ewaste.shared.events.SolicitacaoAceitaEvent;
import br.com.ewaste.shared.events.SolicitacaoAgendadaEvent;
import br.com.ewaste.shared.events.SolicitacaoCriadaEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class EventosAutomaticos {
  private final JdbcTemplate jdbc;

  EventosAutomaticos(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void criada(SolicitacaoCriadaEvent evento) {
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo, descricao) VALUES (?, 'CRIADA', 'Solicitação criada') ON CONFLICT DO NOTHING",
        evento.solicitacaoId());
  }

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void alertas(AlertasEnviadosEvent evento) {
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo, descricao) VALUES (?, 'ALERTAS_ENVIADOS', ?)",
        evento.solicitacaoId(),
        "Coletoras alertadas: " + evento.coletoraIds().size());
  }

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void aceita(SolicitacaoAceitaEvent evento) {
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo, descricao) VALUES (?, 'ACEITA', 'Coleta aceita') ON CONFLICT DO NOTHING",
        evento.solicitacaoId());
  }

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void agendada(SolicitacaoAgendadaEvent evento) {
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo, descricao) VALUES (?, 'AGENDADA', ?)",
        evento.solicitacaoId(),
        "Retirada agendada para " + evento.agendadaPara());
  }
}
