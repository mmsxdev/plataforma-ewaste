package br.com.ewaste.solicitacao.web;

import br.com.ewaste.solicitacao.dominio.TipoResiduo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Uma linha de item no formulário. Linha toda vazia é ignorada. */
public class ItemForm {

  private TipoResiduo tipoResiduo;

  @Positive(message = "A quantidade deve ser maior que zero.")
  private Integer quantidade;

  @Positive(message = "O peso deve ser maior que zero.")
  @Digits(integer = 8, fraction = 2, message = "Use no máximo 2 casas decimais.")
  private BigDecimal pesoKg;

  /** Alguma coluna da linha foi preenchida. */
  public boolean isPreenchido() {
    return tipoResiduo != null || quantidade != null || pesoKg != null;
  }

  /** Linha começada precisa ter as três colunas. */
  @AssertTrue(message = "Preencha tipo, quantidade e peso do item.")
  public boolean isCompleto() {
    return !isPreenchido() || (tipoResiduo != null && quantidade != null && pesoKg != null);
  }

  public TipoResiduo getTipoResiduo() {
    return tipoResiduo;
  }

  public void setTipoResiduo(TipoResiduo tipoResiduo) {
    this.tipoResiduo = tipoResiduo;
  }

  public Integer getQuantidade() {
    return quantidade;
  }

  public void setQuantidade(Integer quantidade) {
    this.quantidade = quantidade;
  }

  public BigDecimal getPesoKg() {
    return pesoKg;
  }

  public void setPesoKg(BigDecimal pesoKg) {
    this.pesoKg = pesoKg;
  }
}
