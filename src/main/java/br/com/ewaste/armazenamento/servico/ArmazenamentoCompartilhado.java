package br.com.ewaste.armazenamento.servico;

import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.armazenamento.api.ArquivoRef;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Arquivos do protótipo compartilhados na mesma instância PostgreSQL, sem SMB ou disco por cliente.
 */
@Service
@Profile("team")
@Transactional
public class ArmazenamentoCompartilhado implements Armazenamento {
  private final JdbcTemplate jdbc;

  public ArmazenamentoCompartilhado(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ArquivoRef salvar(MultipartFile arquivo, String pasta) {
    if (arquivo == null || arquivo.isEmpty()) {
      throw new IllegalArgumentException("Selecione um arquivo para enviar.");
    }
    ArmazenamentoEmDisco.validarTamanho(arquivo.getSize());
    try {
      return salvar(
          arquivo.getBytes(), arquivo.getOriginalFilename(), arquivo.getContentType(), pasta);
    } catch (IOException e) {
      throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
    }
  }

  @Override
  public ArquivoRef salvar(byte[] conteudo, String nomeOriginal, String contentType, String pasta) {
    if (conteudo == null || conteudo.length == 0) {
      throw new IllegalArgumentException("O arquivo está vazio.");
    }
    ArmazenamentoEmDisco.validarTamanho(conteudo.length);
    String tipo = ArmazenamentoEmDisco.normalizarTipo(contentType);
    ArmazenamentoEmDisco.validarConteudo(conteudo, tipo);
    ArmazenamentoEmDisco.validarPasta(pasta);
    String caminho =
        pasta + "/" + UUID.randomUUID() + ArmazenamentoEmDisco.EXTENSAO_POR_TIPO.get(tipo);
    jdbc.update(
        "INSERT INTO arquivo_compartilhado (caminho, nome_original, content_type, tamanho, conteudo) VALUES (?, ?, ?, ?, ?)",
        caminho,
        nomeOriginal,
        tipo,
        conteudo.length,
        conteudo);
    return new ArquivoRef(caminho, nomeOriginal, tipo, conteudo.length);
  }

  @Override
  @Transactional(readOnly = true)
  public Resource carregar(String caminho) {
    if (caminho == null
        || caminho.isBlank()
        || caminho.contains("..")
        || caminho.startsWith("/")
        || caminho.contains("\\")) {
      throw new IllegalArgumentException("Referência de arquivo inválida.");
    }
    var arquivos =
        jdbc.query(
            "SELECT conteudo FROM arquivo_compartilhado WHERE caminho = ?",
            (rs, row) -> rs.getBytes(1),
            caminho);
    if (arquivos.isEmpty()) {
      throw new IllegalArgumentException("Arquivo não encontrado no armazenamento compartilhado.");
    }
    return new ByteArrayResource(arquivos.getFirst()) {
      @Override
      public String getFilename() {
        return caminho.substring(caminho.lastIndexOf('/') + 1);
      }
    };
  }
}
