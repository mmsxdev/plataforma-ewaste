package br.com.ewaste.solicitacao.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.ewaste.shared.security.SecurityConfig;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.dominio.TipoResiduo;
import br.com.ewaste.solicitacao.servico.AceiteService;
import br.com.ewaste.solicitacao.servico.Oportunidade;
import br.com.ewaste.solicitacao.servico.OportunidadeService;
import br.com.ewaste.solicitacao.servico.ResultadoAceite;
import br.com.ewaste.usuario.api.PerfilUsuario;
import br.com.ewaste.usuario.api.UsuarioAtual;
import br.com.ewaste.usuario.api.UsuarioLogado;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Telas da coletora: lista, detalhe, 404 de quem não foi alertado e matriz de acesso. */
@WebMvcTest(OportunidadeController.class)
@Import({SecurityConfig.class, DatasSolicitacao.class})
class OportunidadeControllerTest {

  static final long COLETORA = 2L;

  @Autowired MockMvc mvc;
  @MockitoBean OportunidadeService oportunidades;
  @MockitoBean AceiteService aceite;
  @MockitoBean UsuarioAtual usuarioAtual;

  @BeforeEach
  void coletoraLogada() {
    when(usuarioAtual.atual())
        .thenReturn(new UsuarioLogado(3L, COLETORA, "Carlos", PerfilUsuario.COLETORA));
  }

  @Test
  void listaMostraAsOportunidadesDaColetoraLogadaComDistancia() throws Exception {
    when(oportunidades.listarAbertas(COLETORA)).thenReturn(List.of(oportunidade(7L)));

    mvc.perform(get("/coletora/oportunidades").with(coletora()))
        .andExpect(status().isOk())
        .andExpect(view().name("solicitacao/oportunidades"))
        .andExpect(content().string(containsString("5,8 km")))
        .andExpect(content().string(containsString("Sim, 5")))
        .andExpect(content().string(containsString("/coletora/oportunidades/7")));
    verify(oportunidades).listarAbertas(COLETORA);
  }

  @Test
  void detalheDeOportunidadeAbertaTemBotaoDeAceitar() throws Exception {
    when(oportunidades.buscar(7L, COLETORA)).thenReturn(oportunidade(7L));

    mvc.perform(get("/coletora/oportunidades/7").with(coletora()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/coletora/oportunidades/7/aceitar")))
        .andExpect(content().string(containsString("Aceitar esta coleta")));
  }

  @Test
  void detalheDeOportunidadeJaAceitaNaoTemBotao() throws Exception {
    Oportunidade aceita = oportunidade(7L);
    ReflectionTestUtils.setField(aceita.solicitacao(), "status", StatusSolicitacao.ACEITA);
    when(oportunidades.buscar(7L, COLETORA)).thenReturn(aceita);

    mvc.perform(get("/coletora/oportunidades/7").with(coletora()))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("Aceitar esta coleta"))))
        .andExpect(content().string(containsString("não está mais disponível")));
  }

  @Test
  void detalheDeSolicitacaoNaoAlertadaResponde404() throws Exception {
    when(oportunidades.buscar(8L, COLETORA)).thenThrow(new SolicitacaoNaoEncontradaException(8L));

    mvc.perform(get("/coletora/oportunidades/8").with(coletora())).andExpect(status().isNotFound());
  }

  @Test
  void geradoraRecebe403NasRotasDaColetora() throws Exception {
    mvc.perform(get("/coletora/oportunidades").with(user("g").roles("GERADORA")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/coletora/oportunidades/7").with(user("g").roles("GERADORA")))
        .andExpect(status().isForbidden());
    verify(oportunidades, never()).listarAbertas(COLETORA);
  }

  // ---- Aceite ----

  @Test
  void aceiteVencedorVaiParaODetalheComMensagem() throws Exception {
    when(aceite.aceitar(7L, COLETORA)).thenReturn(ResultadoAceite.ACEITO);

    mvc.perform(post("/coletora/oportunidades/7/aceitar").with(coletora()).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/coletora/oportunidades/7"))
        .andExpect(flash().attribute("mensagem", "Coleta aceita! Agora agende a retirada."));
  }

  @Test
  void quemPerdeVoltaParaAListaComAvisoDeJaAceita() throws Exception {
    when(aceite.aceitar(7L, COLETORA)).thenReturn(ResultadoAceite.JA_ACEITA);

    mvc.perform(post("/coletora/oportunidades/7/aceitar").with(coletora()).with(csrf()))
        .andExpect(redirectedUrl("/coletora/oportunidades"))
        .andExpect(flash().attribute("erro", "Esta coleta já foi aceita por outra coletora."));
  }

  @Test
  void homologacaoInativaRecebeAviso() throws Exception {
    when(aceite.aceitar(7L, COLETORA)).thenReturn(ResultadoAceite.HOMOLOGACAO_INATIVA);

    mvc.perform(post("/coletora/oportunidades/7/aceitar").with(coletora()).with(csrf()))
        .andExpect(redirectedUrl("/coletora/oportunidades/7"))
        .andExpect(flash().attribute("erro", "Sua homologação não está ativa."));
  }

  @Test
  void aceiteDeOportunidadeNaoAlertadaResponde404() throws Exception {
    when(aceite.aceitar(8L, COLETORA)).thenThrow(new SolicitacaoNaoEncontradaException(8L));

    mvc.perform(post("/coletora/oportunidades/8/aceitar").with(coletora()).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void geradoraNaoAceita() throws Exception {
    mvc.perform(
            post("/coletora/oportunidades/7/aceitar")
                .with(user("g").roles("GERADORA"))
                .with(csrf()))
        .andExpect(status().isForbidden());
    verify(aceite, never()).aceitar(anyLong(), anyLong());
  }

  @Test
  void aceiteSemTokenCsrfEhRecusado() throws Exception {
    mvc.perform(post("/coletora/oportunidades/7/aceitar").with(coletora()))
        .andExpect(status().isForbidden());
    verify(aceite, never()).aceitar(anyLong(), anyLong());
  }

  private static RequestPostProcessor coletora() {
    return user("coletora@teste.com").roles("COLETORA");
  }

  private static Oportunidade oportunidade(Long id) {
    Solicitacao s =
        Solicitacao.abrir(
            1L,
            "HDs",
            new BigDecimal("250"),
            true,
            5,
            "Av. Paulista, 1000",
            "São Paulo",
            "SP",
            -23.56,
            -46.65);
    s.adicionarItem(TipoResiduo.MIDIA_ARMAZENAMENTO, 5, new BigDecimal("10"));
    ReflectionTestUtils.setField(s, "id", id);
    return new Oportunidade(s, new BigDecimal("5.83"));
  }
}
