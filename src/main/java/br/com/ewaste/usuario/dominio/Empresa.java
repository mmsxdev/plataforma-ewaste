package br.com.ewaste.usuario.dominio;

import br.com.ewaste.usuario.api.EmpresaResumo;
import br.com.ewaste.usuario.api.TipoEmpresa;
import jakarta.persistence.*;

@Entity
@Table(name = "empresa")
public class Empresa {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoEmpresa tipo;

  @Column(nullable = false, length = 150)
  private String razaoSocial;

  @Column(nullable = false, unique = true, length = 14)
  private String cnpj;

  @Column(length = 20)
  private String telefone;

  @Column(length = 200)
  private String endereco;

  @Column(nullable = false, length = 100)
  private String cidade;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
  @Column(nullable = false, length = 2)
  private String uf;

  private Double latitude;
  private Double longitude;
  private Integer raioAtendimentoKm;

  protected Empresa() {}

  public Empresa(
      TipoEmpresa tipo,
      String razaoSocial,
      String cnpj,
      String telefone,
      String endereco,
      String cidade,
      String uf,
      Double latitude,
      Double longitude,
      Integer raio) {
    this.tipo = tipo;
    this.razaoSocial = razaoSocial;
    this.cnpj = cnpj;
    this.telefone = telefone;
    this.endereco = endereco;
    this.cidade = cidade;
    this.uf = uf;
    this.latitude = latitude;
    this.longitude = longitude;
    this.raioAtendimentoKm = tipo == TipoEmpresa.COLETORA ? raio : null;
  }

  public Long getId() {
    return id;
  }

  public EmpresaResumo resumo() {
    return new EmpresaResumo(
        id, tipo, razaoSocial, cnpj, cidade, uf, latitude, longitude, raioAtendimentoKm);
  }
}
