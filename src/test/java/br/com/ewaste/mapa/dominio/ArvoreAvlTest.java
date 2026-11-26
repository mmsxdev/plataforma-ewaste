package br.com.ewaste.mapa.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArvoreAvlTest {
  @Test
  void equilibraAsQuatroRotacoesEBuscaChaves() {
    for (int[] ordem : new int[][] {{3, 2, 1}, {1, 2, 3}, {3, 1, 2}, {1, 3, 2}}) {
      ArvoreAvl<Integer, String> arvore = new ArvoreAvl<>();
      for (int chave : ordem) arvore.inserir(chave, "p" + chave);
      assertThat(arvore.emOrdem()).containsExactly("p1", "p2", "p3");
      assertThat(arvore.altura()).isEqualTo(2);
      assertThat(arvore.buscar(2)).contains("p2");
      assertThat(arvore.buscar(9)).isEmpty();
    }
  }

  @Test
  void entradaOrdenadaNaoDegeneraEChaveRepetidaAtualizaSemDuplicar() {
    ArvoreAvl<Integer, Integer> arvore = new ArvoreAvl<>();
    assertThat(arvore.emOrdem()).isEmpty();
    for (int i = 0; i < 1000; i++) arvore.inserir(i, i);
    assertThat(arvore.altura()).isLessThanOrEqualTo(11);
    arvore.inserir(500, 9000);
    assertThat(arvore.buscar(500)).contains(9000);
    assertThat(arvore.emOrdem()).hasSize(1000);
  }
}
