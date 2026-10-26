package br.com.ewaste.shared.events;

/**
 * A coletora registrou a retirada física (RNF02).
 *
 * <p>Publicado por: Dev 4 (rastreio). Ouvido por: Dev 5 (notifica a geradora).
 */
public record ColetaRegistradaEvent(Long solicitacaoId, Long geradoraId) {}
