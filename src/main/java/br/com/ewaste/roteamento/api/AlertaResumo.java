package br.com.ewaste.roteamento.api;

import java.math.BigDecimal;
import java.time.Instant;

/** Um alerta enviado a uma coletora, com a distância até o local da solicitação. */
public record AlertaResumo(
    Long solicitacaoId, Long coletoraId, BigDecimal distanciaKm, Instant enviadoEm) {}
