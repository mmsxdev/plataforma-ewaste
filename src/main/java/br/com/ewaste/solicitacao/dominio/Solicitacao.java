package br.com.ewaste.solicitacao.dominio;

import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Pedido de coleta de uma geradora (RF02). Tabela solicitacao.
 *
 * <p>O status não tem setter: cada mudança passa por um método que valida a transição em {@link
 * TransicoesSolicitacao}. A exceção é o aceite, feito por um UPDATE condicional no repositório para
 * que só uma coletora vença (RF04).
 */
@Entity
@Table(name = "solicitacao")
public class Solicitacao {

  /** Fuso usado para receber e mostrar datas (o banco guarda em UTC, coluna TIMESTAMPTZ). */
  public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Empresa geradora. Outro módulo (usuario), então a referência é só pelo id. */
  @Column(name = "geradora_id", nullable = false)
  private Long geradoraId;

  @Column(length = 1000)
  private String descricao;

  @Column(name = "peso_estimado_kg", nullable = false, precision = 10, scale = 2)
  private BigDecimal pesoEstimadoKg;

  @Column(name = "contem_midias", nullable = false)
  private boolean contemMidias;

  @Column(name = "qtd_midias", nullable = false)
  private int qtdMidias;

  @Column(length = 200)
  private String endereco;

  @Column(nullable = false, length = 100)
  private String cidade;

  /** Coluna CHAR(2) no banco. */
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 2)
  private String uf;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusSolicitacao status;

  /** Coletora que venceu o aceite; nula enquanto a solicitação está ABERTA. */
  @Column(name = "coletora_id")
  private Long coletoraId;

  @Column(name = "aceita_em")
  private Instant aceitaEm;

  @Column(name = "agendada_para")
  private Instant agendadaPara;

  @Column(name = "criada_em", nullable = false)
  private Instant criadaEm;

  /** Optimistic locking: salvar uma versão desatualizada falha em vez de sobrescrever. */
  @Version
  @Column(nullable = false)
  private Long version;

  /** Itens da solicitação. Salvar a solicitação salva junto os itens (cascade). */
  @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private List<SolicitacaoItem> itens = new ArrayList<>();

  /** Exigido pelo JPA; use {@link #abrir} para criar. */
  protected Solicitacao() {}

  /** Toda solicitação nasce ABERTA, esperando o aceite de uma coletora. */
  public static Solicitacao abrir(
      Long geradoraId,
      String descricao,
      BigDecimal pesoEstimadoKg,
      boolean contemMidias,
      int qtdMidias,
      String endereco,
      String cidade,
      String uf,
      double latitude,
      double longitude) {
    if (pesoEstimadoKg == null || pesoEstimadoKg.signum() <= 0) {
      throw new IllegalArgumentException("O peso estimado deve ser maior que zero.");
    }
    if (contemMidias && qtdMidias <= 0) {
      throw new IllegalArgumentException("Informe a quantidade de mídias.");
    }
    Solicitacao s = new Solicitacao();
    s.geradoraId = geradoraId;
    s.descricao = descricao;
    s.pesoEstimadoKg = pesoEstimadoKg;
    s.contemMidias = contemMidias;
    s.qtdMidias = contemMidias ? qtdMidias : 0;
    s.endereco = endereco;
    s.cidade = cidade;
    s.uf = uf;
    s.latitude = latitude;
    s.longitude = longitude;
    s.status = StatusSolicitacao.ABERTA;
    s.criadaEm = Instant.now();
    return s;
  }

  public SolicitacaoItem adicionarItem(TipoResiduo tipo, int quantidade, BigDecimal pesoKg) {
    var item = new SolicitacaoItem(this, tipo, quantidade, pesoKg);
    itens.add(item);
    return item;
  }

  // ---------------------------------------------------------------------------------------------
  // Transições de status
  // ---------------------------------------------------------------------------------------------

  /**
   * ACEITA -> AGENDADA, definindo a data da retirada. Já AGENDADA: só troca a data (reagendamento),
   * sem mudar o status. Qualquer outro status lança {@link TransicaoInvalidaException}.
   */
  public void agendar(Instant data) {
    if (data == null) {
      throw new IllegalArgumentException("Informe a data da retirada.");
    }
    if (status != StatusSolicitacao.AGENDADA) {
      mudarPara(StatusSolicitacao.AGENDADA);
    }
    agendadaPara = data;
  }

  /** AGENDADA -> COLETADA. Pedido pelo rastreio (Dev 4). */
  public void marcarColetada() {
    mudarPara(StatusSolicitacao.COLETADA);
  }

  /** COLETADA -> CONCLUIDA. Pedido pelo rastreio (Dev 4). */
  public void marcarConcluida() {
    mudarPara(StatusSolicitacao.CONCLUIDA);
  }

  private void mudarPara(StatusSolicitacao destino) {
    TransicoesSolicitacao.exigir(status, destino);
    status = destino;
  }

  public Long getId() {
    return id;
  }

  public Long getGeradoraId() {
    return geradoraId;
  }

  public String getDescricao() {
    return descricao;
  }

  public BigDecimal getPesoEstimadoKg() {
    return pesoEstimadoKg;
  }

  public boolean isContemMidias() {
    return contemMidias;
  }

  public int getQtdMidias() {
    return qtdMidias;
  }

  public String getEndereco() {
    return endereco;
  }

  public String getCidade() {
    return cidade;
  }

  public String getUf() {
    return uf;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public StatusSolicitacao getStatus() {
    return status;
  }

  public Long getColetoraId() {
    return coletoraId;
  }

  public Instant getAceitaEm() {
    return aceitaEm;
  }

  public Instant getAgendadaPara() {
    return agendadaPara;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }

  public Long getVersion() {
    return version;
  }

  /** Lista só de leitura: itens novos entram por {@link #adicionarItem}. */
  public List<SolicitacaoItem> getItens() {
    return Collections.unmodifiableList(itens);
  }
}
