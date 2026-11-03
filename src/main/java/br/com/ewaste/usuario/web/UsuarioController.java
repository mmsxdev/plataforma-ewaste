package br.com.ewaste.usuario.web;

import br.com.ewaste.usuario.api.UsuarioAtual;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UsuarioController {
  private final UsuarioAtual usuarioAtual;

  public UsuarioController(UsuarioAtual usuarioAtual) {
    this.usuarioAtual = usuarioAtual;
  }

  @GetMapping("/login")
  public String login() {
    return "usuario/login";
  }

  @GetMapping("/painel")
  public String painel(Model model) {
    model.addAttribute("usuario", usuarioAtual.atual());
    return "usuario/painel";
  }
}
