package br.com.ewaste.solicitacao;

import br.com.ewaste.shared.events.AlertasEnviadosEvent;
import br.com.ewaste.shared.events.SemColetoraNaRegiaoEvent;
import br.com.ewaste.shared.events.SolicitacaoAceitaEvent;
import br.com.ewaste.shared.events.SolicitacaoAgendadaEvent;
import br.com.ewaste.shared.events.SolicitacaoCriadaEvent;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Guarda os eventos da frente do Davi que chegaram DEPOIS do commit, como os ouvintes reais
 * recebem. Uso nos testes: {@code @Import(EventosCapturados.class)}.
 *
 * <p>Não usamos {@code @RecordApplicationEvents} porque ele só enxerga eventos publicados na thread
 * do teste, e o teste de concorrência publica em outras threads. Por isso a fila é thread-safe.
 */
@TestComponent
public class EventosCapturados {

  private final Queue<Object> eventos = new ConcurrentLinkedQueue<>();

  @TransactionalEventListener
  void criada(SolicitacaoCriadaEvent e) {
    eventos.add(e);
  }

  @TransactionalEventListener
  void alertas(AlertasEnviadosEvent e) {
    eventos.add(e);
  }

  @TransactionalEventListener
  void semColetora(SemColetoraNaRegiaoEvent e) {
    eventos.add(e);
  }

  @TransactionalEventListener
  void aceita(SolicitacaoAceitaEvent e) {
    eventos.add(e);
  }

  @TransactionalEventListener
  void agendada(SolicitacaoAgendadaEvent e) {
    eventos.add(e);
  }

  public <T> List<T> doTipo(Class<T> tipo) {
    return eventos.stream().filter(tipo::isInstance).map(tipo::cast).toList();
  }

  public void limpar() {
    eventos.clear();
  }
}
