package br.com.ewaste.mapa.web;

import br.com.ewaste.mapa.api.PontosColeta;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class PontosColetaController {
  private final PontosColeta pontos;

  PontosColetaController(PontosColeta pontos) {
    this.pontos = pontos;
  }

  @GetMapping("/mapa")
  String listar(
      @RequestParam(defaultValue = "") String cidade,
      @RequestParam(defaultValue = "") String tipo,
      Model model) {
    model.addAttribute("pontos", pontos.consultar(cidade, tipo));
    model.addAttribute("cidade", cidade);
    model.addAttribute("tipo", tipo);
    model.addAttribute(
        "categorias",
        java.util.List.of(
            "INFORMATICA",
            "TELEFONIA",
            "ELETRODOMESTICO",
            "MIDIA_ARMAZENAMENTO",
            "BATERIA",
            "OUTRO"));
    return "mapa/pontos";
  }
}
