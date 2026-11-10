package br.com.ewaste.documento.servico;

import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.documento.api.DadosEmissao;
import br.com.ewaste.documento.api.EmissorDocumentos;
import br.com.ewaste.documento.api.TipoDocumento;
import br.com.ewaste.shared.events.DocumentoEmitidoEvent;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DocumentoService implements EmissorDocumentos {
  private final JdbcTemplate jdbc;
  private final Armazenamento armazenamento;
  private final GeradorPdf gerador;
  private final ApplicationEventPublisher eventos;

  DocumentoService(
      JdbcTemplate jdbc,
      Armazenamento armazenamento,
      GeradorPdf gerador,
      ApplicationEventPublisher eventos) {
    this.jdbc = jdbc;
    this.armazenamento = armazenamento;
    this.gerador = gerador;
    this.eventos = eventos;
  }

  @Override
  public void emitir(DadosEmissao dados) {
    List<TipoDocumento> tipos =
        new ArrayList<>(List.of(TipoDocumento.CDF, TipoDocumento.LAUDO_TECNICO));
    if (!dados.midias().isEmpty()) {
      tipos.add(TipoDocumento.CERTIFICADO_DESTRUICAO);
    }
    for (TipoDocumento tipo : tipos) {
      String numero = tipo.name() + "-" + dados.solicitacaoId();
      byte[] pdf = gerador.gerar(dados, tipo, numero);
      String arquivo =
          armazenamento.salvar(pdf, numero + ".pdf", "application/pdf", "documentos").caminho();
      String codigo = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
      Long id =
          jdbc.queryForObject(
              """
          INSERT INTO documento_emitido
              (solicitacao_id, tipo, numero, arquivo_ref, hash_sha256, codigo_verificacao)
          VALUES (?, ?, ?, ?, ?, ?) RETURNING id
          """,
              Long.class,
              dados.solicitacaoId(),
              tipo.name(),
              numero,
              arquivo,
              hash(pdf),
              codigo);
      eventos.publishEvent(
          new DocumentoEmitidoEvent(id, dados.solicitacaoId(), dados.geradoraId(), tipo));
    }
  }

  @Transactional(readOnly = true)
  public List<DocumentoResumo> listarDaGeradora(Long geradoraId) {
    return jdbc.query(
        """
        SELECT d.id, d.solicitacao_id, d.tipo, d.numero, d.codigo_verificacao, d.emitido_em
          FROM documento_emitido d JOIN solicitacao s ON s.id = d.solicitacao_id
         WHERE s.geradora_id = ? ORDER BY d.emitido_em DESC, d.id DESC
        """,
        (rs, row) ->
            new DocumentoResumo(
                rs.getLong("id"),
                rs.getLong("solicitacao_id"),
                TipoDocumento.valueOf(rs.getString("tipo")),
                rs.getString("numero"),
                rs.getString("codigo_verificacao"),
                rs.getTimestamp("emitido_em").toInstant()),
        geradoraId);
  }

  @Transactional(readOnly = true)
  public ArquivoDocumento baixar(Long id, Long geradoraId) {
    List<String[]> encontrados =
        jdbc.query(
            """
        SELECT d.numero, d.arquivo_ref, s.geradora_id FROM documento_emitido d
          JOIN solicitacao s ON s.id = d.solicitacao_id
         WHERE d.id = ?
        """,
            (rs, row) ->
                new String[] {
                  rs.getString("numero"), rs.getString("arquivo_ref"), rs.getString("geradora_id")
                },
            id);
    if (encontrados.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado.");
    }
    String[] encontrado = encontrados.getFirst();
    if (!geradoraId.toString().equals(encontrado[2])) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Documento de outra empresa.");
    }
    return new ArquivoDocumento(encontrado[0] + ".pdf", armazenamento.carregar(encontrado[1]));
  }

  @Transactional(readOnly = true)
  public Optional<Verificacao> verificar(String codigo) {
    List<String[]> encontrados =
        jdbc.query(
            "SELECT numero, tipo, arquivo_ref, hash_sha256 FROM documento_emitido WHERE codigo_verificacao = ?",
            (rs, row) ->
                new String[] {
                  rs.getString("numero"),
                  rs.getString("tipo"),
                  rs.getString("arquivo_ref"),
                  rs.getString("hash_sha256")
                },
            codigo);
    if (encontrados.isEmpty()) {
      return Optional.empty();
    }
    String[] encontrado = encontrados.getFirst();
    boolean valido;
    try {
      valido = hash(armazenamento.carregar(encontrado[2])).equals(encontrado[3]);
    } catch (IllegalArgumentException | IOException e) {
      valido = false;
    }
    return Optional.of(
        new Verificacao(encontrado[0], TipoDocumento.valueOf(encontrado[1]), valido));
  }

  private static String hash(byte[] dados) {
    return HexFormat.of().formatHex(digest().digest(dados));
  }

  private static String hash(Resource arquivo) throws IOException {
    MessageDigest resumo = digest();
    try (InputStream entrada = new DigestInputStream(arquivo.getInputStream(), resumo)) {
      entrada.transferTo(java.io.OutputStream.nullOutputStream());
    }
    return HexFormat.of().formatHex(resumo.digest());
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
