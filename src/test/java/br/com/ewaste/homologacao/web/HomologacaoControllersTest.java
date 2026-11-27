package br.com.ewaste.homologacao.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.servico.HomologacaoService;
import br.com.ewaste.shared.security.SecurityConfig;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.PerfilUsuario;
import br.com.ewaste.usuario.api.UsuarioAtual;
import br.com.ewaste.usuario.api.UsuarioLogado;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Telas de homologação: acesso por perfil e ações principais. */
@WebMvcTest({HomologacaoColetoraController.class, AdminHomologacaoController.class})
@Import(SecurityConfig.class)
class HomologacaoControllersTest {

  @Autowired MockMvc mvc;
  @MockitoBean HomologacaoService servico;
  @MockitoBean UsuarioAtual usuarioAtual;
  @MockitoBean EmpresaConsulta empresas;

  @Test
  void coletoraVeSituacaoEFormularioDeDocumentos() throws Exception {
    when(usuarioAtual.atual())
        .thenReturn(new UsuarioLogado(5L, 8L, "Coletora", PerfilUsuario.COLETORA));
    when(servico.daColetora(8L)).thenReturn(Homologacao.pendente(8L));

    mvc.perform(get("/coletora/homologacao").with(user("c").roles("COLETORA")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Licença ambiental (obrigatório)")))
        .andExpect(content().string(containsString("Enviar para análise")));
  }

  @Test
  void somenteCadaPerfilAcessaSuaTela() throws Exception {
    mvc.perform(get("/coletora/homologacao").with(user("g").roles("GERADORA")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/admin/homologacoes").with(user("c").roles("COLETORA")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminListaEAprova() throws Exception {
    when(usuarioAtual.atual())
        .thenReturn(new UsuarioLogado(1L, null, "Admin", PerfilUsuario.ADMIN));
    when(servico.todas()).thenReturn(List.of(Homologacao.pendente(8L)));
    when(empresas.buscar(8L)).thenReturn(Optional.empty());

    mvc.perform(get("/admin/homologacoes").with(user("a").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Coletora nº 8")));

    mvc.perform(post("/admin/homologacoes/3/aprovar").with(user("a").roles("ADMIN")).with(csrf()))
        .andExpect(redirectedUrl("/admin/homologacoes"));
    verify(servico).aprovar(3L, 1L);
  }

  @Test
  void documentoDeOutraColetoraRetorna404SemArquivoNemDetalheInterno() throws Exception {
    when(usuarioAtual.atual())
        .thenReturn(new UsuarioLogado(5L, 8L, "Coletora", PerfilUsuario.COLETORA));
    when(servico.documentoDaColetora(8L, 99L))
        .thenThrow(new IllegalArgumentException("Documento pertence a outra empresa."));

    mvc.perform(get("/coletora/homologacao/documentos/99").with(user("c").roles("COLETORA")))
        .andExpect(status().isNotFound())
        .andExpect(content().string(""))
        .andExpect(header().doesNotExist("Content-Disposition"));
    verify(servico).documentoDaColetora(8L, 99L);
  }

  @Test
  void adminComHomologacaoInexistenteRetorna404SemDetalheInterno() throws Exception {
    when(servico.documentoParaAdmin(999L, 55L))
        .thenThrow(new IllegalArgumentException("Homologação não encontrada."));

    mvc.perform(get("/admin/homologacoes/999/documentos/55").with(user("a").roles("ADMIN")))
        .andExpect(status().isNotFound())
        .andExpect(content().string(""))
        .andExpect(header().doesNotExist("Content-Disposition"));
    verify(servico).documentoParaAdmin(999L, 55L);
  }
}
