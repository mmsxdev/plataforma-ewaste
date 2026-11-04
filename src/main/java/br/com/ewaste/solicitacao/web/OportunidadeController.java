package br.com.ewaste.solicitacao.web;

import br.com.ewaste.solicitacao.servico.AceiteService;
import br.com.ewaste.solicitacao.servico.OportunidadeService;
import br.com.ewaste.usuario.api.UsuarioAtual;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas da coletora (RF03/RF04). Ficam no módulo solicitacao, junto do aceite, porque só este
 * módulo muda o status; o roteamento só informa quem foi alertado (AlertaConsulta).
 */
@Controller
@RequestMapping("/coletora/oportunidades")
class OportunidadeController {

  private final OportunidadeService oportunidades;
  private final AceiteService aceite;
  private final UsuarioAtual usuarioAtual;

  OportunidadeController(
      OportunidadeService oportunidades, AceiteService aceite, UsuarioAtual usuarioAtual) {
    this.oportunidades = oportunidades;
    this.aceite = aceite;
    this.usuarioAtual = usuarioAtual;
  }

  @GetMapping
  String listar(Model model) {
    model.addAttribute("oportunidades", oportunidades.listarAbertas(coletoraLogada()));
    return "solicitacao/oportunidades";
  }

  @GetMapping("/{id}")
  String detalhe(@PathVariable Long id, Model model) {
    Long coletora = coletoraLogada();
    model.addAttribute("o", oportunidades.buscar(id, coletora));
    model.addAttribute("coletoraLogada", coletora);
    return "solicitacao/oportunidade";
  }

  /** Aceite atômico: o desfecho vira mensagem (flash) e redirect (PRG). */
  @PostMapping("/{id}/aceitar")
  String aceitar(@PathVariable Long id, RedirectAttributes redirect) {
    return switch (aceite.aceitar(id, coletoraLogada())) {
      case ACEITO -> {
        redirect.addFlashAttribute("mensagem", "Coleta aceita! Agora agende a retirada.");
        yield "redirect:/coletora/oportunidades/" + id;
      }
      case JA_ACEITA -> {
        redirect.addFlashAttribute("erro", "Esta coleta já foi aceita por outra coletora.");
        yield "redirect:/coletora/oportunidades";
      }
      case HOMOLOGACAO_INATIVA -> {
        redirect.addFlashAttribute("erro", "Sua homologação não está ativa.");
        yield "redirect:/coletora/oportunidades/" + id;
      }
    };
  }

  private Long coletoraLogada() {
    return usuarioAtual.atual().empresaId();
  }
}
