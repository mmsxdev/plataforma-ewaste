package br.com.ewaste.solicitacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Todas as combinações origem x destino (7 x 7) do ciclo de vida da solicitação. */
class TransicoesSolicitacaoTest {

  /** Escrito à mão, independente da implementação: é a especificação do ciclo. */
  static final Set<String> VALIDAS =
      Set.of(
          "ABERTA>ACEITA",
          "ABERTA>CANCELADA",
          "ABERTA>EXPIRADA",
          "ACEITA>AGENDADA",
          "AGENDADA>COLETADA",
          "COLETADA>CONCLUIDA");

  static Stream<Arguments> todasAsCombinacoes() {
    return Arrays.stream(StatusSolicitacao.values())
        .flatMap(
            origem ->
                Arrays.stream(StatusSolicitacao.values())
                    .map(
                        destino ->
                            Arguments.of(
                                origem, destino, VALIDAS.contains(origem + ">" + destino))));
  }

  @ParameterizedTest(name = "{0} -> {1}: válida = {2}")
  @MethodSource("todasAsCombinacoes")
  void podeIrParaSegueOCiclo(StatusSolicitacao origem, StatusSolicitacao destino, boolean valida) {
    assertThat(TransicoesSolicitacao.podeIrPara(origem, destino)).isEqualTo(valida);
  }

  @ParameterizedTest(name = "{0} -> {1}: válida = {2}")
  @MethodSource("todasAsCombinacoes")
  void exigirLancaExcecaoSoNasInvalidas(
      StatusSolicitacao origem, StatusSolicitacao destino, boolean valida) {
    if (valida) {
      assertThatCode(() -> TransicoesSolicitacao.exigir(origem, destino))
          .doesNotThrowAnyException();
    } else {
      assertThatThrownBy(() -> TransicoesSolicitacao.exigir(origem, destino))
          .isInstanceOf(TransicaoInvalidaException.class)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining(origem.name())
          .hasMessageContaining(destino.name());
    }
  }
}
