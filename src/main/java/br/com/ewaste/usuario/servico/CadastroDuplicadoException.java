package br.com.ewaste.usuario.servico;

public class CadastroDuplicadoException extends RuntimeException {
  private final String campo;

  public CadastroDuplicadoException(String campo, String mensagem) {
    super(mensagem);
    this.campo = campo;
  }

  public String getCampo() {
    return campo;
  }
}
