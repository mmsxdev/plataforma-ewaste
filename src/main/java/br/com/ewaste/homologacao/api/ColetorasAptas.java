package br.com.ewaste.homologacao.api;

import java.util.List;

/**
 * Filtro de homologação (RNF01 / RN02): só coletoras aptas recebem alertas. Implementação: Dev 2 —
 * Felipe. Usado por: Dev 3 — Davi (roteamento).
 */
public interface ColetorasAptas {

  /** Apta = homologação APROVADA e sem documento obrigatório vencido. */
  boolean isApta(Long coletoraId);

  /** Ids das coletoras aptas neste momento. */
  List<Long> listarAptas();
}
