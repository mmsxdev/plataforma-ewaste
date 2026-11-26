package br.com.ewaste.homologacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.armazenamento.api.ArquivoRef;
import br.com.ewaste.homologacao.api.StatusHomologacao;
import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao;
import br.com.ewaste.homologacao.repositorio.HomologacaoRepository;
import br.com.ewaste.shared.events.ColetoraHomologadaEvent;
import br.com.ewaste.shared.events.ColetoraReprovadaEvent;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

class HomologacaoServiceTest {

  static final long COLETORA = 8L;
  static final long ADMIN = 1L;

  HomologacaoRepository repositorio = mock(HomologacaoRepository.class);
  Armazenamento armazenamento = mock(Armazenamento.class);
  ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
  HomologacaoService servico = new HomologacaoService(repositorio, armazenamento, eventos);
  Homologacao homologacao = Homologacao.pendente(COLETORA);
  MockMultipartFile pdf =
      new MockMultipartFile(
          "arquivo", "doc.pdf", "application/pdf", new byte[] {'%', 'P', 'D', 'F'});

  @BeforeEach
  void setUp() {
    when(repositorio.findComDocumentosByEmpresaId(COLETORA)).thenReturn(Optional.of(homologacao));
    when(repositorio.findComDocumentosById(3L)).thenReturn(Optional.of(homologacao));
    when(armazenamento.salvar(any(MockMultipartFile.class), eq("homologacao")))
        .thenReturn(new ArquivoRef("homologacao/x.pdf", "doc.pdf", "application/pdf", 4));
  }

  @Test
  void naoEnviaParaAnaliseSemDocumentosObrigatorios() {
    servico.enviarDocumento(COLETORA, TipoDocumentoHomologacao.LICENCA_AMBIENTAL, null, pdf);

    assertThatThrownBy(() -> servico.enviarParaAnalise(COLETORA))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cadastro IBAMA");
    assertThat(homologacao.getStatus()).isEqualTo(StatusHomologacao.PENDENTE);
  }

  @Test
  void aprovacaoPublicaEventoParaAlertarSolicitacoesAbertas() {
    enviarObrigatoriosEPedirAnalise();
    assertThat(homologacao.getStatus()).isEqualTo(StatusHomologacao.EM_ANALISE);

    servico.aprovar(3L, ADMIN);

    assertThat(homologacao.getStatus()).isEqualTo(StatusHomologacao.APROVADA);
    assertThat(HomologacaoService.isApta(homologacao)).isTrue();
    verify(eventos).publishEvent(new ColetoraHomologadaEvent(COLETORA));
  }

  @Test
  void reprovacaoExigeMotivoEPermiteReenvio() {
    enviarObrigatoriosEPedirAnalise();

    assertThatThrownBy(() -> servico.reprovar(3L, ADMIN, " "))
        .isInstanceOf(IllegalArgumentException.class);
    servico.reprovar(3L, ADMIN, "Licença ilegível");

    assertThat(homologacao.getStatus()).isEqualTo(StatusHomologacao.REPROVADA);
    verify(eventos).publishEvent(new ColetoraReprovadaEvent(COLETORA, "Licença ilegível"));
    servico.enviarDocumento(COLETORA, TipoDocumentoHomologacao.LICENCA_AMBIENTAL, null, pdf);
    servico.enviarParaAnalise(COLETORA);
    assertThat(homologacao.getStatus()).isEqualTo(StatusHomologacao.EM_ANALISE);
  }

  @Test
  void naoAceitaDocumentoDuranteAnaliseNemValidadeVencida() {
    assertThatThrownBy(
            () ->
                servico.enviarDocumento(
                    COLETORA,
                    TipoDocumentoHomologacao.LICENCA_AMBIENTAL,
                    LocalDate.now().minusDays(2),
                    pdf))
        .hasMessageContaining("validade");
    enviarObrigatoriosEPedirAnalise();

    assertThatThrownBy(
            () -> servico.enviarDocumento(COLETORA, TipoDocumentoHomologacao.MTR, null, pdf))
        .hasMessageContaining("em análise");
  }

  @Test
  void soAprovaHomologacaoEmAnalise() {
    assertThatThrownBy(() -> servico.aprovar(3L, ADMIN))
        .isInstanceOf(IllegalArgumentException.class);
    verify(eventos, never()).publishEvent(any(ColetoraHomologadaEvent.class));
  }

  private void enviarObrigatoriosEPedirAnalise() {
    servico.enviarDocumento(
        COLETORA, TipoDocumentoHomologacao.LICENCA_AMBIENTAL, LocalDate.now().plusYears(1), pdf);
    servico.enviarDocumento(COLETORA, TipoDocumentoHomologacao.CADASTRO_IBAMA, null, pdf);
    servico.enviarParaAnalise(COLETORA);
  }
}
