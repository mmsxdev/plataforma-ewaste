package br.com.ewaste.rastreio.web;

import br.com.ewaste.rastreio.servico.RastreioService;
import br.com.ewaste.usuario.api.UsuarioAtual;
import java.math.BigDecimal;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/coletora/coletas")
class ColetaController {
  private final RastreioService rastreio;
  private final UsuarioAtual usuarioAtual;

  ColetaController(RastreioService rastreio, UsuarioAtual usuarioAtual) {
    this.rastreio = rastreio;
    this.usuarioAtual = usuarioAtual;
  }

  @GetMapping
  String listar(Model model) {
    model.addAttribute("coletas", rastreio.listarDaColetora(usuarioAtual.atual().empresaId()));
    return "rastreio/lista";
  }

  @GetMapping("/{id}")
  String detalhe(@PathVariable Long id, Model model) {
    Long coletoraId = usuarioAtual.atual().empresaId();
    model.addAttribute("s", rastreio.buscarDaColetora(id, coletoraId));
    model.addAttribute("historico", rastreio.listar(id));
    model.addAttribute("midias", rastreio.listarMidias(id));
    model.addAttribute(
        "destinada", rastreio.listar(id).stream().anyMatch(e -> e.tipo().equals("DESTINADA")));
    return "rastreio/detalhe";
  }

  @PostMapping("/{id}/coletar")
  String coletar(
      @PathVariable Long id,
      @RequestParam BigDecimal pesoRealKg,
      @RequestParam(required = false) MultipartFile foto,
      RedirectAttributes redirect) {
    return executar(
        id,
        redirect,
        "Coleta registrada.",
        () -> rastreio.registrarColeta(id, empresa(), usuario(), pesoRealKg, foto));
  }

  @PostMapping("/{id}/midias")
  String midia(
      @PathVariable Long id,
      @RequestParam String numeroSerie,
      @RequestParam String tipo,
      @RequestParam String metodo,
      @RequestParam String nivel,
      @RequestParam(required = false) MultipartFile foto,
      RedirectAttributes redirect) {
    return executar(
        id,
        redirect,
        "Mídia destruída registrada.",
        () ->
            rastreio.registrarMidia(
                id, empresa(), usuario(), numeroSerie, tipo, metodo, nivel, foto));
  }

  @PostMapping("/{id}/destinar")
  String destinar(
      @PathVariable Long id, @RequestParam String destinacao, RedirectAttributes redirect) {
    return executar(
        id,
        redirect,
        "Destinação registrada.",
        () -> rastreio.registrarDestinacao(id, empresa(), usuario(), destinacao));
  }

  @PostMapping("/{id}/concluir")
  String concluir(@PathVariable Long id, RedirectAttributes redirect) {
    return executar(
        id,
        redirect,
        "Coleta concluída. Documentos emitidos.",
        () -> rastreio.concluir(id, empresa(), usuario()));
  }

  @GetMapping("/{id}/evidencias/{eventoId}")
  ResponseEntity<Resource> evidenciaColeta(@PathVariable Long id, @PathVariable Long eventoId) {
    return arquivo(rastreio.evidenciaDaColeta(id, eventoId, empresa()));
  }

  @GetMapping("/{id}/midias/{midiaId}/evidencia")
  ResponseEntity<Resource> evidenciaMidia(@PathVariable Long id, @PathVariable Long midiaId) {
    return arquivo(rastreio.evidenciaDaMidia(id, midiaId, empresa()));
  }

  private String executar(Long id, RedirectAttributes redirect, String sucesso, Runnable acao) {
    try {
      acao.run();
      redirect.addFlashAttribute("mensagem", sucesso);
    } catch (IllegalArgumentException e) {
      redirect.addFlashAttribute("erro", e.getMessage());
    } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
      redirect.addFlashAttribute("erro", "A coleta mudou. Atualize a página e tente novamente.");
    }
    return "redirect:/coletora/coletas/" + id;
  }

  private Long empresa() {
    return usuarioAtual.atual().empresaId();
  }

  private Long usuario() {
    return usuarioAtual.atual().usuarioId();
  }

  private static ResponseEntity<Resource> arquivo(Resource arquivo) {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(arquivo.getFilename()).build().toString())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(arquivo);
  }
}
