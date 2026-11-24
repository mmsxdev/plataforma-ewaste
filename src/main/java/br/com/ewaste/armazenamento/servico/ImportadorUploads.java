package br.com.ewaste.armazenamento.servico;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importação explícita: mantém referências e arquivos originais; nunca substitui bytes existentes.
 */
@Component
@Profile("import-uploads")
@Order(0)
public class ImportadorUploads implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final Path raiz;

  public ImportadorUploads(JdbcTemplate jdbc, @Value("${app.storage.dir}") String diretorio) {
    this.jdbc = jdbc;
    this.raiz = Path.of(diretorio).toAbsolutePath().normalize();
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    var referencias =
        jdbc.queryForList(
            """
        SELECT arquivo_ref FROM documento_emitido
        UNION SELECT arquivo_ref FROM documento_homologacao
        UNION SELECT evidencia_ref FROM evento_rastreio WHERE evidencia_ref IS NOT NULL
        UNION SELECT evidencia_ref FROM midia_destruida WHERE evidencia_ref IS NOT NULL
        """,
            String.class);
    int importados = 0;
    for (String referencia : referencias) {
      if (Boolean.TRUE.equals(
          jdbc.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM arquivo_compartilhado WHERE caminho = ?)",
              Boolean.class,
              referencia))) {
        continue;
      }
      Path arquivo = raiz.resolve(referencia).normalize();
      if (!arquivo.startsWith(raiz)
          || !Files.isRegularFile(arquivo)
          || !arquivo.toRealPath().startsWith(raiz.toRealPath())) {
        throw new IllegalArgumentException(
            "Importação interrompida: referência sem arquivo local seguro. Originais preservados.");
      }
      ArmazenamentoEmDisco.validarTamanho(Files.size(arquivo));
      byte[] bytes = Files.readAllBytes(arquivo);
      String nome = arquivo.getFileName().toString();
      String tipo =
          nome.toLowerCase(Locale.ROOT).endsWith(".pdf")
              ? "application/pdf"
              : nome.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg";
      ArmazenamentoEmDisco.validarConteudo(bytes, tipo);
      jdbc.update(
          "INSERT INTO arquivo_compartilhado(caminho,nome_original,content_type,tamanho,conteudo) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
          referencia,
          nome,
          tipo,
          bytes.length,
          bytes);
      byte[] guardado =
          jdbc.queryForObject(
              "SELECT conteudo FROM arquivo_compartilhado WHERE caminho = ?",
              byte[].class,
              referencia);
      if (!Arrays.equals(bytes, guardado)) {
        throw new IllegalStateException(
            "Conteúdo concorrente diferente; importação revertida sem substituir arquivos.");
      }
      importados++;
    }
    System.out.println(
        "Importação de uploads concluída: " + importados + ". Arquivos locais preservados.");
  }
}
