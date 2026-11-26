package br.com.ewaste.homologacao.web;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Download de documentos como anexo (nunca renderizados inline no domínio da aplicação). */
final class Arquivos {

  private Arquivos() {}

  static ResponseEntity<Resource> download(Resource arquivo) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(arquivo.getFilename()).build().toString())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(arquivo);
  }
}
