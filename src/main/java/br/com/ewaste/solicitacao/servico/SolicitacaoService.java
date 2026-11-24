package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.mapa.api.PontosColeta;
import br.com.ewaste.shared.events.SolicitacaoCriadaEvent;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso da geradora: criar e consultar as próprias solicitações (RF02). */
@Service
@Transactional
public class SolicitacaoService {

  private final SolicitacaoRepository solicitacoes;
  private final ApplicationEventPublisher eventos;
  private final PontosColeta pontos;

  SolicitacaoService(
      SolicitacaoRepository solicitacoes, ApplicationEventPublisher eventos, PontosColeta pontos) {
    this.pontos = pontos;
    this.solicitacoes = solicitacoes;
    this.eventos = eventos;
  }

  /**
   * Grava a solicitação (ABERTA) e anuncia a criação. O evento é publicado dentro da transação: os
   * ouvintes (roteamento, rastreio) só o recebem depois do commit, e não recebem nada se houver
   * rollback.
   */
  public Long criar(Solicitacao nova) {
    pontos.validar(
        nova.getPontoDescarteId(),
        nova.getItens().stream().map(i -> i.getTipoResiduo().name()).toList());
    if (nova.getItens().stream()
        .anyMatch(i -> i.getIdentificacao() == null || i.getIdentificacao().isBlank()))
      throw new IllegalArgumentException("Identifique todos os resíduos da nova solicitação.");
    Solicitacao salva = solicitacoes.save(nova);
    eventos.publishEvent(new SolicitacaoCriadaEvent(salva.getId(), salva.getGeradoraId()));
    return salva.getId();
  }

  /**
   * Edição textual: não muda local/categorias já usados no roteamento. Lock serializa com aceite.
   */
  public void editar(Long id, Long geradoraId, String descricao, Map<Long, String> identificacoes) {
    Solicitacao s =
        solicitacoes
            .findParaEdicao(id, geradoraId)
            .orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));
    if (s.getStatus() != StatusSolicitacao.ABERTA)
      throw new IllegalArgumentException("Somente solicitações abertas podem ser alteradas.");
    if (identificacoes.keySet().stream()
        .anyMatch(itemId -> s.getItens().stream().noneMatch(i -> i.getId().equals(itemId))))
      throw new IllegalArgumentException("Item não pertence à solicitação.");
    if (s.getItens().stream()
        .anyMatch(
            i ->
                identificacoes.containsKey(i.getId())
                    && (identificacoes.get(i.getId()) == null
                        || identificacoes.get(i.getId()).isBlank())
                    && (s.getPontoDescarteId() != null || i.getIdentificacao() != null)))
      throw new IllegalArgumentException("Uma identificação existente não pode ser apagada.");
    s.editarDescricao(descricao);
    s.getItens()
        .forEach(
            i -> {
              if (identificacoes.containsKey(i.getId()))
                i.identificar(identificacoes.get(i.getId()));
            });
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
