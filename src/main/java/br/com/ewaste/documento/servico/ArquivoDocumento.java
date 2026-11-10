package br.com.ewaste.documento.servico;

import org.springframework.core.io.Resource;

public record ArquivoDocumento(String nome, Resource arquivo) {}
