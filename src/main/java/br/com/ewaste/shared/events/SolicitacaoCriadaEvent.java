package br.com.ewaste.shared.events;

/**
 * Uma geradora criou uma solicitação de coleta (RF02).
 *
 * <p>Publicado por: Dev 3. Ouvido por: Dev 3 (roteamento), Dev 4 (linha do tempo).
 */
public record SolicitacaoCriadaEvent(Long solicitacaoId, Long geradoraId) {}
