package br.com.ewaste.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.armazenamento.servico.ArmazenamentoCompartilhado;
import br.com.ewaste.armazenamento.servico.ImportadorUploads;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "spring.flyway.enabled=true",
      "spring.datasource.url=jdbc:postgresql://localhost/isolado",
      "spring.datasource.username=test",
      "spring.datasource.password=test"
    })
@ActiveProfiles("team")
@Import(TestcontainersConfiguration.class)
class ArmazenamentoCompartilhadoIT {
  @Autowired Armazenamento arquivos;
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transacao;

  @Test
  void outraInstanciaLeOMesmoConteudoSemPastaLocal() throws Exception {
    byte[] bytes = "%PDF-1.7 compartilhado ficticio".getBytes(StandardCharsets.UTF_8);
    var ref = arquivos.salvar(bytes, "teste.pdf", "application/pdf", "documentos");
    var outraInstancia = new ArmazenamentoCompartilhado(new JdbcTemplate(jdbc.getDataSource()));
    assertThat(arquivos).isInstanceOf(ArmazenamentoCompartilhado.class);
    assertThat(outraInstancia.carregar(ref.caminho()).getContentAsByteArray()).isEqualTo(bytes);
    assertThat(outraInstancia.carregar(ref.caminho()).getFilename()).endsWith(".pdf");
  }

  @Test
  void rollbackDaOperacaoTambemDesfazOArquivo() {
    long antes = contar();
    transacao.executeWithoutResult(
        status -> {
          arquivos.salvar(
              "%PDF-1.7 rollback".getBytes(StandardCharsets.UTF_8),
              "r.pdf",
              "application/pdf",
              "documentos");
          status.setRollbackOnly();
        });
    assertThat(contar()).isEqualTo(antes);
  }

  @Test
  void tipoForjadoETamanhoExcessivoNaoPersistem() {
    long antes = contar();
    assertThatThrownBy(
            () -> arquivos.salvar("invalido".getBytes(), "x.pdf", "application/pdf", "documentos"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> arquivos.salvar(new byte[10485761], "x.pdf", "application/pdf", "documentos"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> arquivos.carregar("../fora.pdf"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(contar()).isEqualTo(antes);
  }

  private long contar() {
    return jdbc.queryForObject("SELECT count(*) FROM arquivo_compartilhado", Long.class);
  }

  @Test
  void importaLegadoSemApagarArquivoENaoDuplica(@TempDir Path pasta) throws Exception {
    String caminho = "documentos/legado-" + UUID.randomUUID() + ".pdf";
    Path local = pasta.resolve(caminho);
    Files.createDirectories(local.getParent());
    byte[] bytes = "%PDF-1.7 legado ficticio".getBytes(StandardCharsets.UTF_8);
    Files.write(local, bytes);
    transacao.executeWithoutResult(
        status -> {
          Long empresa =
              jdbc.queryForObject(
                  "INSERT INTO empresa(tipo,razao_social,cnpj,cidade,uf) VALUES ('GERADORA','Importacao ficticia','99887766000155','Cidade ficticia','GO') RETURNING id",
                  Long.class);
          Long solicitacao =
              jdbc.queryForObject(
                  "INSERT INTO solicitacao(geradora_id,peso_estimado_kg,cidade,uf,latitude,longitude) VALUES (?,1,'Cidade ficticia','GO',0,0) RETURNING id",
                  Long.class,
                  empresa);
          jdbc.update(
              "INSERT INTO documento_emitido(solicitacao_id,tipo,numero,arquivo_ref) VALUES (?,'CDF',?,?)",
              solicitacao,
              "LEGADO-" + UUID.randomUUID().toString().replace("-", ""),
              caminho);
          importar(pasta);
          long depois = contar();
          importar(pasta);
          assertThat(contar()).isEqualTo(depois);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT conteudo FROM arquivo_compartilhado WHERE caminho=?",
                      byte[].class,
                      caminho))
              .isEqualTo(bytes);
          status.setRollbackOnly();
        });
    assertThat(Files.readAllBytes(local)).isEqualTo(bytes);
  }

  private void importar(Path pasta) {
    try {
      new ImportadorUploads(jdbc, pasta.toString()).run(new DefaultApplicationArguments());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
