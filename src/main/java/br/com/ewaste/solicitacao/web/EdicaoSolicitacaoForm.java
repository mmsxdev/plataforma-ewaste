package br.com.ewaste.solicitacao.web;

import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

public class EdicaoSolicitacaoForm {
  @Size(max = 1000)
  private String descricao;

  @jakarta.validation.constraints.NotNull
  @Size(max = 100)
  private Map<Long, @Size(max = 150) String> identificacoes = new LinkedHashMap<>();

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String valor) {
    descricao = valor;
  }

  public Map<Long, String> getIdentificacoes() {
    return identificacoes;
  }

  public void setIdentificacoes(Map<Long, String> valor) {
    identificacoes = valor;
  }
}
