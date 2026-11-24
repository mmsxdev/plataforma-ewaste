package br.com.ewaste.mapa.api;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record PontoResumo(
    Long id,
    String nome,
    String endereco,
    String cidade,
    String uf,
    String tiposAceitos,
    String horario) {
  public Set<String> categorias() {
    if (tiposAceitos == null || tiposAceitos.isBlank()) return Set.of();
    return Arrays.stream(tiposAceitos.split(","))
        .map(String::strip)
        .map(t -> t.toUpperCase(java.util.Locale.ROOT))
        .collect(Collectors.toUnmodifiableSet());
  }
}
