package br.com.ewaste.solicitacao.web;

import br.com.ewaste.solicitacao.servico.OportunidadeService;
import br.com.ewaste.usuario.api.UsuarioAtual;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Telas da coletora (RF03/RF04). Ficam no módulo solicitacao, junto do aceite, porque só este
 * módulo muda o status; o roteamento só informa quem foi alertado (AlertaConsulta).
 */
@Controller
@RequestMapping("/coletora/oportunidades")
class OportunidadeController {

  private final OportunidadeService oportunidades;
  private final UsuarioAtual usuarioAtual;

  OportunidadeController(OportunidadeService oportunidades, UsuarioAtual usuarioAtual) {
    this.oportunidades = oportunidades;
    this.usuarioAtual = usuarioAtual;
  }

  @GetMapping
  String listar(Model model) {
    model.addAttribute("oportunidades", oportunidades.listarAbertas(coletoraLogada()));
    return "solicitacao/oportunidades";
  }

  @GetMapping("/{id}")
  String detalhe(@PathVariable Long id, Model model) {
    model.addAttribute("o", oportunidades.buscar(id, coletoraLogada()));
    return "solicitacao/oportunidade";
  }

  private Long coletoraLogada() {
    return usuarioAtual.atual().empresaId();
  }
}
