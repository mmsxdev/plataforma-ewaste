package br.com.ewaste.shared.events;

/**
 * O admin reprovou a homologação de uma coletora.
 *
 * <p>Publicado por: Dev 2. Ouvido por: Dev 5 (notifica a coletora).
 */
public record ColetoraReprovadaEvent(Long coletoraId, String motivo) {}
