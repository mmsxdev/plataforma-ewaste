package br.com.ewaste.documento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.armazenamento.api.ArquivoRef;
import br.com.ewaste.documento.api.DadosEmissao;
import br.com.ewaste.documento.api.TipoDocumento;
import br.com.ewaste.shared.events.DocumentoEmitidoEvent;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Teste unitário do módulo documento: gerador de PDF real, banco, armazenamento e eventos
 * simulados. O fluxo completo com PostgreSQL continua em FluxoRastreioIT.
 */
class DocumentoServiceTest {

  static final long SOLICITACAO = 10L;
  static final long GERADORA = 3L;
  static final DadosEmissao.Midia HD1 =
      new DadosEmissao.Midia("HD-001", "HDD", "TRITURACAO", "H-4");
  static final DadosEmissao.Midia HD2 =
      new DadosEmissao.Midia("HD-002", "SSD", "TRITURACAO", "H-5");

  JdbcTemplate jdbc = mock(JdbcTemplate.class);
  Armazenamento armazenamento = mock(Armazenamento.class);
  ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
  DocumentoService servico = new DocumentoService(jdbc, armazenamento, new GeradorPdf(), eventos);

  /** PDFs entregues ao armazenamento, pelo nome do arquivo (ex.: CDF-10.pdf). */
  Map<String, byte[]> salvos = new LinkedHashMap<>();

  /** Parâmetros de cada INSERT em documento_emitido, na ordem do SQL. */
  List<Object[]> inseridos = new ArrayList<>();

  @BeforeEach
  void preparar() {
    when(armazenamento.salvar(
            any(byte[].class), anyString(), eq("application/pdf"), eq("documentos")))
        .thenAnswer(
            inv -> {
              byte[] pdf = inv.getArgument(0);
              String nome = inv.getArgument(1);
              salvos.put(nome, pdf);
              return new ArquivoRef("documentos/" + nome, nome, "application/pdf", pdf.length);
            });
    when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
        .thenAnswer(
            inv -> {
              Object[] todos = inv.getArguments();
              inseridos.add(Arrays.copyOfRange(todos, 2, todos.length));
              return (long) inseridos.size();
            });
  }

  @Test
  void semMidiasEmiteSomenteCdfELaudo() throws IOException {
    servico.emitir(dados(List.of()));

    assertThat(inseridos).extracting(p -> p[1]).containsExactly("CDF", "LAUDO_TECNICO");
    assertThat(salvos).containsOnlyKeys("CDF-10.pdf", "LAUDO_TECNICO-10.pdf");
    assertThat(texto(salvos.get("CDF-10.pdf")))
        .contains(
            "Certificado de Destinação Final",
            "Comprovante de destinação final",
            "Número: CDF-10",
            "247.50 kg",
            "Reciclagem licenciada",
            "Sem validade legal")
        .doesNotContain("HD-001");
    assertThat(texto(salvos.get("LAUDO_TECNICO-10.pdf")))
        .contains("Laudo Técnico", "Laudo técnico da coleta", "Número: LAUDO_TECNICO-10");
    verify(eventos)
        .publishEvent(new DocumentoEmitidoEvent(1L, SOLICITACAO, GERADORA, TipoDocumento.CDF));
    verify(eventos)
        .publishEvent(
            new DocumentoEmitidoEvent(2L, SOLICITACAO, GERADORA, TipoDocumento.LAUDO_TECNICO));
  }

  @Test
  void comMidiasEmiteTambemOCertificadoComOsNumerosDeSerie() throws IOException {
    servico.emitir(dados(List.of(HD1, HD2)));

    assertThat(inseridos)
        .extracting(p -> p[1])
        .containsExactly("CDF", "LAUDO_TECNICO", "CERTIFICADO_DESTRUICAO");
    assertThat(texto(salvos.get("CERTIFICADO_DESTRUICAO-10.pdf")))
        .contains(
            "Certificado de Destruição de Mídias",
            "HD-001 — HDD — TRITURACAO — nível H-4",
            "HD-002 — SSD — TRITURACAO — nível H-5");
    verify(eventos)
        .publishEvent(
            new DocumentoEmitidoEvent(
                3L, SOLICITACAO, GERADORA, TipoDocumento.CERTIFICADO_DESTRUICAO));
  }

  @Test
  void registraHashSha256DoArquivoSalvoECodigoDeVerificacao() throws Exception {
    servico.emitir(dados(List.of()));

    Object[] cdf = inseridos.getFirst();
    // (solicitacao_id, tipo, numero, arquivo_ref, hash_sha256, codigo_verificacao)
    assertThat(cdf[0]).isEqualTo(SOLICITACAO);
    assertThat(cdf[2]).isEqualTo("CDF-10");
    assertThat(cdf[3]).isEqualTo("documentos/CDF-10.pdf");
    assertThat(cdf[4]).isEqualTo(sha256(salvos.get("CDF-10.pdf")));
    assertThat((String) cdf[5]).matches("[0-9A-F]{12}");
    assertThat(inseridos.get(1)[5]).isNotEqualTo(cdf[5]);
  }

  @Test
  void verificacaoFalhaQuandoOArquivoMuda() throws Exception {
    servico.emitir(dados(List.of()));
    byte[] original = salvos.get("CDF-10.pdf");
    String hashRegistrado = (String) inseridos.getFirst()[4];
    registrarNoBanco("ABC123DEF456", "CDF-10", "documentos/CDF-10.pdf", hashRegistrado);

    when(armazenamento.carregar("documentos/CDF-10.pdf"))
        .thenReturn(new ByteArrayResource(original));
    assertThat(servico.verificar("ABC123DEF456"))
        .hasValue(new Verificacao("CDF-10", TipoDocumento.CDF, true));

    byte[] alterado = original.clone();
    alterado[alterado.length / 2] ^= 1; // um único bit trocado
    when(armazenamento.carregar("documentos/CDF-10.pdf"))
        .thenReturn(new ByteArrayResource(alterado));
    assertThat(servico.verificar("ABC123DEF456"))
        .hasValue(new Verificacao("CDF-10", TipoDocumento.CDF, false));
  }

  @Test
  void verificacaoFalhaQuandoOArquivoSumiu() {
    registrarNoBanco("ABC123DEF456", "CDF-10", "documentos/CDF-10.pdf", "qualquer-hash");
    when(armazenamento.carregar("documentos/CDF-10.pdf"))
        .thenThrow(new IllegalArgumentException("Arquivo não encontrado."));

    assertThat(servico.verificar("ABC123DEF456"))
        .hasValue(new Verificacao("CDF-10", TipoDocumento.CDF, false));
  }

  private static DadosEmissao dados(List<DadosEmissao.Midia> midias) {
    return new DadosEmissao(
        SOLICITACAO, GERADORA, new BigDecimal("247.50"), "Reciclagem licenciada", midias);
  }

  /** Simula a linha de documento_emitido que a verificação procura pelo código. */
  @SuppressWarnings("unchecked")
  private void registrarNoBanco(String codigo, String numero, String caminho, String hash) {
    when(jdbc.query(anyString(), any(RowMapper.class), eq(codigo)))
        .thenAnswer(
            inv -> {
              ResultSet linha = mock(ResultSet.class);
              when(linha.getString("numero")).thenReturn(numero);
              when(linha.getString("tipo")).thenReturn("CDF");
              when(linha.getString("arquivo_ref")).thenReturn(caminho);
              when(linha.getString("hash_sha256")).thenReturn(hash);
              RowMapper<Object> mapeador = inv.getArgument(1);
              return List.of(mapeador.mapRow(linha, 0));
            });
  }

  private static String texto(byte[] pdf) throws IOException {
    try (PdfReader leitor = new PdfReader(pdf)) {
      return new PdfTextExtractor(leitor).getTextFromPage(1);
    }
  }

  private static String sha256(byte[] dados) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados));
  }
}
