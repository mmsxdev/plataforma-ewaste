package br.com.ewaste.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Página inicial pública. Dono: Dev 1 (Miguel). */
@Controller
public class HomeController {

  @GetMapping("/")
  public String inicio() {
    return "index";
  }
}
