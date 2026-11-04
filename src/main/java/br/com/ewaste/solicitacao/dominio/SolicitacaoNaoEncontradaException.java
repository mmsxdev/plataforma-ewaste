package br.com.ewaste.solicitacao.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Solicitação inexistente ou que não pertence a quem pediu. Nos dois casos a resposta é 404, para
 * não revelar que o id existe.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SolicitacaoNaoEncontradaException extends RuntimeException {

  public SolicitacaoNaoEncontradaException(Long id) {
    super("Solicitação não encontrada: " + id);
  }
}
