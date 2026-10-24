package br.com.ewaste.shared.events;

import java.util.List;

/**
 * O roteamento alertou as coletoras aptas dentro do raio (RF03).
 *
 * <p>Publicado por: Dev 3 (roteamento). Ouvido por: Dev 4 (linha do tempo), Dev 5 (notifica cada
 * coletora).
 */
public record AlertasEnviadosEvent(Long solicitacaoId, List<Long> coletoraIds) {}
