package br.com.ewaste.solicitacao.dominio;

/**
 * Tipo de resíduo de um item da solicitação. Os valores são os mesmos do CHECK de
 * solicitacao_item.tipo_residuo no V1 (gravado como texto).
 */
public enum TipoResiduo {
  INFORMATICA("Informática"),
  TELEFONIA("Telefonia"),
  ELETRODOMESTICO("Eletrodoméstico"),
  MIDIA_ARMAZENAMENTO("Mídia de armazenamento (HD, SSD, fita)"),
  BATERIA("Bateria"),
  OUTRO("Outro");

  private final String descricao;

  TipoResiduo(String descricao) {
    this.descricao = descricao;
  }

  /** Nome para mostrar nas telas. */
  public String getDescricao() {
    return descricao;
  }
}
