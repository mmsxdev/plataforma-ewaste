package br.com.ewaste.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.mapa.api.PontosColeta;
import br.com.ewaste.solicitacao.servico.SolicitacaoService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Fluxo acadêmico: categoria -> consulta BD/AVL -> solicitação -> histórico; sucesso e recusa. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FluxoPiIT {
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired PontosColeta pontos;
  @Autowired SolicitacaoService solicitacoes;
  Long empresa;
  Long ponto;

  @BeforeEach
  void preparar() {
    jdbc.execute("TRUNCATE empresa, ponto_descarte RESTART IDENTITY CASCADE");
    empresa =
        jdbc.queryForObject(
            "INSERT INTO empresa(tipo,razao_social,cnpj,cidade,uf) VALUES ('GERADORA','Empresa fictícia','11222333000181','Goiânia','GO') RETURNING id",
            Long.class);
    jdbc.update(
        "INSERT INTO usuario(empresa_id,nome,email,senha_hash,perfil) VALUES (?,'Usuário fictício','pi@teste.com','hash','GERADORA')",
        empresa);
    ponto =
        jdbc.queryForObject(
            "INSERT INTO ponto_descarte(nome,cidade,uf,latitude,longitude,tipos_aceitos) VALUES ('Ponto fictício','Goiânia','GO',-16,-49,'INFORMATICA') RETURNING id",
            Long.class);
  }

  @Test
  void consultaBancoViaArvoreRegistraItemIdentificadoEAcompanha() throws Exception {
    assertThat(pontos.consultar("Goiânia", "INFORMATICA"))
        .extracting(p -> p.id())
        .containsExactly(ponto);
    assertThat(pontos.buscar(ponto)).isPresent();
    var formulario =
        mvc.perform(
                get("/geradora/solicitacoes/nova")
                    .param("pontoDescarteId", ponto.toString())
                    .param("tipoResiduo", "INFORMATICA")
                    .with(user("pi@teste.com").roles("GERADORA")))
            .andExpect(status().isOk())
            .andReturn()
            .getModelAndView()
            .getModel()
            .get("form");
    var selecionado = (br.com.ewaste.solicitacao.web.NovaSolicitacaoForm) formulario;
    assertThat(selecionado.getPontoDescarteId()).isEqualTo(ponto);
    assertThat(selecionado.getItens().getFirst().getTipoResiduo())
        .isEqualTo(br.com.ewaste.solicitacao.dominio.TipoResiduo.INFORMATICA);
    mvc.perform(get("/mapa").param("tipo", "INFORMATICA"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Ponto fictício")));
    mvc.perform(nova("INFORMATICA")).andExpect(status().is3xxRedirection());
    Long id = jdbc.queryForObject("SELECT id FROM solicitacao", Long.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT identificacao FROM solicitacao_item WHERE solicitacao_id=?",
                String.class,
                id))
        .isEqualTo("Notebook fictício");
    assertThat(
            jdbc.queryForObject(
                "SELECT ponto_descarte_id FROM solicitacao WHERE id=?", Long.class, id))
        .isEqualTo(ponto);
    mvc.perform(get("/geradora/solicitacoes/{id}", id).with(user("pi@teste.com").roles("GERADORA")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Notebook fictício")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Linha do tempo")));
    Long item =
        jdbc.queryForObject(
            "SELECT id FROM solicitacao_item WHERE solicitacao_id=?", Long.class, id);
    mvc.perform(
            get("/geradora/solicitacoes/{id}/editar", id)
                .with(user("pi@teste.com").roles("GERADORA")))
        .andExpect(status().isOk());
    mvc.perform(
            post("/geradora/solicitacoes/{id}/editar", id)
                .with(user("pi@teste.com").roles("GERADORA"))
                .with(csrf())
                .param("descricao", "Identificação revisada")
                .param("identificacoes[" + item + "]", "Notebook revisado"))
        .andExpect(status().is3xxRedirection());
    assertThat(
            jdbc.queryForObject(
                "SELECT identificacao FROM solicitacao_item WHERE id=?", String.class, item))
        .isEqualTo("Notebook revisado");
  }

  @Test
  void materialIncompativelEPontoInativoNaoPersistem() throws Exception {
    mvc.perform(nova("BATERIA")).andExpect(status().isOk()).andExpect(model().hasErrors());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitacao", Integer.class)).isZero();
    jdbc.update("UPDATE ponto_descarte SET ativo=false WHERE id=?", ponto);
    assertThat(pontos.consultar("", "")).isEmpty();
    mvc.perform(nova("INFORMATICA")).andExpect(status().isOk()).andExpect(model().hasErrors());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitacao", Integer.class)).isZero();
  }

  @Test
  void edicaoDeOutraEmpresaOuDepoisDoAceiteEhRejeitada() throws Exception {
    mvc.perform(nova("INFORMATICA")).andExpect(status().is3xxRedirection());
    Long id = jdbc.queryForObject("SELECT id FROM solicitacao", Long.class);
    assertThatThrownBy(() -> solicitacoes.editar(id, 999L, "Inválida", Map.of()))
        .isInstanceOf(br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException.class);
    assertThatThrownBy(
            () -> solicitacoes.editar(id, empresa, "Inválida", Map.of(999L, "Item forjado")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não pertence");
    jdbc.update("UPDATE solicitacao SET status='ACEITA' WHERE id=?", id);
    assertThatThrownBy(() -> solicitacoes.editar(id, empresa, "Inválida", Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(
            jdbc.queryForObject("SELECT descricao FROM solicitacao WHERE id=?", String.class, id))
        .isEqualTo("Descarte fictício");
  }

  @Test
  void pontoInexistenteNoFormularioOuPostNaoCriaSolicitacao() throws Exception {
    ponto = 999L;
    mvc.perform(
            get("/geradora/solicitacoes/nova")
                .param("pontoDescarteId", ponto.toString())
                .with(user("pi@teste.com").roles("GERADORA")))
        .andExpect(status().isNotFound());
    mvc.perform(nova("INFORMATICA")).andExpect(status().isOk()).andExpect(model().hasErrors());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitacao", Integer.class)).isZero();
  }

  @Test
  void cadastroAnteriorSemPontoOuIdentificacaoPermaneceValido() {
    // Fixture de registro preexistente: não abre exceção à validação de novos cadastros.
    Long id =
        jdbc.queryForObject(
            "INSERT INTO solicitacao(geradora_id,descricao,peso_estimado_kg,cidade,uf,latitude,longitude) VALUES (?,'Legado',2.50,'Goiânia','GO',-16,-49) RETURNING id",
            Long.class,
            empresa);
    jdbc.update(
        "INSERT INTO solicitacao_item(solicitacao_id,tipo_residuo,quantidade,peso_kg) VALUES (?,'INFORMATICA',1,2.50)",
        id);
    var antiga = solicitacoes.buscarDaGeradora(id, empresa);
    assertThat(antiga.getItens()).hasSize(1);
    assertThat(antiga.getItens().getFirst().getIdentificacao()).isNull();
    solicitacoes.editar(id, empresa, "Descrição do legado revisada", Map.of());
    assertThat(
            jdbc.queryForObject(
                "SELECT ponto_descarte_id FROM solicitacao WHERE id=?", Long.class, id))
        .isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT identificacao FROM solicitacao_item WHERE solicitacao_id=?",
                String.class,
                id))
        .isNull();
  }

  @Test
  void novosItensSemIdentificacaoSaoRejeitadosComESemPonto() throws Exception {
    for (boolean comPonto : new boolean[] {false, true}) {
      for (String identificacao : new String[] {null, "", "   "}) {
        mvc.perform(nova("INFORMATICA", comPonto, identificacao))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "itens[0].completo"));
      }
      var direta = novaEntidade(comPonto);
      assertThatThrownBy(() -> solicitacoes.criar(direta))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Identifique todos");
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitacao", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitacao_item", Integer.class))
        .isZero();
  }

  @Test
  void novoItemIdentificadoSemPontoEhAceito() throws Exception {
    mvc.perform(nova("INFORMATICA", false, "Notebook fictício"))
        .andExpect(status().is3xxRedirection());
    var salva = solicitacoes.listarDaGeradora(empresa).getFirst();
    assertThat(salva.getPontoDescarteId()).isNull();
    assertThat(
            solicitacoes
                .buscarDaGeradora(salva.getId(), empresa)
                .getItens()
                .getFirst()
                .getIdentificacao())
        .isEqualTo("Notebook fictício");
  }

  @Test
  void identificacaoExistenteNaoPodeSerApagadaNemSemPonto() throws Exception {
    for (boolean comPonto : new boolean[] {false, true}) {
      mvc.perform(nova("INFORMATICA", comPonto, "Notebook fictício"))
          .andExpect(status().is3xxRedirection());
      Long id = jdbc.queryForObject("SELECT max(id) FROM solicitacao", Long.class);
      Long item =
          jdbc.queryForObject(
              "SELECT id FROM solicitacao_item WHERE solicitacao_id=?", Long.class, id);
      assertThatThrownBy(
              () -> solicitacoes.editar(id, empresa, "Não deve gravar", Map.of(item, "  ")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("não pode ser apagada");
      mvc.perform(
              post("/geradora/solicitacoes/{id}/editar", id)
                  .with(user("pi@teste.com").roles("GERADORA"))
                  .with(csrf())
                  .param("descricao", "Não deve gravar")
                  .param("identificacoes[" + item + "]", ""))
          .andExpect(status().isOk())
          .andExpect(model().hasErrors());
      assertThat(
              jdbc.queryForObject(
                  "SELECT identificacao FROM solicitacao_item WHERE id=?", String.class, item))
          .isEqualTo("Notebook fictício");
      assertThat(
              jdbc.queryForObject("SELECT descricao FROM solicitacao WHERE id=?", String.class, id))
          .isEqualTo("Descarte fictício");
    }
  }

  @Test
  void pontoDesativadoPermaneceNoHistoricoMasNaoAceitaNovasSolicitacoes() throws Exception {
    mvc.perform(nova("INFORMATICA")).andExpect(status().is3xxRedirection());
    Long id = jdbc.queryForObject("SELECT id FROM solicitacao", Long.class);
    jdbc.update("UPDATE ponto_descarte SET ativo=false WHERE id=?", ponto);
    assertThat(pontos.buscar(ponto)).isEmpty();
    assertThat(pontos.consultar("", "")).isEmpty();
    assertThat(pontos.buscarParaHistorico(ponto))
        .get()
        .extracting(p -> p.nome())
        .isEqualTo("Ponto fictício");
    assertThat(pontos.buscarParaHistorico(null)).isEmpty();
    assertThat(pontos.buscarParaHistorico(999L)).isEmpty();
    mvc.perform(get("/geradora/solicitacoes/{id}", id).with(user("pi@teste.com").roles("GERADORA")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "Ponto de destino: <span>Ponto fictício</span>")));
    mvc.perform(
            get("/geradora/solicitacoes/nova")
                .param("pontoDescarteId", ponto.toString())
                .with(user("pi@teste.com").roles("GERADORA")))
        .andExpect(status().isNotFound());
    mvc.perform(nova("INFORMATICA")).andExpect(status().isOk()).andExpect(model().hasErrors());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM solicitacao", Integer.class)).isEqualTo(1);
  }

  private br.com.ewaste.solicitacao.dominio.Solicitacao novaEntidade(boolean comPonto) {
    var nova =
        br.com.ewaste.solicitacao.dominio.Solicitacao.abrir(
            empresa,
            "Nova",
            new java.math.BigDecimal("2.50"),
            false,
            0,
            null,
            "Goiânia",
            "GO",
            -16,
            -49);
    nova.adicionarItem(
        br.com.ewaste.solicitacao.dominio.TipoResiduo.INFORMATICA,
        1,
        new java.math.BigDecimal("2.50"));
    nova.definirPonto(comPonto ? ponto : null);
    return nova;
  }

  private MockHttpServletRequestBuilder nova(String tipo) {
    return nova(tipo, true, "Notebook fictício");
  }

  private MockHttpServletRequestBuilder nova(String tipo, boolean comPonto, String identificacao) {
    var requisicao =
        post("/geradora/solicitacoes")
            .with(user("pi@teste.com").roles("GERADORA"))
            .with(csrf())
            .param("descricao", "Descarte fictício")
            .param("pesoEstimadoKg", "2.50")
            .param("cidade", "Goiânia")
            .param("uf", "GO")
            .param("latitude", "-16")
            .param("longitude", "-49")
            .param("itens[0].tipoResiduo", tipo)
            .param("itens[0].quantidade", "1")
            .param("itens[0].pesoKg", "2.50");
    if (comPonto) requisicao.param("pontoDescarteId", ponto.toString());
    if (identificacao != null) requisicao.param("itens[0].identificacao", identificacao);
    return requisicao;
  }
}
