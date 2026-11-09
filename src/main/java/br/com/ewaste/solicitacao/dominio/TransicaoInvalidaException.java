package br.com.ewaste.solicitacao.dominio;

import br.com.ewaste.solicitacao.api.StatusSolicitacao;

/**
 * Tentativa de mudar o status da solicitação por um caminho que o ciclo de vida não permite (ex.:
 * ABERTA -> COLETADA).
 *
 * <p>Estende {@link IllegalStateException} porque é isso que o contrato {@code
 * TransicaoSolicitacao} promete: outros módulos não podem importar esta classe (ArchUnit), então
 * capturam a exceção pela superclasse.
 */
public class TransicaoInvalidaException extends IllegalStateException {

  private final StatusSolicitacao origem;
  private final StatusSolicitacao destino;

  public TransicaoInvalidaException(StatusSolicitacao origem, StatusSolicitacao destino) {
    super("Não é possível passar a solicitação de " + origem + " para " + destino + ".");
    this.origem = origem;
    this.destino = destino;
  }

  public StatusSolicitacao getOrigem() {
    return origem;
  }

  public StatusSolicitacao getDestino() {
    return destino;
  }
}
