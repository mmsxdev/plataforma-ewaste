package br.com.ewaste.armazenamento.servico;

import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.armazenamento.api.ArquivoRef;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Fallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementação PROVISÓRIA (PR 0): guarda os arquivos só na memória (somem ao reiniciar). Apague
 * quando a implementação real (em disco) entrar.
 */
@Fallback
@Component
class ArmazenamentoProvisorio implements Armazenamento {

  private final Map<String, byte[]> arquivos = new ConcurrentHashMap<>();

  @Override
  public ArquivoRef salvar(MultipartFile arquivo, String pasta) {
    try {
      return salvar(
          arquivo.getBytes(), arquivo.getOriginalFilename(), arquivo.getContentType(), pasta);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public ArquivoRef salvar(byte[] conteudo, String nomeOriginal, String contentType, String pasta) {
    String caminho = pasta + "/" + UUID.randomUUID();
    arquivos.put(caminho, conteudo);
    return new ArquivoRef(caminho, nomeOriginal, contentType, conteudo.length);
  }

  @Override
  public Resource carregar(String caminho) {
    byte[] conteudo = arquivos.get(caminho);
    if (conteudo == null) {
      throw new IllegalArgumentException("Arquivo não encontrado: " + caminho);
    }
    return new ByteArrayResource(conteudo);
  }
}
