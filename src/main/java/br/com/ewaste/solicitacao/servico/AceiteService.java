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
    // RN02 de novo: a homologação pode ter sido suspensa depois do alerta. Se ela já é a dona
    // (venceu antes da suspensão), a mensagem certa é "você já aceitou"
    if (!coletorasAptas.isApta(coletoraId)) {
      return ehDona(buscar(solicitacaoId), coletoraId)
          ? ResultadoAceite.JA_ERA_SUA
          : ResultadoAceite.HOMOLOGACAO_INATIVA;
    }
    // A garantia está no WHERE status = 'ABERTA', executado pelo banco
    boolean venceu = solicitacoes.aceitarSeAberta(solicitacaoId, coletoraId) == 1;
    Solicitacao s = buscar(solicitacaoId);
    if (!venceu) {
      return semAceite(s, coletoraId);
    }
    // Venceu: avisa rastreio (Dev 4) e notificação/cobrança (Dev 5) depois do commit
    eventos.publishEvent(new SolicitacaoAceitaEvent(solicitacaoId, s.getGeradoraId(), coletoraId));
    return ResultadoAceite.ACEITO;
  }

  /**
   * 0 linhas alteradas: a solicitação não estava mais ABERTA. O dono decide a mensagem; o evento
   * não se repete (já foi publicado por quem venceu).
   */
  private static ResultadoAceite semAceite(Solicitacao s, Long coletoraId) {
    if (s.getColetoraId() == null) {
      return ResultadoAceite.INDISPONIVEL; // cancelada ou expirada: ninguém aceitou
    }
    return ehDona(s, coletoraId) ? ResultadoAceite.JA_ERA_SUA : ResultadoAceite.JA_ACEITA;
  }

  private static boolean ehDona(Solicitacao s, Long coletoraId) {
    return coletoraId.equals(s.getColetoraId());
  }

  private Solicitacao buscar(Long solicitacaoId) {
    return solicitacoes
        .findById(solicitacaoId)
        .orElseThrow(() -> new SolicitacaoNaoEncontradaException(solicitacaoId));
  }
}
