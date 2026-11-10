package br.com.ewaste.solicitacao.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.ewaste.roteamento.api.AlertaConsulta;
import br.com.ewaste.shared.security.SecurityConfig;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.dominio.TipoResiduo;
import br.com.ewaste.solicitacao.servico.SolicitacaoService;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.PerfilUsuario;
import br.com.ewaste.usuario.api.UsuarioAtual;
import br.com.ewaste.usuario.api.UsuarioLogado;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Telas da geradora: validação do formulário, matriz de acesso por perfil e dono do recurso. */
@WebMvcTest(SolicitacaoController.class)
@Import({SecurityConfig.class, DatasSolicitacao.class})
class SolicitacaoControllerTest {

  static final long GERADORA = 1L;

  @Autowired MockMvc mvc;
  @MockitoBean SolicitacaoService servico;
  @MockitoBean UsuarioAtual usuarioAtual;
  @MockitoBean EmpresaConsulta empresas;
  @MockitoBean AlertaConsulta alertas;

  static final String SEM_COLETORA = "Nenhuma coletora apta na sua região no momento.";

  @BeforeEach
  void geradoraLogada() {
    when(usuarioAtual.atual())
        .thenReturn(new UsuarioLogado(2L, GERADORA, "Gabriela", PerfilUsuario.GERADORA));
  }

  // ---- Formulário ----

  @Test
  void formularioAbreComTresLinhasDeItem() throws Exception {
    mvc.perform(get("/geradora/solicitacoes/nova").with(geradora()))
        .andExpect(status().isOk())
        .andExpect(view().name("solicitacao/nova"))
        .andExpect(content().string(containsString("itens[2].tipoResiduo")));
  }

  @Test
  void postValidoSalvaComAGeradoraDaSessaoERedirecionaParaODetalhe() throws Exception {
    when(servico.criar(any())).thenReturn(42L);

    mvc.perform(valido().param("geradoraId", "999")) // tentativa de forjar a geradora: ignorada
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/geradora/solicitacoes/42"));

    var captor = ArgumentCaptor.forClass(Solicitacao.class);
    verify(servico).criar(captor.capture());
    Solicitacao salva = captor.getValue();
    assertThat(salva.getGeradoraId()).isEqualTo(GERADORA);
    assertThat(salva.getPesoEstimadoKg()).isEqualByComparingTo("250");
    assertThat(salva.getUf()).isEqualTo("SP");
    assertThat(salva.getItens()).hasSize(1); // as 2 linhas vazias foram ignoradas
    assertThat(salva.getItens().get(0).getTipoResiduo()).isEqualTo(TipoResiduo.INFORMATICA);
  }

  @Test
  void ufComEspacosEmVoltaEhAceitaESalvaSemEles() throws Exception {
    when(servico.criar(any())).thenReturn(42L);

    mvc.perform(valido("uf", " sp ", "cidade", " São Paulo "))
        .andExpect(redirectedUrl("/geradora/solicitacoes/42"));

    var captor = ArgumentCaptor.forClass(Solicitacao.class);
    verify(servico).criar(captor.capture());
    assertThat(captor.getValue().getUf()).isEqualTo("SP");
    assertThat(captor.getValue().getCidade()).isEqualTo("São Paulo");
  }

  @Test
  void ufComTresLetrasEhRecusada() throws Exception {
    mvc.perform(valido("uf", "SPX")).andExpect(model().attributeHasFieldErrors("form", "uf"));
    verify(servico, never()).criar(any());
  }

  @Test
  void pesoZeroERecusadoNaPropriaTela() throws Exception {
    mvc.perform(valido("pesoEstimadoKg", "0"))
        .andExpect(status().isOk())
        .andExpect(view().name("solicitacao/nova"))
        .andExpect(model().attributeHasFieldErrors("form", "pesoEstimadoKg"))
        .andExpect(content().string(containsString("O peso estimado deve ser maior que zero.")));
    verify(servico, never()).criar(any());
  }

  @Test
  void pesoNegativoERecusado() throws Exception {
    mvc.perform(valido("pesoEstimadoKg", "-5"))
        .andExpect(model().attributeHasFieldErrors("form", "pesoEstimadoKg"));
    verify(servico, never()).criar(any());
  }

  @Test
  void semLocalERecusadoEMantemOQueFoiDigitado() throws Exception {
    mvc.perform(valido("latitude", "", "longitude", ""))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "latitude", "longitude"))
        .andExpect(content().string(containsString("value=\"250\"")));
    verify(servico, never()).criar(any());
  }

  @Test
  void semItensERecusado() throws Exception {
    mvc.perform(
            valido("itens[0].tipoResiduo", "", "itens[0].quantidade", "", "itens[0].pesoKg", ""))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "itensPreenchidos"))
        .andExpect(content().string(containsString("Informe pelo menos um item.")));
    verify(servico, never()).criar(any());
  }

  @Test
  void itemIncompletoERecusado() throws Exception {
    mvc.perform(valido("itens[0].pesoKg", ""))
        .andExpect(model().attributeHasFieldErrors("form", "itens[0].completo"));
    verify(servico, never()).criar(any());
  }

  @Test
  void midiasSemQuantidadeSaoRecusadas() throws Exception {
    mvc.perform(valido("contemMidias", "true"))
        .andExpect(model().attributeHasFieldErrors("form", "qtdMidiasValida"));
    verify(servico, never()).criar(any());
  }

  // ---- Lista e detalhe (dono do recurso) ----

  @Test
  void listaPedeSoAsSolicitacoesDaEmpresaLogada() throws Exception {
    when(servico.listarDaGeradora(GERADORA)).thenReturn(List.of(solicitacao()));

    mvc.perform(get("/geradora/solicitacoes").with(geradora()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("São Paulo/SP")));
    verify(servico).listarDaGeradora(GERADORA);
  }

  @Test
  void detalheMostraOsDadosDaPropriaSolicitacao() throws Exception {
    when(servico.buscarDaGeradora(7L, GERADORA)).thenReturn(solicitacao());
    when(alertas.contarAlertas(7L)).thenReturn(2L);

    mvc.perform(get("/geradora/solicitacoes/7").with(geradora()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("250,00 kg")))
        .andExpect(content().string(containsString("Aguardando o aceite de uma coletora")))
        .andExpect(content().string(not(containsString(SEM_COLETORA))))
        .andExpect(content().string(containsString("id=\"linha-do-tempo\"")));
  }

  @Test
  void detalheAvisaQuandoNenhumaColetoraFoiAlertada() throws Exception {
    when(servico.buscarDaGeradora(7L, GERADORA)).thenReturn(solicitacao());
    when(alertas.contarAlertas(7L)).thenReturn(0L);

    mvc.perform(get("/geradora/solicitacoes/7").with(geradora()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(SEM_COLETORA)));
  }

  @Test
  void detalheDeSolicitacaoDeOutraEmpresaResponde404() throws Exception {
    when(servico.buscarDaGeradora(eq(8L), anyLong()))
        .thenThrow(new SolicitacaoNaoEncontradaException(8L));

    mvc.perform(get("/geradora/solicitacoes/8").with(geradora())).andExpect(status().isNotFound());
  }

  // ---- Matriz de acesso (SecurityConfig) ----

  @Test
  void coletoraRecebe403NasRotasDaGeradora() throws Exception {
    mvc.perform(get("/geradora/solicitacoes").with(user("c").roles("COLETORA")))
        .andExpect(status().isForbidden());
    mvc.perform(valido().with(user("c").roles("COLETORA"))).andExpect(status().isForbidden());
    verify(servico, never()).criar(any());
  }

  @Test
  void anonimoEhLevadoAoLogin() throws Exception {
    mvc.perform(get("/geradora/solicitacoes"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
  }

  // ---- Auxiliares ----

  private static org.springframework.test.web.servlet.request.RequestPostProcessor geradora() {
    return user("geradora@teste.com").roles("GERADORA");
  }

  /**
   * POST com um formulário válido. Cada teste passa pares (campo, valor) que SUBSTITUEM o padrão:
   * {@code param()} do MockMvc acrescentaria um segundo valor em vez de trocar o primeiro.
   */
  private MockHttpServletRequestBuilder valido(String... sobrescritas) {
    Map<String, String> campos = new LinkedHashMap<>();
    campos.put("descricao", "Computadores antigos");
    campos.put("pesoEstimadoKg", "250");
    campos.put("endereco", "Av. Paulista, 1000");
    campos.put("cidade", "São Paulo");
    campos.put("uf", "sp");
    campos.put("latitude", "-23.5614");
    campos.put("longitude", "-46.6559");
    campos.put("itens[0].tipoResiduo", "INFORMATICA");
    campos.put("itens[0].quantidade", "10");
    campos.put("itens[0].pesoKg", "250");
    for (int i = 0; i < sobrescritas.length; i += 2) {
      campos.put(sobrescritas[i], sobrescritas[i + 1]);
    }
    var requisicao = post("/geradora/solicitacoes").with(geradora()).with(csrf());
    campos.forEach(requisicao::param);
    return requisicao;
  }

  private static Solicitacao solicitacao() {
    Solicitacao s =
        Solicitacao.abrir(
            GERADORA,
            "PCs",
            new BigDecimal("250"),
            false,
            0,
            null,
            "São Paulo",
            "SP",
            -23.56,
            -46.65);
    s.adicionarItem(TipoResiduo.INFORMATICA, 10, new BigDecimal("250"));
    return s;
  }
}
