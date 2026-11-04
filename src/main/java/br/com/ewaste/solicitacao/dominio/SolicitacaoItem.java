package br.com.ewaste.solicitacao.dominio;

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
import java.math.BigDecimal;

/** Um tipo de resíduo dentro da solicitação. Tabela solicitacao_item. */
@Entity
@Table(name = "solicitacao_item")
public class SolicitacaoItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Solicitação dona do item (mesmo módulo, então pode ser referência JPA). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitacao_id")
  private Solicitacao solicitacao;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_residuo", nullable = false, length = 30)
  private TipoResiduo tipoResiduo;

  @Column(nullable = false)
  private int quantidade;

  @Column(name = "peso_kg", precision = 10, scale = 2)
  private BigDecimal pesoKg;

  /** Exigido pelo JPA; use Solicitacao.adicionarItem(...) para criar. */
  protected SolicitacaoItem() {}

  SolicitacaoItem(
      Solicitacao solicitacao, TipoResiduo tipoResiduo, int quantidade, BigDecimal pesoKg) {
    if (quantidade <= 0) {
      throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
    }
    this.solicitacao = solicitacao;
    this.tipoResiduo = tipoResiduo;
    this.quantidade = quantidade;
    this.pesoKg = pesoKg;
  }

  public Long getId() {
    return id;
  }

  public TipoResiduo getTipoResiduo() {
    return tipoResiduo;
  }

  public int getQuantidade() {
    return quantidade;
  }

  public BigDecimal getPesoKg() {
    return pesoKg;
  }
}
