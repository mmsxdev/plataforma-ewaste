package br.com.ewaste.rastreio.servico;

import java.time.Instant;

public record MidiaResumo(
    Long id,
    String numeroSerie,
    String tipo,
    String metodo,
    String nivel,
    String evidenciaRef,
    Instant destruidaEm) {}
