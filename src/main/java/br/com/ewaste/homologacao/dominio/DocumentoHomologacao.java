package br.com.ewaste.homologacao.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Um arquivo (licença, certificação) enviado pela coletora. Tabela documento_homologacao. */
@Entity
@Table(name = "documento_homologacao")
public class DocumentoHomologacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Homologação dona do documento (mesmo módulo, então pode ser referência JPA). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "homologacao_id")
  private Homologacao homologacao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoDocumentoHomologacao tipo;

  /** Caminho devolvido pelo Armazenamento (ArquivoRef.caminho()). */
  @Column(name = "arquivo_ref", nullable = false, length = 300)
  private String arquivoRef;

  @Column(name = "nome_original", length = 200)
  private String nomeOriginal;

  /** Data de validade informada pela coletora; nula = documento sem vencimento. */
  private LocalDate validade;

  @Column(name = "enviado_em", nullable = false)
  private Instant enviadoEm;

  /** Exigido pelo JPA; use Homologacao.adicionarDocumento(...) para criar. */
  protected DocumentoHomologacao() {}

  DocumentoHomologacao(
      Homologacao homologacao,
      TipoDocumentoHomologacao tipo,
      String arquivoRef,
      String nomeOriginal,
      LocalDate validade,
      Instant enviadoEm) {
    this.homologacao = homologacao;
    this.tipo = tipo;
    this.arquivoRef = arquivoRef;
    this.nomeOriginal = nomeOriginal;
    this.validade = validade;
    this.enviadoEm = enviadoEm;
  }

  /** Vencido = tem validade e ela é anterior a hoje (vence no fim do dia da validade). */
  public boolean isVencido(LocalDate hoje) {
    return validade != null && validade.isBefore(hoje);
  }

  public Long getId() {
    return id;
  }

  public Homologacao getHomologacao() {
    return homologacao;
  }

  public TipoDocumentoHomologacao getTipo() {
    return tipo;
  }

  public String getArquivoRef() {
    return arquivoRef;
  }

  public String getNomeOriginal() {
    return nomeOriginal;
  }

  public LocalDate getValidade() {
    return validade;
  }

  public Instant getEnviadoEm() {
    return enviadoEm;
  }
}
