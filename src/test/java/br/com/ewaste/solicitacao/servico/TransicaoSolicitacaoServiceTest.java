package br.com.ewaste.solicitacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.dominio.TransicaoInvalidaException;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TransicaoSolicitacaoServiceTest {

  SolicitacaoRepository repositorio = mock(SolicitacaoRepository.class);
  TransicaoSolicitacaoService servico = new TransicaoSolicitacaoService(repositorio);
  Solicitacao solicitacao;

  @BeforeEach
  void criarSolicitacao() {
    solicitacao =
        Solicitacao.abrir(
            1L, "PCs", new BigDecimal("250"), false, 0, null, "São Paulo", "SP", -23.5, -46.6);
    when(repositorio.findById(7L)).thenReturn(Optional.of(solicitacao));
  }

  @Test
  void marcaColetadaQuandoAgendada() {
    colocarEm(StatusSolicitacao.AGENDADA);

    servico.marcarColetada(7L);

    assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.COLETADA);
  }

  @Test
  void marcaConcluidaQuandoColetada() {
    colocarEm(StatusSolicitacao.COLETADA);

    servico.marcarConcluida(7L);

    assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.CONCLUIDA);
  }

  @Test
  void recusaColetarSolicitacaoAberta() {
    assertThatThrownBy(() -> servico.marcarColetada(7L))
        .isInstanceOf(TransicaoInvalidaException.class)
        .isInstanceOf(IllegalStateException.class);
    assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.ABERTA);
  }

  @Test
  void recusaConcluirSemTerColetado() {
    colocarEm(StatusSolicitacao.AGENDADA);

    assertThatThrownBy(() -> servico.marcarConcluida(7L))
        .isInstanceOf(TransicaoInvalidaException.class);
  }

  @Test
  void idInexistenteLancaNaoEncontrada() {
    when(repositorio.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servico.marcarColetada(99L))
        .isInstanceOf(SolicitacaoNaoEncontradaException.class);
  }

  /** O aceite e o agendamento ainda não existem neste ponto; o teste posiciona o status direto. */
  private void colocarEm(StatusSolicitacao status) {
    ReflectionTestUtils.setField(solicitacao, "status", status);
  }
}
