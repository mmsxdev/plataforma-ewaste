package br.com.ewaste.solicitacao.web;

import br.com.ewaste.mapa.api.PontosColeta;
import br.com.ewaste.rastreio.api.RastreioConsulta;
import br.com.ewaste.roteamento.api.AlertaConsulta;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
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
import org.springframework.web.bind.annotation.RequestParam;
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
  private final AlertaConsulta alertas;
  private final RastreioConsulta rastreio;
  private final PontosColeta pontos;

  SolicitacaoController(
      SolicitacaoService servico,
      UsuarioAtual usuarioAtual,
      EmpresaConsulta empresas,
      AlertaConsulta alertas,
      RastreioConsulta rastreio,
      PontosColeta pontos) {
    this.pontos = pontos;
    this.servico = servico;
    this.usuarioAtual = usuarioAtual;
    this.empresas = empresas;
    this.alertas = alertas;
    this.rastreio = rastreio;
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
  String nova(
      @RequestParam(required = false) Long pontoDescarteId,
      @RequestParam(required = false) TipoResiduo tipoResiduo,
      Model model) {
    var form = new NovaSolicitacaoForm();
    form.setPontoDescarteId(pontoDescarteId);
    if (pontoDescarteId != null)
      pontos
          .buscar(pontoDescarteId)
          .orElseThrow(
              () ->
                  new org.springframework.web.server.ResponseStatusException(
                      org.springframework.http.HttpStatus.NOT_FOUND));
    if (tipoResiduo != null) form.getItens().getFirst().setTipoResiduo(tipoResiduo);
    model.addAttribute("form", form);
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
    Long id;
    try {
      id = servico.criar(form.paraSolicitacao(geradoraLogada()));
    } catch (IllegalArgumentException e) {
      erros.reject("regra", e.getMessage());
      return "solicitacao/nova";
    }
    redirect.addFlashAttribute("mensagem", "Solicitação criada.");
    return "redirect:/geradora/solicitacoes/" + id;
  }

  @GetMapping("/{id}/editar")
  String editar(@PathVariable Long id, Model model) {
    Solicitacao s = servico.buscarDaGeradora(id, geradoraLogada());
    if (s.getStatus() != StatusSolicitacao.ABERTA)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT, "A solicitação já foi aceita.");
    var form = new EdicaoSolicitacaoForm();
    form.setDescricao(s.getDescricao());
    s.getItens().forEach(i -> form.getIdentificacoes().put(i.getId(), i.getIdentificacao()));
    model.addAttribute("s", s);
    model.addAttribute("form", form);
    return "solicitacao/editar";
  }

  @PostMapping("/{id}/editar")
  String atualizar(
      @PathVariable Long id,
      @Valid @ModelAttribute("form") EdicaoSolicitacaoForm form,
      BindingResult erros,
      Model model,
      RedirectAttributes redirect) {
    Solicitacao s = servico.buscarDaGeradora(id, geradoraLogada());
    if (!erros.hasErrors()) {
      try {
        servico.editar(id, geradoraLogada(), form.getDescricao(), form.getIdentificacoes());
        redirect.addFlashAttribute("mensagem", "Identificação atualizada.");
        return "redirect:/geradora/solicitacoes/" + id;
      } catch (IllegalArgumentException e) {
        erros.reject("regra", e.getMessage());
      }
    }
    model.addAttribute("s", s);
    return "solicitacao/editar";
  }

  @GetMapping("/{id}")
  String detalhe(@PathVariable Long id, Model model) {
    Solicitacao s = servico.buscarDaGeradora(id, geradoraLogada());
    model.addAttribute("s", s);
    model.addAttribute("ponto", pontos.buscarParaHistorico(s.getPontoDescarteId()).orElse(null));
    // O roteamento roda logo após o commit da criação, na mesma thread: aqui os alertas já existem
    model.addAttribute("alertas", alertas.contarAlertas(id));
    model.addAttribute("historico", rastreio.listar(id));
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
