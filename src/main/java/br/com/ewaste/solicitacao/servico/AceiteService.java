package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.roteamento.api.AlertaConsulta;
import br.com.ewaste.shared.events.SolicitacaoAceitaEvent;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aceite ágil (RF04): a primeira coletora alertada que aceitar assume a coleta e a solicitação fica
 * bloqueada para as outras (RN04).
 */
@Service
public class AceiteService {

  private final SolicitacaoRepository solicitacoes;
  private final AlertaConsulta alertas;
  private final ColetorasAptas coletorasAptas;
  private final ApplicationEventPublisher eventos;

  AceiteService(
      SolicitacaoRepository solicitacoes,
      AlertaConsulta alertas,
      ColetorasAptas coletorasAptas,
      ApplicationEventPublisher eventos) {
    this.solicitacoes = solicitacoes;
    this.alertas = alertas;
    this.coletorasAptas = coletorasAptas;
    this.eventos = eventos;
  }

  /**
   * O coletoraId vem como parâmetro (o controller lê do usuário logado), o que permite testar a
   * concorrência com duas threads sem simular login.
   *
   * @throws SolicitacaoNaoEncontradaException se a coletora não foi alertada (404)
   */
  @Transactional
  public ResultadoAceite aceitar(Long solicitacaoId, Long coletoraId) {
    if (!alertas.foiAlertada(solicitacaoId, coletoraId)) {
      throw new SolicitacaoNaoEncontradaException(solicitacaoId);
    }
    // RN02 de novo: a homologação pode ter sido suspensa depois do alerta
    if (!coletorasAptas.isApta(coletoraId)) {
      return ResultadoAceite.HOMOLOGACAO_INATIVA;
    }
    // A garantia está no WHERE status = 'ABERTA', executado pelo banco
    if (solicitacoes.aceitarSeAberta(solicitacaoId, coletoraId) == 0) {
      return ResultadoAceite.JA_ACEITA;
    }
    // Venceu: avisa rastreio (Dev 4) e notificação/cobrança (Dev 5) depois do commit
    Long geradoraId =
        solicitacoes.findById(solicitacaoId).map(Solicitacao::getGeradoraId).orElseThrow();
    eventos.publishEvent(new SolicitacaoAceitaEvent(solicitacaoId, geradoraId, coletoraId));
    return ResultadoAceite.ACEITO;
  }
}
