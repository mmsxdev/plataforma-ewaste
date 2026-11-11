package br.com.ewaste.documento.servico;

import br.com.ewaste.documento.api.TipoDocumento;

public record Verificacao(String numero, TipoDocumento tipo, boolean valido) {}
