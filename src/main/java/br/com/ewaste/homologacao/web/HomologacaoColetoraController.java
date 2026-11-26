package br.com.ewaste.homologacao.web;

import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao;
import br.com.ewaste.homologacao.servico.HomologacaoService;
import br.com.ewaste.usuario.api.UsuarioAtual;
import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Coletora envia seus documentos e pede a análise da homologação. */
@Controller
@RequestMapping("/coletora/homologacao")
class HomologacaoColetoraController {

  private final HomologacaoService homologacoes;
  private final UsuarioAtual usuarioAtual;

  HomologacaoColetoraController(HomologacaoService homologacoes, UsuarioAtual usuarioAtual) {
    this.homologacoes = homologacoes;
    this.usuarioAtual = usuarioAtual;
  }

  @GetMapping
  String pagina(Model model) {
    Homologacao h = homologacoes.daColetora(coletora());
    model.addAttribute("h", HomologacaoView.de(h, null));
    model.addAttribute(
        "tipos",
        Arrays.stream(TipoDocumentoHomologacao.values())
            .map(
                t ->
                    new HomologacaoView.TipoOpcao(
                        t, HomologacaoService.rotulo(t), t.isObrigatorio()))
            .toList());
    return "homologacao/coletora";
  }

  @PostMapping("/documentos")
  String enviarDocumento(
      @RequestParam(required = false) TipoDocumentoHomologacao tipo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate validade,
      @RequestParam(required = false) MultipartFile arquivo,
      RedirectAttributes redirect) {
    return executar(
        redirect,
        "Documento enviado.",
        () -> homologacoes.enviarDocumento(coletora(), tipo, validade, arquivo));
  }

  @PostMapping("/enviar")
  String enviarParaAnalise(RedirectAttributes redirect) {
    return executar(
        redirect,
        "Documentos enviados para análise. Você será avisado quando o administrador decidir.",
        () -> homologacoes.enviarParaAnalise(coletora()));
  }

  @GetMapping("/documentos/{documentoId}")
  ResponseEntity<Resource> documento(@PathVariable Long documentoId) {
    return Arquivos.download(homologacoes.documentoDaColetora(coletora(), documentoId));
  }

  private String executar(RedirectAttributes redirect, String sucesso, Runnable acao) {
    try {
      acao.run();
      redirect.addFlashAttribute("mensagem", sucesso);
    } catch (IllegalArgumentException e) {
      redirect.addFlashAttribute("erro", e.getMessage());
    }
    return "redirect:/coletora/homologacao";
  }

  private Long coletora() {
    return usuarioAtual.atual().empresaId();
  }
}
