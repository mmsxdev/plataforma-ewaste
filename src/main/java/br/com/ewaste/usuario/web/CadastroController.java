package br.com.ewaste.usuario.web;

import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.servico.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CadastroController {
  private final CadastroService cadastro;

  public CadastroController(CadastroService cadastro) {
    this.cadastro = cadastro;
  }

  @GetMapping("/cadastro/{tipo}")
  public String formulario(@PathVariable String tipo, Model model) {
    preparar(tipo, model);
    model.addAttribute("cadastro", CadastroDados.vazio());
    return "usuario/cadastro";
  }

  @PostMapping("/cadastro/{tipo}")
  public String cadastrar(
      @PathVariable String tipo,
      @Valid @ModelAttribute("cadastro") CadastroDados dados,
      BindingResult erros,
      Model model) {
    TipoEmpresa empresa = preparar(tipo, model);
    if (empresa == TipoEmpresa.COLETORA && dados.raioAtendimentoKm() == null)
      erros.rejectValue("raioAtendimentoKm", "obrigatorio", "Informe o raio de atendimento.");
    if (erros.hasErrors()) return "usuario/cadastro";
    try {
      cadastro.cadastrar(empresa, dados);
    } catch (CadastroDuplicadoException ex) {
      erros.rejectValue(ex.getCampo(), "duplicado", ex.getMessage());
      return "usuario/cadastro";
    } catch (DataIntegrityViolationException ex) {
      // Outra requisição pode ter cadastrado o mesmo CNPJ/e-mail após a consulta inicial.
      erros.reject(
          "duplicado", "Não foi possível cadastrar. Verifique se o CNPJ ou e-mail já está em uso.");
      return "usuario/cadastro";
    }
    return "redirect:/login?cadastro";
  }

  private TipoEmpresa preparar(String tipo, Model model) {
    TipoEmpresa empresa =
        switch (tipo) {
          case "geradora" -> TipoEmpresa.GERADORA;
          case "coletora" -> TipoEmpresa.COLETORA;
          default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    model.addAttribute("tipo", tipo);
    return empresa;
  }
}
