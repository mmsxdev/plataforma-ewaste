package br.com.ewaste.homologacao.web;

import br.com.ewaste.homologacao.api.StatusHomologacao;
import br.com.ewaste.homologacao.servico.HomologacaoService;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.UsuarioAtual;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin analisa os documentos e aprova ou reprova a homologação das coletoras. */
@Controller
@RequestMapping("/admin/homologacoes")
class AdminHomologacaoController {

  private final HomologacaoService homologacoes;
  private final EmpresaConsulta empresas;
  private final UsuarioAtual usuarioAtual;

  AdminHomologacaoController(
      HomologacaoService homologacoes, EmpresaConsulta empresas, UsuarioAtual usuarioAtual) {
    this.homologacoes = homologacoes;
    this.empresas = empresas;
    this.usuarioAtual = usuarioAtual;
  }

  @GetMapping
  String listar(Model model) {
    List<HomologacaoView> todas =
        homologacoes.todas().stream()
            .map(h -> HomologacaoView.de(h, empresas.buscar(h.getEmpresaId()).orElse(null)))
            .toList();
    model.addAttribute(
        "emAnalise",
        todas.stream().filter(h -> h.status() == StatusHomologacao.EM_ANALISE).toList());
    model.addAttribute(
        "demais", todas.stream().filter(h -> h.status() != StatusHomologacao.EM_ANALISE).toList());
    return "homologacao/admin";
  }

  @PostMapping("/{id}/aprovar")
  String aprovar(@PathVariable Long id, RedirectAttributes redirect) {
    return executar(
        redirect,
        "Coletora aprovada. Ela passa a receber as solicitações abertas na sua região.",
        () -> homologacoes.aprovar(id, usuarioAtual.atual().usuarioId()));
  }

  @PostMapping("/{id}/reprovar")
  String reprovar(
      @PathVariable Long id,
      @RequestParam(required = false) String motivo,
      RedirectAttributes redirect) {
    return executar(
        redirect,
        "Homologação reprovada. A coletora verá o motivo e poderá reenviar.",
        () -> homologacoes.reprovar(id, usuarioAtual.atual().usuarioId(), motivo));
  }

  @GetMapping("/{id}/documentos/{documentoId}")
  ResponseEntity<Resource> documento(@PathVariable Long id, @PathVariable Long documentoId) {
    try {
      return Arquivos.download(homologacoes.documentoParaAdmin(id, documentoId));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  private String executar(RedirectAttributes redirect, String sucesso, Runnable acao) {
    try {
      acao.run();
      redirect.addFlashAttribute("mensagem", sucesso);
    } catch (IllegalArgumentException e) {
      redirect.addFlashAttribute("erro", e.getMessage());
    }
    return "redirect:/admin/homologacoes";
  }
}
