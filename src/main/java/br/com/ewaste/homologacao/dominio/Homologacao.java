package br.com.ewaste.homologacao.dominio;

import br.com.ewaste.homologacao.api.StatusHomologacao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Processo de homologação de uma coletora (uma por empresa). Tabela homologacao.
 *
 * <p>Concentra a regra de aptidão (RNF01 / RN02): só coletoras aptas recebem alertas de coleta.
 */
@Entity
@Table(name = "homologacao")
public class Homologacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Empresa coletora. Outro módulo (usuario), então a referência é só pelo id. */
  @Column(name = "empresa_id", nullable = false, unique = true)
  private Long empresaId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusHomologacao status;

  /** Motivo da reprovação ou suspensão, mostrado à coletora. */
  @Column(length = 500)
  private String motivo;

  /** Usuário admin que analisou (id da tabela usuario, do Dev 1). */
  @Column(name = "analisado_por")
  private Long analisadoPor;

  @Column(name = "analisado_em")
  private Instant analisadoEm;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  /** Documentos enviados. Salvar a homologação salva junto os documentos novos (cascade). */
  @OneToMany(mappedBy = "homologacao", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DocumentoHomologacao> documentos = new ArrayList<>();

  /** Exigido pelo JPA; use {@link #pendente(Long)} para criar. */
  protected Homologacao() {}

  /** Toda homologação nasce PENDENTE, esperando a coletora enviar os documentos. */
  public static Homologacao pendente(Long empresaId) {
    Homologacao h = new Homologacao();
    h.empresaId = empresaId;
    h.status = StatusHomologacao.PENDENTE;
    h.criadoEm = Instant.now();
    return h;
  }

  /** Registra um documento enviado (o arquivo já foi gravado pelo Armazenamento). */
  public DocumentoHomologacao adicionarDocumento(
      TipoDocumentoHomologacao tipo, String arquivoRef, String nomeOriginal, LocalDate validade) {
    var documento =
        new DocumentoHomologacao(this, tipo, arquivoRef, nomeOriginal, validade, Instant.now());
    documentos.add(documento);
    return documento;
  }

  // ---------------------------------------------------------------------------------------------
  // Transições de status (decisão do admin)
  // ---------------------------------------------------------------------------------------------

  /** Coletora enviou documentos: entra na fila de análise do admin. */
  public void enviarParaAnalise() {
    exigirStatus(StatusHomologacao.PENDENTE, StatusHomologacao.REPROVADA);
    status = StatusHomologacao.EM_ANALISE;
  }

  /** Admin aprova: a coletora passa a poder ficar apta. */
  public void aprovar(Long adminId) {
    exigirStatus(StatusHomologacao.PENDENTE, StatusHomologacao.EM_ANALISE);
    registrarAnalise(StatusHomologacao.APROVADA, adminId, null);
  }

  /** Admin reprova: o motivo é obrigatório, porque a coletora precisa saber o que corrigir. */
  public void reprovar(Long adminId, String motivo) {
    exigirStatus(StatusHomologacao.PENDENTE, StatusHomologacao.EM_ANALISE);
    registrarAnalise(StatusHomologacao.REPROVADA, adminId, exigirMotivo(motivo));
  }

  /** Suspende uma coletora aprovada (ex.: licença vencida). Também exige motivo. */
  public void suspender(Long adminId, String motivo) {
    exigirStatus(StatusHomologacao.APROVADA);
    registrarAnalise(StatusHomologacao.SUSPENSA, adminId, exigirMotivo(motivo));
  }

  private void registrarAnalise(StatusHomologacao novo, Long adminId, String motivo) {
    this.status = novo;
    this.analisadoPor = adminId;
    this.analisadoEm = Instant.now();
    this.motivo = motivo;
  }

  /** Recusa a transição se o status atual não for um dos permitidos. */
  private void exigirStatus(StatusHomologacao... permitidos) {
    if (!Arrays.asList(permitidos).contains(status)) {
      throw new IllegalStateException("Ação não permitida com a homologação " + status);
    }
  }

  private static String exigirMotivo(String motivo) {
    if (motivo == null || motivo.isBlank()) {
      throw new IllegalArgumentException("Informe o motivo.");
    }
    return motivo.trim();
  }

  // ---------------------------------------------------------------------------------------------
  // Regra de aptidão
  // ---------------------------------------------------------------------------------------------

  /**
   * Apta = homologação APROVADA e, para cada tipo obrigatório, o documento mais recente desse tipo
   * existe e não está vencido. Se a coletora reenviou uma licença renovada, vale a mais nova.
   */
  public boolean isApta(LocalDate hoje) {
    if (status != StatusHomologacao.APROVADA) {
      return false;
    }
    return Arrays.stream(TipoDocumentoHomologacao.values())
        .filter(TipoDocumentoHomologacao::isObrigatorio)
        .allMatch(tipo -> documentoMaisRecente(tipo).filter(d -> !d.isVencido(hoje)).isPresent());
  }

  /**
   * Último documento enviado de um tipo (por data de envio). Em empate de horário, vale o que foi
   * adicionado por último.
   */
  public Optional<DocumentoHomologacao> documentoMaisRecente(TipoDocumentoHomologacao tipo) {
    return documentos.stream()
        .filter(d -> d.getTipo() == tipo)
        .reduce(
            (anterior, atual) ->
                atual.getEnviadoEm().isBefore(anterior.getEnviadoEm()) ? anterior : atual);
  }

  public Long getId() {
    return id;
  }

  public Long getEmpresaId() {
    return empresaId;
  }

  public StatusHomologacao getStatus() {
    return status;
  }

  public String getMotivo() {
    return motivo;
  }

  public Long getAnalisadoPor() {
    return analisadoPor;
  }

  public Instant getAnalisadoEm() {
    return analisadoEm;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  /** Lista só de leitura: documentos novos entram por {@link #adicionarDocumento}. */
  public List<DocumentoHomologacao> getDocumentos() {
    return Collections.unmodifiableList(documentos);
  }
}
