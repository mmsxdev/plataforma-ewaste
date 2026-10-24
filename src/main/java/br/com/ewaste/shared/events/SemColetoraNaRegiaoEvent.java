package br.com.ewaste.shared.events;

/**
 * Nenhuma coletora apta atende o local da solicitação.
 *
 * <p>Publicado por: Dev 3 (roteamento). Ouvido por: Dev 5 (avisa a geradora).
 */
public record SemColetoraNaRegiaoEvent(Long solicitacaoId, Long geradoraId) {}
