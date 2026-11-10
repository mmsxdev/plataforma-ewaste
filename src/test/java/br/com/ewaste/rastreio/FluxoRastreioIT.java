package br.com.ewaste.rastreio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.documento.api.TipoDocumento;
import br.com.ewaste.documento.servico.DocumentoService;
import br.com.ewaste.rastreio.servico.RastreioService;
import br.com.ewaste.shared.events.SolicitacaoAceitaEvent;
import br.com.ewaste.shared.events.SolicitacaoCriadaEvent;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = "app.storage.dir=target/test-arquivos-dev4")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FluxoRastreioIT {
  @Autowired JdbcTemplate jdbc;
  @Autowired RastreioService rastreio;
  @Autowired DocumentoService documentos;
  @Autowired MockMvc mvc;
  @Autowired ApplicationEventPublisher publicador;
  @Autowired TransactionTemplate transacao;

  Long geradora;
  Long outraGeradora;
  Long coletora;
  Long usuario;
  Long solicitacao;

  @BeforeEach
  void preparar() {
    jdbc.execute("TRUNCATE empresa RESTART IDENTITY CASCADE");
    geradora = empresa("GERADORA", "11222333000181");
    outraGeradora = empresa("GERADORA", "55666777000181");
    coletora = empresa("COLETORA", "22333444000181");
    usuario =
        jdbc.queryForObject(
            "INSERT INTO usuario (empresa_id, nome, email, senha_hash, perfil) VALUES (?, 'Coletora', 'coletora@teste.com', 'hash', 'COLETORA') RETURNING id",
            Long.class,
            coletora);
    jdbc.update(
        "INSERT INTO usuario (empresa_id, nome, email, senha_hash, perfil) VALUES (?, 'Geradora', 'geradora@teste.com', 'hash', 'GERADORA')",
        geradora);
    jdbc.update(
        "INSERT INTO usuario (empresa_id, nome, email, senha_hash, perfil) VALUES (?, 'Outra', 'outra@teste.com', 'hash', 'GERADORA')",
        outraGeradora);
    solicitacao =
        jdbc.queryForObject(
            """
        INSERT INTO solicitacao
            (geradora_id, peso_estimado_kg, contem_midias, qtd_midias, cidade, uf,
             latitude, longitude, status, coletora_id, agendada_para)
        VALUES (?, 250, true, 2, 'São Paulo', 'SP', -23.56, -46.65,
                'AGENDADA', ?, now() + interval '1 day') RETURNING id
        """,
            Long.class,
            geradora,
            coletora);
  }

  @Test
  void regraR3ExigeColetaDestinacaoETodasAsMidias() {
    assertThatThrownBy(() -> rastreio.concluir(solicitacao, coletora, usuario))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(quantidadeDocumentos()).isZero();

    rastreio.registrarColeta(solicitacao, coletora, usuario, new BigDecimal("247.50"), null);
    assertThatThrownBy(() -> rastreio.concluir(solicitacao, coletora, usuario))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("destinação");

    rastreio.registrarDestinacao(solicitacao, coletora, usuario, "Reciclagem licenciada");
    assertThatThrownBy(() -> rastreio.concluir(solicitacao, coletora, usuario))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mídias");
    assertThat(quantidadeDocumentos()).isZero();
  }

  @Test
  void eventosAutomaticosEntramNaLinhaDoTempoUmaVez() {
    transacao.executeWithoutResult(
        status -> {
          publicador.publishEvent(new SolicitacaoCriadaEvent(solicitacao, geradora));
          publicador.publishEvent(new SolicitacaoCriadaEvent(solicitacao, geradora));
          publicador.publishEvent(new SolicitacaoAceitaEvent(solicitacao, geradora, coletora));
        });

    assertThat(rastreio.listar(solicitacao)).extracting("tipo").containsExactly("CRIADA", "ACEITA");
    assertThat(rastreio.listar(solicitacao))
        .allSatisfy(
            e -> {
              assertThat(e.autor()).isEqualTo("Sistema");
              assertThat(e.registradoEm()).isNotNull();
            });
  }

  @Test
  void emiteTresPdfsComSeriesEVerificaHashSemLiberarOutraGeradora() throws Exception {
    mvc.perform(
            get("/coletora/coletas/{id}", solicitacao)
                .with(user("coletora@teste.com").roles("COLETORA")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Registrar retirada")));
    MockMultipartFile foto =
        new MockMultipartFile(
            "foto",
            "coleta.png",
            "image/png",
            new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
    rastreio.registrarColeta(solicitacao, coletora, usuario, new BigDecimal("247.50"), foto);
    rastreio.registrarMidia(
        solicitacao, coletora, usuario, "HD-001", "HDD", "TRITURACAO", "H-4", null);
    rastreio.registrarMidia(
        solicitacao, coletora, usuario, "HD-002", "HDD", "TRITURACAO", "H-4", null);
    rastreio.registrarDestinacao(solicitacao, coletora, usuario, "Reciclagem licenciada");
    rastreio.concluir(solicitacao, coletora, usuario);

    assertThat(statusSolicitacao()).isEqualTo("CONCLUIDA");
    assertThat(quantidadeDocumentos()).isEqualTo(3);
    assertThat(rastreio.listar(solicitacao))
        .extracting("tipo")
        .contains("COLETADA", "MIDIAS_DESTRUIDAS", "DESTINADA", "CONCLUIDA");

    var certificado =
        documentos.listarDaGeradora(geradora).stream()
            .filter(d -> d.tipo() == TipoDocumento.CERTIFICADO_DESTRUICAO)
            .findFirst()
            .orElseThrow();
    var arquivo = documentos.baixar(certificado.id(), geradora).arquivo();
    try (PdfReader pdf = new PdfReader(arquivo.getInputStream())) {
      String texto = new PdfTextExtractor(pdf).getTextFromPage(1);
      assertThat(texto).contains("HD-001", "HD-002", "DOCUMENTO SIMULADO");
    }
    assertThat(documentos.verificar(certificado.codigoVerificacao()).orElseThrow().valido())
        .isTrue();
    mvc.perform(
            get("/geradora/documentos/{id}/baixar", certificado.id())
                .with(user("geradora@teste.com").roles("GERADORA")))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/pdf"));
    mvc.perform(
            get("/geradora/documentos/{id}/baixar", certificado.id())
                .with(user("outra@teste.com").roles("GERADORA")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/verificar/{codigo}", certificado.codigoVerificacao()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Documento confirmado")));
    mvc.perform(
            get("/geradora/solicitacoes/{id}", solicitacao)
                .with(user("geradora@teste.com").roles("GERADORA")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Linha do tempo")));
    assertThatThrownBy(() -> documentos.baixar(certificado.id(), outraGeradora))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

    Files.writeString(arquivo.getFile().toPath(), "arquivo alterado");
    assertThat(documentos.verificar(certificado.codigoVerificacao()).orElseThrow().valido())
        .isFalse();
    mvc.perform(get("/verificar/{codigo}", certificado.codigoVerificacao()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Arquivo alterado")));
    assertThat(
            rastreio.listar(solicitacao).stream()
                .filter(e -> e.tipo().equals("COLETADA"))
                .findFirst()
                .orElseThrow()
                .evidenciaRef())
        .isNotBlank();
  }

  private Long empresa(String tipo, String cnpj) {
    return jdbc.queryForObject(
        "INSERT INTO empresa (tipo, razao_social, cnpj, cidade, uf) VALUES (?, ?, ?, 'São Paulo', 'SP') RETURNING id",
        Long.class,
        tipo,
        "Empresa " + cnpj,
        cnpj);
  }

  private int quantidadeDocumentos() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM documento_emitido WHERE solicitacao_id = ?",
        Integer.class,
        solicitacao);
  }

  private String statusSolicitacao() {
    return jdbc.queryForObject(
        "SELECT status FROM solicitacao WHERE id = ?", String.class, solicitacao);
  }
}
