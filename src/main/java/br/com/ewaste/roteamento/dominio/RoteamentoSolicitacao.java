package br.com.ewaste.roteamento.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Marca de que a solicitação já foi roteada, com ou sem coletoras no raio. Tabela
 * roteamento_solicitacao. É gravada pelo repositório (INSERT ... ON CONFLICT), não por esta classe.
 *
 * <p>Solicitação é de outro módulo, então a referência é só pelo id.
 */
@Entity
@Table(name = "roteamento_solicitacao")
public class RoteamentoSolicitacao {

  @Id
  @Column(name = "solicitacao_id")
  private Long solicitacaoId;

  @Column(name = "processado_em", nullable = false)
  private Instant processadoEm;

  /** Exigido pelo JPA. */
  protected RoteamentoSolicitacao() {}

  public Long getSolicitacaoId() {
    return solicitacaoId;
  }

  public Instant getProcessadoEm() {
    return processadoEm;
  }
}
