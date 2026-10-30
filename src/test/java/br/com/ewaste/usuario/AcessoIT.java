package br.com.ewaste.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.dominio.*;
import br.com.ewaste.usuario.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AcessoIT {
  @Autowired MockMvc mvc;
  @Autowired EmpresaRepository empresas;
  @Autowired UsuarioRepository usuarios;
  @Autowired PasswordEncoder encoder;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void isolarDados() {
    jdbc.execute("truncate table empresa, usuario restart identity cascade");
  }

  @ParameterizedTest
  @CsvSource({
    "GERADORA,/geradora/solicitacoes",
    "COLETORA,/coletora/oportunidades",
    "ADMIN,/admin/homologacoes"
  })
  void loginAbrePainelDoPerfilELogoutEncerraSessao(String perfil, String atalho) throws Exception {
    Long empresaId = null;
    if (!perfil.equals("ADMIN")) {
      empresaId =
          empresas
              .saveAndFlush(
                  new Empresa(
                      TipoEmpresa.valueOf(perfil),
                      "Empresa acesso",
                      "04252011000110",
                      "11999999999",
                      "Rua A",
                      "São Paulo",
                      "SP",
                      -23.5,
                      -46.6,
                      50))
              .getId();
    }
    usuarios.saveAndFlush(
        new Usuario(
            empresaId,
            "Responsável",
            "acesso@integracao.com",
            encoder.encode("senha123"),
            PerfilUsuario.valueOf(perfil)));
    var login =
        mvc.perform(formLogin().user("acesso@integracao.com").password("senha123"))
            .andExpect(authenticated().withRoles(perfil))
            .andExpect(redirectedUrl("/painel"))
            .andReturn();
    var session = (MockHttpSession) login.getRequest().getSession(false);
    assertThat(session).isNotNull();
    mvc.perform(get("/painel").session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(atalho)));
    mvc.perform(post("/logout").session(session).with(csrf()))
        .andExpect(redirectedUrl("/"))
        .andExpect(unauthenticated());
    mvc.perform(get("/painel")).andExpect(redirectedUrlPattern("**/login"));
  }
}
