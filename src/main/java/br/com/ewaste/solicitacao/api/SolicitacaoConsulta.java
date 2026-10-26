package br.com.ewaste.solicitacao.api;

import java.util.Optional;

/** Consulta de solicitações. Implementação: Dev 3 — Davi. Usado por: Dev 4 e Dev 5. */
public interface SolicitacaoConsulta {

  Optional<SolicitacaoResumo> buscar(Long id);
}
