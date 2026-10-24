package br.com.ewaste.solicitacao.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Dados de uma solicitação para outros módulos.
 *
 * @param coletoraId {@code null} enquanto ninguém aceitou
 * @param agendadaPara {@code null} enquanto não houver agendamento
 */
public record SolicitacaoResumo(
    Long id,
    Long geradoraId,
    Long coletoraId,
    StatusSolicitacao status,
    BigDecimal pesoEstimadoKg,
    boolean contemMidias,
    int qtdMidias,
    String cidade,
    String uf,
    double latitude,
    double longitude,
    OffsetDateTime agendadaPara) {}
