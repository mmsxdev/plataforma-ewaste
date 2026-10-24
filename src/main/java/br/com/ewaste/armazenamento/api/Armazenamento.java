package br.com.ewaste.armazenamento.api;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda e devolve arquivos (licenças, evidências, PDFs gerados). Implementação: Dev 2 — Felipe.
 * Usado por: Dev 2 e Dev 4 — Guilherme.
 *
 * <p>Quem chama é responsável por checar se o usuário pode ver o arquivo antes de devolvê-lo.
 */
public interface Armazenamento {

  /** Upload vindo de formulário. Recusa arquivo acima do limite ou de tipo não permitido. */
  ArquivoRef salvar(MultipartFile arquivo, String pasta);

  /** Arquivo gerado pelo sistema (ex.: PDF de um documento). */
  ArquivoRef salvar(byte[] conteudo, String nomeOriginal, String contentType, String pasta);

  Resource carregar(String caminho);
}
