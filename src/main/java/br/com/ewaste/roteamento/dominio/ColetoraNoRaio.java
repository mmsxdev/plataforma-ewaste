package br.com.ewaste.roteamento.dominio;

/** Coletora que atende o local da solicitação, com a distância em linha reta até ele. */
public record ColetoraNoRaio(Long coletoraId, double distanciaKm) {}
