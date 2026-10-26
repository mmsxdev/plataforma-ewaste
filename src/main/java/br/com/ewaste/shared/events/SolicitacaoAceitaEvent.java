package br.com.ewaste.shared.events;

/**
 * A primeira coletora aceitou a solicitação (RF04).
 *
 * <p>Publicado por: Dev 3 (aceite). Ouvido por: Dev 4 (linha do tempo), Dev 5 (notifica a geradora
 * e lança TAXA_MATCH).
 */
public record SolicitacaoAceitaEvent(Long solicitacaoId, Long geradoraId, Long coletoraId) {}
