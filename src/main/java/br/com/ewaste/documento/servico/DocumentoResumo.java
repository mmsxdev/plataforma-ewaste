package br.com.ewaste.documento.servico;

import br.com.ewaste.documento.api.TipoDocumento;
import java.time.Instant;

public record DocumentoResumo(
    Long id,
    Long solicitacaoId,
    TipoDocumento tipo,
    String numero,
    String codigoVerificacao,
    Instant emitidoEm) {}
