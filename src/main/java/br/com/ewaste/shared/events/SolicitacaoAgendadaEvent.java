package br.com.ewaste.shared.events;

import java.time.OffsetDateTime;

/**
 * A coletora vencedora agendou a retirada.
 *
 * <p>Publicado por: Dev 3 (agendamento). Ouvido por: Dev 4 (linha do tempo), Dev 5 (notifica a
 * geradora).
 */
public record SolicitacaoAgendadaEvent(
    Long solicitacaoId, Long geradoraId, OffsetDateTime agendadaPara) {}
