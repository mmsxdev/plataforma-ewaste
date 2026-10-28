package br.com.ewaste.shared.web;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.ewaste.shared.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {
  @Autowired MockMvc mvc;

  @Test
  void inicioPublicoUsaLayoutEOfereceCadastro() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(content().string(containsString("/css/app.css")))
        .andExpect(content().string(containsString("/cadastro/geradora")))
        .andExpect(content().string(containsString("/cadastro/coletora")))
        .andExpect(content().string(containsString("/login")))
        .andExpect(content().string(not(containsString("/logout"))))
        .andExpect(content().string(containsString("Projeto acadêmico")));
  }

  @ParameterizedTest
  @CsvSource({
    "GERADORA,/geradora/solicitacoes,/coletora/oportunidades,/admin/homologacoes",
    "COLETORA,/coletora/oportunidades,/geradora/solicitacoes,/admin/homologacoes",
    "ADMIN,/admin/homologacoes,/geradora/solicitacoes,/coletora/oportunidades"
  })
  void navbarMostraAtalhosDoPerfil(String perfil, String permitido, String outro, String terceiro)
      throws Exception {
    mvc.perform(get("/").with(user("usuario").roles(perfil)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(permitido)))
        .andExpect(content().string(not(containsString(outro))))
        .andExpect(content().string(not(containsString(terceiro))))
        .andExpect(content().string(containsString("id=\"aviso-notificacoes\"")))
        .andExpect(content().string(containsString("/notificacoes")))
        .andExpect(content().string(containsString("/logout")))
        .andExpect(content().string(containsString("name=\"_csrf\"")));
  }
}
