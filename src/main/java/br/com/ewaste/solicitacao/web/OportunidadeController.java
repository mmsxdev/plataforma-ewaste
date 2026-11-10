package br.com.ewaste.solicitacao.web;

import br.com.ewaste.solicitacao.dominio.TransicaoInvalidaException;
import br.com.ewaste.solicitacao.servico.AceiteService;
import br.com.ewaste.solicitacao.servico.AgendamentoService;
import br.com.ewaste.solicitacao.servico.Oportunidade;
import br.com.ewaste.solicitacao.servico.OportunidadeService;
import br.com.ewaste.usuario.api.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
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
 * Telas da coletora (RF03/RF04). Ficam no módulo solicitacao, junto do aceite, porque só este
 * módulo muda o status; o roteamento só informa quem foi alertado (AlertaConsulta).
 */
@Controller
@RequestMapping("/coletora/oportunidades")
class OportunidadeController {

  private final OportunidadeService oportunidades;
  private final AceiteService aceite;
  private final AgendamentoService agendamento;
  private final UsuarioAtual usuarioAtual;

  OportunidadeController(
      OportunidadeService oportunidades,
      AceiteService aceite,
      AgendamentoService agendamento,
      UsuarioAtual usuarioAtual) {
    this.oportunidades = oportunidades;
    this.aceite = aceite;
    this.agendamento = agendamento;
    this.usuarioAtual = usuarioAtual;
  }

  @GetMapping
  String listar(Model model) {
    model.addAttribute("oportunidades", oportunidades.listarAbertas(coletoraLogada()));
    return "solicitacao/oportunidades";
  }

  /** Coletas que esta coletora venceu (ACEITAS ou AGENDADAS). */
  @GetMapping("/aceitas")
  String aceitas(Model model) {
    model.addAttribute("aceitas", agendamento.listarAceitas(coletoraLogada()));
    return "solicitacao/aceitas";
  }

  @GetMapping("/{id}")
  String detalhe(@PathVariable Long id, Model model) {
    Oportunidade o = oportunidades.buscar(id, coletoraLogada());
    model.addAttribute("agendamento", new AgendamentoForm(o.solicitacao().getAgendadaPara()));
    return mostrarDetalhe(o, model);
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
      case INDISPONIVEL -> {
        redirect.addFlashAttribute("erro", "Esta coleta não está mais disponível.");
        yield "redirect:/coletora/oportunidades";
      }
      case JA_ERA_SUA -> {
        redirect.addFlashAttribute("mensagem", "Você já aceitou esta coleta.");
        yield "redirect:/coletora/oportunidades/" + id;
      }
      case HOMOLOGACAO_INATIVA -> {
        redirect.addFlashAttribute("erro", "Sua homologação não está ativa.");
        yield "redirect:/coletora/oportunidades/" + id;
      }
    };
  }

  /** ACEITA -> AGENDADA (ou reagendamento). Data inválida: volta ao detalhe com o erro no campo. */
  @PostMapping("/{id}/agendar")
  String agendar(
      @PathVariable Long id,
      @Valid @ModelAttribute("agendamento") AgendamentoForm form,
      BindingResult erros,
      Model model,
      RedirectAttributes redirect) {
    if (erros.hasErrors()) {
      return mostrarDetalhe(oportunidades.buscar(id, coletoraLogada()), model);
    }
    try {
      agendamento.agendar(id, coletoraLogada(), form.paraInstant());
    } catch (TransicaoInvalidaException
        | IllegalArgumentException
        | OptimisticLockingFailureException e) {
      // A coleta mudou enquanto a tela estava aberta (ex.: já foi coletada) ou outro envio
      // simultâneo gravou antes (@Version). Sem @ControllerAdvice no projeto, isso viraria um 500.
      redirect.addFlashAttribute(
          "erro",
          "Não foi possível agendar: esta coleta mudou de situação. Confira e tente de novo.");
      return "redirect:/coletora/oportunidades/" + id;
    }
    redirect.addFlashAttribute("mensagem", "Retirada agendada.");
    return "redirect:/coletora/oportunidades/" + id;
  }

  private String mostrarDetalhe(Oportunidade o, Model model) {
    model.addAttribute("o", o);
    model.addAttribute("coletoraLogada", coletoraLogada());
    return "solicitacao/oportunidade";
  }

  private Long coletoraLogada() {
    return usuarioAtual.atual().empresaId();
  }
}
