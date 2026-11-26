package br.com.ewaste.solicitacao.api;

import java.util.List;
import java.util.Optional;

/**
 * Consulta de solicitações. Implementação: Dev 3 — Davi. Usado por: Dev 4, Dev 5 e roteamento (ao
 * homologar uma coletora).
 */
public interface SolicitacaoConsulta {

  Optional<SolicitacaoResumo> buscar(Long id);

  List<SolicitacaoResumo> listarDaColetora(Long coletoraId);

  /** Solicitações ainda ABERTAS (sem coletora), para alertar coletoras recém-homologadas. */
  List<SolicitacaoResumo> listarAbertas();
}
