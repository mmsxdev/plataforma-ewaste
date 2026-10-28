package br.com.ewaste.armazenamento.servico;

import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.armazenamento.api.ArquivoRef;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementação real do {@link Armazenamento}: grava os arquivos em disco, na pasta configurada em
 * {@code app.storage.dir} (fora de static/, então nada é servido direto pelo navegador).
 *
 * <p>Erros de validação (arquivo vazio, grande demais, tipo proibido, pasta ou caminho inválido)
 * viram {@link IllegalArgumentException} com uma mensagem que pode ser mostrada ao usuário.
 */
@Service
class ArmazenamentoEmDisco implements Armazenamento {

  /** Limite de tamanho por arquivo: 10 MB (o mesmo do spring.servlet.multipart.max-file-size). */
  static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;

  /** Tipos aceitos (content type) e a extensão usada no nome gravado em disco. */
  private static final Map<String, String> EXTENSAO_POR_TIPO =
      Map.of("application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");

  /**
   * Assinatura binária (primeiros bytes) de cada tipo. O content type vem do navegador e pode ser
   * falsificado; conferir os bytes garante que o conteúdo é mesmo um PDF, JPG ou PNG.
   */
  private static final Map<String, byte[]> ASSINATURA_POR_TIPO =
      Map.of(
          "application/pdf", new byte[] {'%', 'P', 'D', 'F'},
          "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
          "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

  /**
   * Nome de pasta permitido: minúsculas, números e hífen, com subpastas (ex.: "homologacao/12").
   */
  private static final Pattern PASTA_VALIDA = Pattern.compile("[a-z0-9-]+(/[a-z0-9-]+)*");

  /** Pasta raiz onde todos os arquivos ficam, já normalizada para caminho absoluto. */
  private final Path raiz;

  ArmazenamentoEmDisco(@Value("${app.storage.dir}") String diretorio) {
    this.raiz = Path.of(diretorio).toAbsolutePath().normalize();
  }

  /** Upload vindo de formulário: lê os bytes e reaproveita o salvar(byte[]...). */
  @Override
  public ArquivoRef salvar(MultipartFile arquivo, String pasta) {
    if (arquivo == null || arquivo.isEmpty()) {
      throw new IllegalArgumentException("Selecione um arquivo para enviar.");
    }
    // Checa o tamanho antes de ler, para não carregar um arquivo enorme na memória à toa
    validarTamanho(arquivo.getSize());
    try {
      return salvar(
          arquivo.getBytes(), arquivo.getOriginalFilename(), arquivo.getContentType(), pasta);
    } catch (IOException e) {
      throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
    }
  }

  /** Arquivo gerado pelo sistema (ex.: PDF do Dev 4) ou já lido do formulário. */
  @Override
  public ArquivoRef salvar(byte[] conteudo, String nomeOriginal, String contentType, String pasta) {
    // 1. Validações: nada vai para o disco se alguma falhar
    if (conteudo == null || conteudo.length == 0) {
      throw new IllegalArgumentException("O arquivo está vazio.");
    }
    validarTamanho(conteudo.length);
    String tipo = normalizarTipo(contentType);
    validarConteudo(conteudo, tipo);
    validarPasta(pasta);

    // 2. Nome gerado (UUID): o nome original nunca vira caminho, o que evita colisão entre
    //    arquivos e impede nomes maliciosos como "../../config.yml"
    String caminho = pasta + "/" + UUID.randomUUID() + EXTENSAO_POR_TIPO.get(tipo);
    Path destino = resolverDentroDaRaiz(caminho);

    // 3. Grava: cria a pasta se ainda não existir e escreve os bytes
    try {
      Files.createDirectories(destino.getParent());
      Files.write(destino, conteudo);
    } catch (IOException e) {
      throw new UncheckedIOException("Falha ao gravar o arquivo " + caminho, e);
    }

    // 4. Devolve a referência que o chamador grava no banco (coluna *_ref)
    return new ArquivoRef(caminho, nomeOriginal, tipo, conteudo.length);
  }

  /**
   * Devolve o arquivo para download. Quem chama deve checar antes se o usuário pode vê-lo (aqui não
   * sabemos de quem é o arquivo).
   */
  @Override
  public Resource carregar(String caminho) {
    if (caminho == null || caminho.isBlank()) {
      throw new IllegalArgumentException("Caminho de arquivo vazio.");
    }
    Path arquivo = resolverDentroDaRaiz(caminho);
    if (!Files.isRegularFile(arquivo)) {
      throw new IllegalArgumentException("Arquivo não encontrado: " + caminho);
    }
    return new PathResource(arquivo);
  }

  // ---------------------------------------------------------------------------------------------
  // Validações auxiliares
  // ---------------------------------------------------------------------------------------------

  private static void validarTamanho(long tamanho) {
    if (tamanho > TAMANHO_MAXIMO) {
      throw new IllegalArgumentException("O arquivo passa do limite de 10 MB.");
    }
  }

  /** Aceita só PDF, JPG e PNG; ignora maiúsculas e parâmetros como "; charset=...". */
  private static String normalizarTipo(String contentType) {
    String tipo = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
    if (tipo.equals("image/jpg")) {
      tipo = "image/jpeg"; // alguns navegadores mandam a variante não oficial
    }
    if (!EXTENSAO_POR_TIPO.containsKey(tipo)) {
      throw new IllegalArgumentException("Tipo de arquivo não permitido. Envie PDF, JPG ou PNG.");
    }
    return tipo;
  }

  /** Confere se os primeiros bytes batem com o tipo declarado. */
  private static void validarConteudo(byte[] conteudo, String tipo) {
    byte[] assinatura = ASSINATURA_POR_TIPO.get(tipo);
    boolean confere =
        conteudo.length >= assinatura.length
            && Arrays.equals(conteudo, 0, assinatura.length, assinatura, 0, assinatura.length);
    if (!confere) {
      throw new IllegalArgumentException("O conteúdo do arquivo não corresponde ao tipo " + tipo);
    }
  }

  private static void validarPasta(String pasta) {
    if (pasta == null || !PASTA_VALIDA.matcher(pasta).matches()) {
      throw new IllegalArgumentException("Pasta inválida: " + pasta);
    }
  }

  /**
   * Transforma o caminho relativo em caminho absoluto e garante que ele continua dentro da raiz.
   * Bloqueia tentativas como "../../etc/passwd" (path traversal).
   */
  private Path resolverDentroDaRaiz(String caminho) {
    Path resolvido = raiz.resolve(caminho).normalize();
    if (!resolvido.startsWith(raiz)) {
      throw new IllegalArgumentException("Caminho fora da pasta de arquivos: " + caminho);
    }
    return resolvido;
  }
}
