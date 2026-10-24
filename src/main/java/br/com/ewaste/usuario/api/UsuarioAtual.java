package br.com.ewaste.usuario.api;

/** Dá acesso ao usuário autenticado. Implementação: Dev 1 — Miguel. */
public interface UsuarioAtual {

  /**
   * @return o usuário autenticado na requisição atual
   * @throws IllegalStateException se não houver usuário autenticado
   */
  UsuarioLogado atual();
}
