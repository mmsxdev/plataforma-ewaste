package br.com.ewaste.usuario.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class CnpjTest {
  @ParameterizedTest
  @ValueSource(strings = {"11222333000181", "11.222.333/0001-81", "04.252.011/0001-10"})
  void aceitaCnpjComDigitosCorretos(String valor) {
    assertThat(Cnpj.valido(valor)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "00000000000000",
        "11111111111111",
        "11222333000180",
        "123",
        "ab11222333000181",
        "11.222.333/0001-82"
      })
  void recusaCnpjInvalido(String valor) {
    assertThat(Cnpj.valido(valor)).isFalse();
  }
}
