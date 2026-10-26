package br.com.ewaste.shared.events;

import br.com.ewaste.documento.api.TipoDocumento;

/**
 * Um documento (CDF, laudo ou certificado) foi emitido (RF06/RF07).
 *
 * <p>Publicado por: Dev 4 (documento). Ouvido por: Dev 5 (notifica a geradora e lança
 * TAXA_DOCUMENTO).
 */
public record DocumentoEmitidoEvent(
    Long documentoId, Long solicitacaoId, Long geradoraId, TipoDocumento tipo) {}
