package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.shared.events.SolicitacaoAgendadaEvent;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agendamento da retirada pela coletora que venceu o aceite. */
@Service
@Transactional
public class AgendamentoService {

  private final SolicitacaoRepository solicitacoes;
  private final ApplicationEventPublisher eventos;

  AgendamentoService(SolicitacaoRepository solicitacoes, ApplicationEventPublisher eventos) {
    this.solicitacoes = solicitacoes;
    this.eventos = eventos;
  }

  /** Coletas que esta coletora venceu e ainda estão por retirar. */
  @Transactional(readOnly = true)
  public List<Solicitacao> listarAceitas(Long coletoraId) {
    return solicitacoes.findByColetoraIdAndStatusInOrderByAceitaEmDesc(
        coletoraId, EnumSet.of(StatusSolicitacao.ACEITA, StatusSolicitacao.AGENDADA));
  }

  /**
   * Só a vencedora agenda (outra coletora = 404). A transição e o reagendamento seguem a regra da
   * entidade; o {@code @Version} impede que dois agendamentos simultâneos se sobrescrevam.
   */
  public void agendar(Long solicitacaoId, Long coletoraId, Instant data) {
    if (data == null || !data.isAfter(Instant.now())) {
      throw new IllegalArgumentException("A data da retirada deve estar no futuro.");
    }
    Solicitacao s =
        solicitacoes
            .findByIdAndColetoraId(solicitacaoId, coletoraId)
            .orElseThrow(() -> new SolicitacaoNaoEncontradaException(solicitacaoId));
    s.agendar(data);
    eventos.publishEvent(
        new SolicitacaoAgendadaEvent(
            s.getId(), s.getGeradoraId(), data.atZone(Solicitacao.FUSO).toOffsetDateTime()));
  }
}
