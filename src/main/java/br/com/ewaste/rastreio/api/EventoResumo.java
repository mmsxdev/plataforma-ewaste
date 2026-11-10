package br.com.ewaste.rastreio.api;

import java.math.BigDecimal;
import java.time.Instant;

public record EventoResumo(
    Long id,
    String tipo,
    String descricao,
    BigDecimal pesoRealKg,
    String evidenciaRef,
    String autor,
    Instant registradoEm) {}
