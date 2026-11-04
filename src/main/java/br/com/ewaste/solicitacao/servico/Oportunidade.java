package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.solicitacao.dominio.Solicitacao;
import java.math.BigDecimal;

/** Solicitação vista pela coletora alertada, com a distância até o local da coleta. */
public record Oportunidade(Solicitacao solicitacao, BigDecimal distanciaKm) {}
