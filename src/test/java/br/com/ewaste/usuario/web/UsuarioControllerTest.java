package br.com.ewaste.usuario.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.ewaste.shared.security.SecurityConfig;
import br.com.ewaste.shared.web.HomeController;
import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.servico.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.web.bind.annotation.*;

@WebMvcTest({UsuarioController.class, CadastroController.class, HomeController.class})
@Import({SecurityConfig.class, UsuarioControllerTest.ProbeController.class})
class UsuarioControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CadastroService cadastro;
  @MockitoBean UsuarioAtual atual;

  @RestController
  static class ProbeController {
    @GetMapping({"/geradora/teste", "/coletora/teste", "/admin/teste", "/api/public/teste"})
    String teste() {
      return "OK";
    }
  }

  static Stream<Arguments> matriz() {
    return Stream.of("GERADORA", "COLETORA", "ADMIN")
        .flatMap(
            perfil ->
                Stream.of("geradora", "coletora", "admin")
                    .map(
                        prefixo ->
                            Arguments.of(
                                perfil, prefixo, perfil.equalsIgnoreCase(prefixo) ? 200 : 403)));
  }

  @ParameterizedTest
  @MethodSource("matriz")
  void protegePrefixosPorPerfil(String perfil, String prefixo, int esperado) throws Exception {
    mvc.perform(get("/" + prefixo + "/teste").with(user("teste").roles(perfil)))
        .andExpect(status().is(esperado));
  }

  @Test
  void publicoELoginAcessiveisSemSessao() throws Exception {
    for (String rota :
        new String[] {
          "/", "/login", "/cadastro/geradora", "/cadastro/coletora", "/api/public/teste"
        }) mvc.perform(get(rota)).andExpect(status().isOk());
    mvc.perform(get("/painel"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
  }

  @Test
  void painelRenderizaSomenteAtalhosDoPerfil() throws Exception {
    when(atual.atual()).thenReturn(new UsuarioLogado(1L, 2L, "Miguel", PerfilUsuario.GERADORA));
    mvc.perform(get("/painel").with(user("miguel").roles("GERADORA")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/geradora/solicitacoes")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/admin/homologacoes"))));
  }
}
