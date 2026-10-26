package br.com.ewaste.shared.events;

/**
 * O admin aprovou a homologação de uma coletora.
 *
 * <p>Publicado por: Dev 2. Ouvido por: Dev 5 (notifica a coletora).
 */
public record ColetoraHomologadaEvent(Long coletoraId) {}
