package br.com.ewaste.homologacao.dominio;

import static br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao.CADASTRO_IBAMA;
import static br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao.ISO_27001;
import static br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao.LICENCA_AMBIENTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.homologacao.api.StatusHomologacao;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Regras da homologação sem Spring e sem banco: transições de status e aptidão. */
class HomologacaoTest {

  static final LocalDate HOJE = LocalDate.of(2026, 10, 28);
  static final Long ADMIN = 1L;

  // ---- Aptidão: tabela status × validade (critério de aceitação do plano) ----

  @Test
  void aprovadaComDocumentosObrigatoriosValidosEApta() {
    Homologacao h = comDocumentosObrigatorios(HOJE.plusYears(1));
    h.aprovar(ADMIN);

    assertThat(h.isApta(HOJE)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(
      value = StatusHomologacao.class,
      names = {"PENDENTE", "EM_ANALISE", "REPROVADA", "SUSPENSA"})
  void qualquerStatusDiferenteDeAprovadaNaoEApto(StatusHomologacao status) {
    Homologacao h = comDocumentosObrigatorios(HOJE.plusYears(1));
    levarAte(h, status);

    assertThat(h.getStatus()).isEqualTo(status);
    assertThat(h.isApta(HOJE)).isFalse();
  }

  @Test
  void licencaVencidaTiraAAptidao() {
    Homologacao h = comDocumentosObrigatorios(HOJE.minusDays(1));
    h.aprovar(ADMIN);

    assertThat(h.isApta(HOJE)).isFalse();
  }

  @Test
  void licencaQueVenceHojeAindaValeHoje() {
    Homologacao h = comDocumentosObrigatorios(HOJE);
    h.aprovar(ADMIN);

    assertThat(h.isApta(HOJE)).isTrue();
    assertThat(h.isApta(HOJE.plusDays(1))).isFalse();
  }

  @Test
  void documentoSemValidadeNaoVence() {
    Homologacao h = comDocumentosObrigatorios(null);
    h.aprovar(ADMIN);

    assertThat(h.isApta(HOJE.plusYears(50))).isTrue();
  }

  @Test
  void faltandoDocumentoObrigatorioNaoEApta() {
    Homologacao h = Homologacao.pendente(10L);
    h.adicionarDocumento(LICENCA_AMBIENTAL, "h/1.pdf", "licenca.pdf", HOJE.plusYears(1));
    h.adicionarDocumento(ISO_27001, "h/2.pdf", "iso.pdf", HOJE.plusYears(1)); // não é obrigatório
    h.aprovar(ADMIN);

    assertThat(h.isApta(HOJE)).isFalse(); // falta o CADASTRO_IBAMA
  }

  @Test
  void documentoOpcionalVencidoNaoAtrapalha() {
    Homologacao h = comDocumentosObrigatorios(HOJE.plusYears(1));
    h.adicionarDocumento(ISO_27001, "h/3.pdf", "iso.pdf", HOJE.minusYears(1));
    h.aprovar(ADMIN);

    assertThat(h.isApta(HOJE)).isTrue();
  }

  @Test
  void licencaRenovadaSubstituiAVencida() {
    Homologacao h = comDocumentosObrigatorios(HOJE.minusDays(10)); // ambas vencidas
    h.aprovar(ADMIN);
    assertThat(h.isApta(HOJE)).isFalse();

    // Coletora reenvia as duas renovadas: o documento mais recente de cada tipo é o que vale
    h.adicionarDocumento(LICENCA_AMBIENTAL, "h/9.pdf", "nova.pdf", HOJE.plusYears(2));
    h.adicionarDocumento(CADASTRO_IBAMA, "h/10.pdf", "novo.pdf", HOJE.plusYears(2));

    assertThat(h.isApta(HOJE)).isTrue();
  }

  // ---- Transições de status ----

  @Test
  void nascePendenteENaoApta() {
    Homologacao h = Homologacao.pendente(10L);

    assertThat(h.getStatus()).isEqualTo(StatusHomologacao.PENDENTE);
    assertThat(h.getEmpresaId()).isEqualTo(10L);
    assertThat(h.isApta(HOJE)).isFalse();
  }

  @Test
  void aprovarRegistraQuemEQuando() {
    Homologacao h = Homologacao.pendente(10L);
    h.enviarParaAnalise();
    h.aprovar(ADMIN);

    assertThat(h.getStatus()).isEqualTo(StatusHomologacao.APROVADA);
    assertThat(h.getAnalisadoPor()).isEqualTo(ADMIN);
    assertThat(h.getAnalisadoEm()).isNotNull();
    assertThat(h.getMotivo()).isNull();
  }

  @Test
  void reprovarExigeMotivo() {
    Homologacao h = Homologacao.pendente(10L);

    assertThatThrownBy(() -> h.reprovar(ADMIN, "  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motivo");
    assertThat(h.getStatus()).isEqualTo(StatusHomologacao.PENDENTE); // nada mudou

    h.reprovar(ADMIN, " Licença ilegível ");
    assertThat(h.getStatus()).isEqualTo(StatusHomologacao.REPROVADA);
    assertThat(h.getMotivo()).isEqualTo("Licença ilegível");
  }

  @Test
  void reprovadaPodeReenviarParaAnalise() {
    Homologacao h = Homologacao.pendente(10L);
    h.reprovar(ADMIN, "Faltou o IBAMA");
    h.enviarParaAnalise();

    assertThat(h.getStatus()).isEqualTo(StatusHomologacao.EM_ANALISE);
  }

  @Test
  void transicoesInvalidasSaoRecusadas() {
    Homologacao h = Homologacao.pendente(10L);

    // Só suspende quem está aprovada
    assertThatThrownBy(() -> h.suspender(ADMIN, "x")).isInstanceOf(IllegalStateException.class);

    h.aprovar(ADMIN);
    // Aprovada não é aprovada de novo nem reprovada direto
    assertThatThrownBy(() -> h.aprovar(ADMIN)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> h.reprovar(ADMIN, "x")).isInstanceOf(IllegalStateException.class);
  }

  // ---- Auxiliares ----

  /** Homologação PENDENTE com licença ambiental e cadastro IBAMA na validade informada. */
  private static Homologacao comDocumentosObrigatorios(LocalDate validade) {
    Homologacao h = Homologacao.pendente(10L);
    h.adicionarDocumento(LICENCA_AMBIENTAL, "h/1.pdf", "licenca.pdf", validade);
    h.adicionarDocumento(CADASTRO_IBAMA, "h/2.pdf", "ibama.pdf", validade);
    return h;
  }

  /** Leva a homologação até o status pedido usando só transições válidas. */
  private static void levarAte(Homologacao h, StatusHomologacao status) {
    switch (status) {
      case PENDENTE -> {}
      case EM_ANALISE -> h.enviarParaAnalise();
      case REPROVADA -> h.reprovar(ADMIN, "Documento ilegível");
      case APROVADA -> h.aprovar(ADMIN);
      case SUSPENSA -> {
        h.aprovar(ADMIN);
        h.suspender(ADMIN, "Licença vencida");
      }
    }
  }
}
