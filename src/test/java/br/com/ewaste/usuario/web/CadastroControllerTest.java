package br.com.ewaste.usuario.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.ewaste.shared.security.SecurityConfig;
import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.servico.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(CadastroController.class)
@Import(SecurityConfig.class)
class CadastroControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean CadastroService cadastro;

  @Test
  void formulariosPublicos() throws Exception {
    mvc.perform(get("/cadastro/geradora")).andExpect(status().isOk());
    mvc.perform(get("/cadastro/coletora")).andExpect(status().isOk());
  }

  @Test
  void cadastroValidoRedirecionaParaLogin() throws Exception {
    mvc.perform(valido("geradora"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?cadastro"));
    verify(cadastro).cadastrar(eq(TipoEmpresa.GERADORA), any());
  }

  @Test
  void coletoraPrecisaDeRaio() throws Exception {
    mvc.perform(valido("coletora"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("cadastro", "raioAtendimentoKm"));
    verifyNoInteractions(cadastro);
  }

  @Test
  void coletoraValida() throws Exception {
    mvc.perform(valido("coletora").param("raioAtendimentoKm", "50"))
        .andExpect(redirectedUrl("/login?cadastro"));
    verify(cadastro).cadastrar(eq(TipoEmpresa.COLETORA), any());
  }

  @Test
  void dadosInvalidosRetornamErrosESenhaNaoRetornaAoHtml() throws Exception {
    mvc.perform(
            post("/cadastro/geradora")
                .with(csrf())
                .param("cnpj", "00000000000000")
                .param("email", "invalido")
                .param("senha", "curta"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasErrors("cadastro"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("value=\"curta\""))));
    verifyNoInteractions(cadastro);
  }

  @Test
  void duplicidadeExibeMensagem() throws Exception {
    when(cadastro.cadastrar(any(), any()))
        .thenThrow(new CadastroDuplicadoException("email", "Este e-mail já está cadastrado."));
    mvc.perform(valido("geradora"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("cadastro", "email"));
  }

  @Test
  void tipoNaoPermitidoNaoCriaAdmin() throws Exception {
    mvc.perform(get("/cadastro/admin")).andExpect(status().isNotFound());
    verifyNoInteractions(cadastro);
  }

  @Test
  void cadastroELogoutExigemCsrf() throws Exception {
    mvc.perform(post("/cadastro/geradora")).andExpect(status().isForbidden());
    mvc.perform(post("/logout").with(user("teste"))).andExpect(status().isForbidden());
  }

  private MockHttpServletRequestBuilder valido(String tipo) {
    return post("/cadastro/" + tipo)
        .with(csrf())
        .param("razaoSocial", "Empresa Teste")
        .param("cnpj", "11222333000181")
        .param("telefone", "11999999999")
        .param("endereco", "Rua A, 10")
        .param("cidade", "São Paulo")
        .param("uf", "SP")
        .param("latitude", "-23.5")
        .param("longitude", "-46.6")
        .param("nome", "Miguel")
        .param("email", "miguel@teste.com")
        .param("senha", "senha123");
  }
}
