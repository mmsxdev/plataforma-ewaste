package br.com.ewaste.homologacao.dominio;

/**
 * Tipos de documento que a coletora pode enviar. Os valores batem com o CHECK da coluna
 * documento_homologacao.tipo (V1__schema_inicial.sql).
 */
public enum TipoDocumentoHomologacao {
  LICENCA_AMBIENTAL(true),
  CADASTRO_IBAMA(true),
  MTR(false),
  ISO_27001(false),
  TISAX(false),
  LAUDO_IEC_21964(false),
  OUTRO(false);

  /** Obrigatório = sem ele (ou com ele vencido) a coletora não fica apta a receber alertas. */
  private final boolean obrigatorio;

  TipoDocumentoHomologacao(boolean obrigatorio) {
    this.obrigatorio = obrigatorio;
  }

  public boolean isObrigatorio() {
    return obrigatorio;
  }
}
