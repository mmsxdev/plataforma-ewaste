package br.com.ewaste.solicitacao.api;

/**
 * Ciclo de vida da solicitação.
 *
 * <pre>
 * ABERTA -> ACEITA -> AGENDADA -> COLETADA -> CONCLUIDA
 * ABERTA -> CANCELADA | EXPIRADA
 * </pre>
 *
 * Só o módulo solicitacao (Dev 3 — Davi) altera o status; os outros módulos pedem a transição por
 * {@link TransicaoSolicitacao}.
 */
public enum StatusSolicitacao {
  ABERTA,
  ACEITA,
  AGENDADA,
  COLETADA,
  CONCLUIDA,
  CANCELADA,
  EXPIRADA
}
