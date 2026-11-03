package br.com.ewaste.roteamento.api;

import java.util.List;
import java.util.Optional;

/**
 * Quem foi alertado sobre qual solicitação. Implementação e uso: Dev 3 — Davi (o módulo solicitacao
 * usa para as telas de oportunidades e para validar o aceite).
 */
public interface AlertaConsulta {

  boolean foiAlertada(Long solicitacaoId, Long coletoraId);

  Optional<AlertaResumo> buscar(Long solicitacaoId, Long coletoraId);

  /** Alertas recebidos pela coletora, do local mais perto para o mais longe. */
  List<AlertaResumo> listarDaColetora(Long coletoraId);

  /** Quantas coletoras foram alertadas sobre a solicitação. */
  long contarAlertas(Long solicitacaoId);
}
