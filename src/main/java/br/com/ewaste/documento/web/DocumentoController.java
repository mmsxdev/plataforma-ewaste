package br.com.ewaste.documento.web;

import br.com.ewaste.documento.servico.ArquivoDocumento;
import br.com.ewaste.documento.servico.DocumentoService;
import br.com.ewaste.usuario.api.UsuarioAtual;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
class DocumentoController {
  private final DocumentoService documentos;
  private final UsuarioAtual usuarioAtual;

  DocumentoController(DocumentoService documentos, UsuarioAtual usuarioAtual) {
    this.documentos = documentos;
    this.usuarioAtual = usuarioAtual;
  }

  @GetMapping("/geradora/documentos")
  String listar(Model model) {
    model.addAttribute("documentos", documentos.listarDaGeradora(usuarioAtual.atual().empresaId()));
    return "documento/lista";
  }

  @GetMapping("/geradora/documentos/{id}/baixar")
  ResponseEntity<Resource> baixar(@PathVariable Long id) {
    ArquivoDocumento documento = documentos.baixar(id, usuarioAtual.atual().empresaId());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(documento.nome()).build().toString())
        .contentType(MediaType.APPLICATION_PDF)
        .body(documento.arquivo());
  }

  @GetMapping("/verificar/{codigo}")
  String verificar(@PathVariable String codigo, Model model) {
    model.addAttribute("resultado", documentos.verificar(codigo).orElse(null));
    return "documento/verificar";
  }
}
