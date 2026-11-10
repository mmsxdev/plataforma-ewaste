package br.com.ewaste.documento.api;

import java.math.BigDecimal;
import java.util.List;

public record DadosEmissao(
    Long solicitacaoId,
    Long geradoraId,
    BigDecimal pesoRealKg,
    String destinacao,
    List<Midia> midias) {
  public record Midia(String numeroSerie, String tipo, String metodo, String nivel) {}
}
