package br.com.ewaste.solicitacao.web;

import br.com.ewaste.solicitacao.dominio.Solicitacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulário de nova solicitação (RF02). Não tem campo de geradora: ela vem sempre do usuário
 * logado, nunca do que o navegador envia.
 */
public class NovaSolicitacaoForm {

  /** Linhas de item no formulário (P1: número fixo; as vazias são ignoradas). */
  static final int LINHAS_DE_ITEM = 3;

  @Size(max = 1000, message = "Use no máximo 1000 caracteres.")
  private String descricao;

  @NotNull(message = "Informe o peso estimado.")
  @Positive(message = "O peso estimado deve ser maior que zero.")
  @Digits(integer = 8, fraction = 2, message = "Use no máximo 2 casas decimais.")
  private BigDecimal pesoEstimadoKg;

  private boolean contemMidias;

  private Integer qtdMidias;

  @Size(max = 200, message = "Use no máximo 200 caracteres.")
  private String endereco;

  @NotBlank(message = "Informe a cidade.")
  @Size(max = 100, message = "Use no máximo 100 caracteres.")
  private String cidade;

  @NotBlank(message = "Informe a UF.")
  // Aceita espaços em volta (colar/autocompletar): paraSolicitacao() faz o trim
  @Pattern(regexp = "\\s*[A-Za-z]{2}\\s*", message = "Use a sigla com 2 letras (ex.: SP).")
  private String uf;

  @NotNull(message = "Informe a latitude do local.")
  @DecimalMin(value = "-90", message = "Latitude entre -90 e 90.")
  @DecimalMax(value = "90", message = "Latitude entre -90 e 90.")
  private Double latitude;

  @NotNull(message = "Informe a longitude do local.")
  @DecimalMin(value = "-180", message = "Longitude entre -180 e 180.")
  @DecimalMax(value = "180", message = "Longitude entre -180 e 180.")
  private Double longitude;

  @Valid private List<ItemForm> itens = new ArrayList<>();

  public NovaSolicitacaoForm() {
    for (int i = 0; i < LINHAS_DE_ITEM; i++) {
      itens.add(new ItemForm());
    }
  }

  @AssertTrue(message = "Informe a quantidade de mídias (maior que zero).")
  public boolean isQtdMidiasValida() {
    return !contemMidias || (qtdMidias != null && qtdMidias > 0);
  }

  @AssertTrue(message = "Informe pelo menos um item.")
  public boolean isItensPreenchidos() {
    return itens.stream().anyMatch(ItemForm::isPreenchido);
  }

  /** Converte para a entidade, ignorando as linhas de item vazias. */
  public Solicitacao paraSolicitacao(Long geradoraId) {
    Solicitacao s =
        Solicitacao.abrir(
            geradoraId,
            textoOuNulo(descricao),
            pesoEstimadoKg,
            contemMidias,
            qtdMidias == null ? 0 : qtdMidias,
            textoOuNulo(endereco),
            cidade.trim(),
            uf.trim().toUpperCase(),
            latitude,
            longitude);
    itens.stream()
        .filter(ItemForm::isPreenchido)
        .forEach(i -> s.adicionarItem(i.getTipoResiduo(), i.getQuantidade(), i.getPesoKg()));
    return s;
  }

  private static String textoOuNulo(String texto) {
    return texto == null || texto.isBlank() ? null : texto.trim();
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public BigDecimal getPesoEstimadoKg() {
    return pesoEstimadoKg;
  }

  public void setPesoEstimadoKg(BigDecimal pesoEstimadoKg) {
    this.pesoEstimadoKg = pesoEstimadoKg;
  }

  public boolean isContemMidias() {
    return contemMidias;
  }

  public void setContemMidias(boolean contemMidias) {
    this.contemMidias = contemMidias;
  }

  public Integer getQtdMidias() {
    return qtdMidias;
  }

  public void setQtdMidias(Integer qtdMidias) {
    this.qtdMidias = qtdMidias;
  }

  public String getEndereco() {
    return endereco;
  }

  public void setEndereco(String endereco) {
    this.endereco = endereco;
  }

  public String getCidade() {
    return cidade;
  }

  public void setCidade(String cidade) {
    this.cidade = cidade;
  }

  public String getUf() {
    return uf;
  }

  public void setUf(String uf) {
    this.uf = uf;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public List<ItemForm> getItens() {
    return itens;
  }

  public void setItens(List<ItemForm> itens) {
    this.itens = itens;
  }
}
