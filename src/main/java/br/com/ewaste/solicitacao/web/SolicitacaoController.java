package br.com.ewaste.solicitacao.web;

import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.TipoResiduo;
import br.com.ewaste.solicitacao.servico.SolicitacaoService;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.EmpresaResumo;
import br.com.ewaste.usuario.api.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas da geradora (RF02). O acesso por perfil vem do SecurityConfig (/geradora/** só para
 * GERADORA); a checagem de dono do recurso fica no SolicitacaoService.
 */
@Controller
@RequestMapping("/geradora/solicitacoes")
class SolicitacaoController {

  private final SolicitacaoService servico;
  private final UsuarioAtual usuarioAtual;
  private final EmpresaConsulta empresas;

  SolicitacaoController(
      SolicitacaoService servico, UsuarioAtual usuarioAtual, EmpresaConsulta empresas) {
    this.servico = servico;
    this.usuarioAtual = usuarioAtual;
    this.empresas = empresas;
  }

  @ModelAttribute("tiposResiduo")
  TipoResiduo[] tiposResiduo() {
    return TipoResiduo.values();
  }

  @GetMapping
  String listar(Model model) {
    model.addAttribute("solicitacoes", servico.listarDaGeradora(geradoraLogada()));
    return "solicitacao/lista";
  }

  @GetMapping("/nova")
  String nova(Model model) {
    model.addAttribute("form", new NovaSolicitacaoForm());
    return "solicitacao/nova";
  }

  /** Inválido: volta para a mesma tela com os erros e o que foi digitado. Válido: PRG. */
  @PostMapping
  String criar(
      @Valid @ModelAttribute("form") NovaSolicitacaoForm form,
      BindingResult erros,
      RedirectAttributes redirect) {
    if (erros.hasErrors()) {
      return "solicitacao/nova";
    }
    Long id = servico.criar(form.paraSolicitacao(geradoraLogada()));
    redirect.addFlashAttribute("mensagem", "Solicitação criada.");
    return "redirect:/geradora/solicitacoes/" + id;
  }

  @GetMapping("/{id}")
  String detalhe(@PathVariable Long id, Model model) {
    Solicitacao s = servico.buscarDaGeradora(id, geradoraLogada());
    model.addAttribute("s", s);
    if (s.getColetoraId() != null) {
      model.addAttribute(
          "coletoraNome",
          empresas.buscar(s.getColetoraId()).map(EmpresaResumo::razaoSocial).orElse("—"));
    }
    return "solicitacao/detalhe";
  }

  /** Sempre da sessão autenticada, nunca de um campo do formulário. */
  private Long geradoraLogada() {
    return usuarioAtual.atual().empresaId();
  }
}
