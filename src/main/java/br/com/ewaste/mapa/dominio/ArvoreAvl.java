package br.com.ewaste.mapa.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Árvore AVL própria: inserção e busca O(log n), percurso ordenado O(n). Sem estado global. */
public final class ArvoreAvl<K extends Comparable<K>, V> {
  private No<K, V> raiz;

  public void inserir(K chave, V valor) {
    raiz = inserir(raiz, chave, valor);
  }

  public Optional<V> buscar(K chave) {
    No<K, V> atual = raiz;
    while (atual != null) {
      int c = chave.compareTo(atual.chave);
      if (c == 0) return Optional.of(atual.valor);
      atual = c < 0 ? atual.esquerda : atual.direita;
    }
    return Optional.empty();
  }

  public List<V> emOrdem() {
    List<V> valores = new ArrayList<>();
    percorrer(raiz, valores);
    return List.copyOf(valores);
  }

  public int altura() {
    return altura(raiz);
  }

  private void percorrer(No<K, V> no, List<V> valores) {
    if (no == null) return;
    percorrer(no.esquerda, valores);
    valores.add(no.valor);
    percorrer(no.direita, valores);
  }

  private No<K, V> inserir(No<K, V> no, K chave, V valor) {
    if (no == null) return new No<>(chave, valor);
    int c = chave.compareTo(no.chave);
    if (c < 0) no.esquerda = inserir(no.esquerda, chave, valor);
    else if (c > 0) no.direita = inserir(no.direita, chave, valor);
    else {
      no.valor = valor;
      return no;
    }
    atualizar(no);
    int equilibrio = altura(no.esquerda) - altura(no.direita);
    if (equilibrio > 1) {
      if (chave.compareTo(no.esquerda.chave) > 0) no.esquerda = esquerda(no.esquerda);
      return direita(no);
    }
    if (equilibrio < -1) {
      if (chave.compareTo(no.direita.chave) < 0) no.direita = direita(no.direita);
      return esquerda(no);
    }
    return no;
  }

  private No<K, V> direita(No<K, V> no) {
    No<K, V> novo = no.esquerda;
    no.esquerda = novo.direita;
    novo.direita = no;
    atualizar(no);
    atualizar(novo);
    return novo;
  }

  private No<K, V> esquerda(No<K, V> no) {
    No<K, V> novo = no.direita;
    no.direita = novo.esquerda;
    novo.esquerda = no;
    atualizar(no);
    atualizar(novo);
    return novo;
  }

  private int altura(No<K, V> no) {
    return no == null ? 0 : no.altura;
  }

  private void atualizar(No<K, V> no) {
    no.altura = 1 + Math.max(altura(no.esquerda), altura(no.direita));
  }

  private static final class No<K, V> {
    final K chave;
    V valor;
    int altura = 1;
    No<K, V> esquerda;
    No<K, V> direita;

    No(K chave, V valor) {
      this.chave = chave;
      this.valor = valor;
    }
  }
}
