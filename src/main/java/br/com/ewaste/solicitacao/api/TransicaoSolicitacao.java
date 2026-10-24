package br.com.ewaste.solicitacao.api;

/**
 * Transições pedidas por outros módulos. Implementação: Dev 3 — Davi. Usado por: Dev 4 — Guilherme.
 *
 * <p>Lança {@link IllegalStateException} se a transição não for válida a partir do status atual.
 */
public interface TransicaoSolicitacao {

  /** AGENDADA -> COLETADA. */
  void marcarColetada(Long solicitacaoId);

  /** COLETADA -> CONCLUIDA. */
  void marcarConcluida(Long solicitacaoId);
}
