package br.com.ewaste.armazenamento.api;

/**
 * Referência a um arquivo guardado. Grave {@link #caminho()} nas colunas *_ref do banco.
 *
 * @param caminho identificador interno do arquivo (ex.: "homologacao/3f2a...pdf")
 */
public record ArquivoRef(String caminho, String nomeOriginal, String contentType, long tamanho) {}
