package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.shared.events.SolicitacaoCriadaEvent;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso da geradora: criar e consultar as próprias solicitações (RF02). */
@Service
@Transactional
public class SolicitacaoService {

  private final SolicitacaoRepository solicitacoes;
  private final ApplicationEventPublisher eventos;

  SolicitacaoService(SolicitacaoRepository solicitacoes, ApplicationEventPublisher eventos) {
    this.solicitacoes = solicitacoes;
    this.eventos = eventos;
  }

  /**
   * Grava a solicitação (ABERTA) e anuncia a criação. O evento é publicado dentro da transação: os
   * ouvintes (roteamento, rastreio) só o recebem depois do commit, e não recebem nada se houver
   * rollback.
   */
  public Long criar(Solicitacao nova) {
    Solicitacao salva = solicitacoes.save(nova);
    eventos.publishEvent(new SolicitacaoCriadaEvent(salva.getId(), salva.getGeradoraId()));
    return salva.getId();
  }

  @Transactional(readOnly = true)
  public List<Solicitacao> listarDaGeradora(Long geradoraId) {
    return solicitacoes.findByGeradoraIdOrderByCriadaEmDesc(geradoraId);
  }

  /**
   * Busca pelo id E pela geradora: a de outra empresa não é encontrada (404), sem revelar que o id
   * existe.
   */
  @Transactional(readOnly = true)
  public Solicitacao buscarDaGeradora(Long id, Long geradoraId) {
    return solicitacoes
        .findDaGeradoraComItens(id, geradoraId)
        .orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));
  }
}
