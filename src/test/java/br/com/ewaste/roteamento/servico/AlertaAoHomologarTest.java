package br.com.ewaste.roteamento.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.roteamento.dominio.AlertaSolicitacao;
import br.com.ewaste.roteamento.repositorio.AlertaSolicitacaoRepository;
import br.com.ewaste.shared.events.AlertasEnviadosEvent;
import br.com.ewaste.shared.events.ColetoraHomologadaEvent;
import br.com.ewaste.solicitacao.api.SolicitacaoConsulta;
import br.com.ewaste.solicitacao.api.SolicitacaoResumo;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.EmpresaResumo;
import br.com.ewaste.usuario.api.TipoEmpresa;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AlertaAoHomologarTest {

  static final long COLETORA = 8L;

  SolicitacaoConsulta solicitacoes = mock(SolicitacaoConsulta.class);
  ColetorasAptas aptas = mock(ColetorasAptas.class);
  EmpresaConsulta empresas = mock(EmpresaConsulta.class);
  AlertaSolicitacaoRepository alertas = mock(AlertaSolicitacaoRepository.class);
  ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
  AlertaAoHomologar ouvinte =
      new AlertaAoHomologar(solicitacoes, aptas, empresas, alertas, eventos);

  @Test
  void alertaSolicitacoesAbertasNoRaioSemRepetir() {
    when(aptas.listarAptas()).thenReturn(List.of(COLETORA));
    // Goiânia, raio de 30 km
    when(empresas.buscar(COLETORA))
        .thenReturn(
            Optional.of(
                new EmpresaResumo(
                    COLETORA,
                    TipoEmpresa.COLETORA,
                    "Teste",
                    "0",
                    "Goiânia",
                    "GO",
                    -16.6538,
                    -49.2442,
                    30)));
    SolicitacaoResumo perto = aberta(7L, -16.7290, -49.3474); // ~14 km
    SolicitacaoResumo jaAlertada = aberta(9L, -16.70, -49.25);
    SolicitacaoResumo longe = aberta(1L, -23.5629, -46.6544); // São Paulo
    when(solicitacoes.listarAbertas()).thenReturn(List.of(perto, jaAlertada, longe));
    when(alertas.findBySolicitacaoIdAndColetoraId(9L, COLETORA))
        .thenReturn(Optional.of(mock(AlertaSolicitacao.class)));

    ouvinte.aoHomologar(new ColetoraHomologadaEvent(COLETORA));

    ArgumentCaptor<AlertaSolicitacao> salvo = ArgumentCaptor.forClass(AlertaSolicitacao.class);
    verify(alertas).save(salvo.capture());
    assertThat(salvo.getValue().getSolicitacaoId()).isEqualTo(7L);
    assertThat(salvo.getValue().getColetoraId()).isEqualTo(COLETORA);
    verify(eventos).publishEvent(new AlertasEnviadosEvent(7L, List.of(COLETORA)));
  }

  @Test
  void coletoraNaoAptaNaoRecebeAlertas() {
    when(aptas.listarAptas()).thenReturn(List.of());

    ouvinte.aoHomologar(new ColetoraHomologadaEvent(COLETORA));

    verify(alertas, never()).save(any());
    verify(eventos, never()).publishEvent(any());
  }

  private static SolicitacaoResumo aberta(Long id, double lat, double lng) {
    return new SolicitacaoResumo(
        id,
        1L,
        null,
        StatusSolicitacao.ABERTA,
        BigDecimal.TEN,
        false,
        0,
        "c",
        "GO",
        lat,
        lng,
        null);
  }
}
