package br.com.ewaste.solicitacao.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ewaste.solicitacao.dominio.TipoResiduo;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ItemFormTest {
  static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

  @AfterAll
  static void encerrar() {
    FACTORY.close();
  }

  @Test
  void linhaInteiramenteVaziaPermaneceOpcional() {
    assertThat(FACTORY.getValidator().validate(new ItemForm())).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void materialPreenchidoExigeIdentificacaoNaoBranca(String identificacao) {
    ItemForm item = completo(identificacao);
    assertThat(FACTORY.getValidator().validate(item))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("completo");
  }

  @Test
  void identificacaoComDadosValidosEhAceita() {
    assertThat(FACTORY.getValidator().validate(completo("Notebook fictício"))).isEmpty();
  }

  private ItemForm completo(String identificacao) {
    ItemForm item = new ItemForm();
    item.setIdentificacao(identificacao);
    item.setTipoResiduo(TipoResiduo.INFORMATICA);
    item.setQuantidade(1);
    item.setPesoKg(new BigDecimal("2.50"));
    return item;
  }
}
