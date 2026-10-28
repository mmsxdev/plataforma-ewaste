package br.com.ewaste.armazenamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.armazenamento.api.ArquivoRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

/** Testes do armazenamento em disco, usando uma pasta temporária apagada ao fim de cada teste. */
class ArmazenamentoEmDiscoTest {

  // Conteúdos mínimos que começam com a assinatura real de cada tipo
  static final byte[] PDF = "%PDF-1.7 conteudo de teste".getBytes();
  static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};
  static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};

  @TempDir Path pasta;

  ArmazenamentoEmDisco armazenamento;

  @BeforeEach
  void criar() {
    armazenamento = new ArmazenamentoEmDisco(pasta.toString());
  }

  // ---- Caminho feliz ----

  @Test
  void salvaUploadECarregaDeVolta() throws Exception {
    var upload = new MockMultipartFile("arquivo", "licenca.pdf", "application/pdf", PDF);

    ArquivoRef ref = armazenamento.salvar(upload, "homologacao/2");

    // Nome gerado: pasta + UUID + extensão do tipo; o nome original fica só na referência
    assertThat(ref.caminho()).matches("homologacao/2/[0-9a-f-]{36}\\.pdf");
    assertThat(ref.nomeOriginal()).isEqualTo("licenca.pdf");
    assertThat(ref.contentType()).isEqualTo("application/pdf");
    assertThat(ref.tamanho()).isEqualTo(PDF.length);

    // O arquivo existe em disco e volta com o mesmo conteúdo
    Resource carregado = armazenamento.carregar(ref.caminho());
    assertThat(carregado.getContentAsByteArray()).isEqualTo(PDF);
  }

  @Test
  void salvaArquivoGeradoPeloSistema() {
    ArquivoRef ref = armazenamento.salvar(PDF, "cdf-123.pdf", "application/pdf", "documento");

    assertThat(Files.exists(pasta.resolve(ref.caminho()))).isTrue();
  }

  @Test
  void aceitaPngEJpgComExtensaoCorreta() {
    assertThat(armazenamento.salvar(PNG, "foto.png", "image/png", "rastreio").caminho())
        .endsWith(".png");
    assertThat(armazenamento.salvar(JPG, "foto.jpg", "image/jpeg", "rastreio").caminho())
        .endsWith(".jpg");
    // variante não oficial enviada por alguns navegadores
    assertThat(armazenamento.salvar(JPG, "foto.jpg", "image/jpg", "rastreio").caminho())
        .endsWith(".jpg");
  }

  @Test
  void doisUploadsComMesmoNomeNaoSeSobrescrevem() {
    ArquivoRef a = armazenamento.salvar(PDF, "igual.pdf", "application/pdf", "homologacao");
    ArquivoRef b = armazenamento.salvar(PDF, "igual.pdf", "application/pdf", "homologacao");

    assertThat(a.caminho()).isNotEqualTo(b.caminho());
  }

  // ---- Recusas (critério de aceitação: arquivo grande ou de tipo proibido é recusado) ----

  @Test
  void recusaArquivoAcimaDe10Mb() {
    byte[] grande = Arrays.copyOf(PDF, (int) ArmazenamentoEmDisco.TAMANHO_MAXIMO + 1);
    var upload = new MockMultipartFile("arquivo", "grande.pdf", "application/pdf", grande);

    assertThatThrownBy(() -> armazenamento.salvar(upload, "homologacao"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("10 MB");
  }

  @Test
  void recusaTipoNaoPermitido() {
    var exe = new MockMultipartFile("arquivo", "virus.exe", "application/x-msdownload", PDF);

    assertThatThrownBy(() -> armazenamento.salvar(exe, "homologacao"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("PDF, JPG ou PNG");
  }

  @Test
  void recusaConteudoQueNaoBateComOTipoDeclarado() {
    // Arquivo de texto renomeado/declarado como PDF
    var falso = new MockMultipartFile("arquivo", "falso.pdf", "application/pdf", "oi".getBytes());

    assertThatThrownBy(() -> armazenamento.salvar(falso, "homologacao"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não corresponde");
  }

  @Test
  void recusaArquivoVazio() {
    var vazio = new MockMultipartFile("arquivo", "vazio.pdf", "application/pdf", new byte[0]);

    assertThatThrownBy(() -> armazenamento.salvar(vazio, "homologacao"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void recusaPastaInvalida() {
    for (String pastaRuim : new String[] {"../fora", "/absoluta", "Maiuscula", "", "a//b"}) {
      assertThatThrownBy(() -> armazenamento.salvar(PDF, "x.pdf", "application/pdf", pastaRuim))
          .as("pasta '%s'", pastaRuim)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void naoCarregaArquivoForaDaPasta() {
    assertThatThrownBy(() -> armazenamento.carregar("../../etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fora da pasta");
  }

  @Test
  void avisaQuandoArquivoNaoExiste() {
    assertThatThrownBy(() -> armazenamento.carregar("homologacao/nao-existe.pdf"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("não encontrado");
  }
}
