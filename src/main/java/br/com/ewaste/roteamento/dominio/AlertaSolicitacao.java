package br.com.ewaste.roteamento.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Registro de que uma coletora foi alertada sobre uma solicitação (RF03). Tabela
 * alerta_solicitacao, chave (solicitacao_id, coletora_id): no máximo um alerta por par.
 *
 * <p>Solicitação e coletora são de outros módulos, então a referência é só pelo id.
 */
@Entity
@Table(name = "alerta_solicitacao")
@IdClass(AlertaSolicitacao.Chave.class)
public class AlertaSolicitacao {

  @Id
  @Column(name = "solicitacao_id")
  private Long solicitacaoId;

  @Id
  @Column(name = "coletora_id")
  private Long coletoraId;

  @Column(name = "distancia_km", nullable = false, precision = 8, scale = 2)
  private BigDecimal distanciaKm;

  @Column(name = "enviado_em", nullable = false)
  private Instant enviadoEm;

  /** Exigido pelo JPA. */
  protected AlertaSolicitacao() {}

  public AlertaSolicitacao(
      Long solicitacaoId, Long coletoraId, double distanciaKm, Instant enviadoEm) {
    this.solicitacaoId = solicitacaoId;
    this.coletoraId = coletoraId;
    this.distanciaKm = BigDecimal.valueOf(distanciaKm).setScale(2, RoundingMode.HALF_UP);
    this.enviadoEm = enviadoEm;
  }

  public Long getSolicitacaoId() {
    return solicitacaoId;
  }

  public Long getColetoraId() {
    return coletoraId;
  }

  public BigDecimal getDistanciaKm() {
    return distanciaKm;
  }

  public Instant getEnviadoEm() {
    return enviadoEm;
  }

  /** Chave composta exigida pelo JPA para {@code @IdClass}: mesmos nomes dos campos {@code @Id}. */
  public static class Chave implements Serializable {

    private Long solicitacaoId;
    private Long coletoraId;

    public Chave() {}

    public Chave(Long solicitacaoId, Long coletoraId) {
      this.solicitacaoId = solicitacaoId;
      this.coletoraId = coletoraId;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Chave c
          && Objects.equals(solicitacaoId, c.solicitacaoId)
          && Objects.equals(coletoraId, c.coletoraId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(solicitacaoId, coletoraId);
    }
  }
}
