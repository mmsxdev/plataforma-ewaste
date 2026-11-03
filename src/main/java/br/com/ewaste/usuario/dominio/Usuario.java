package br.com.ewaste.usuario.dominio;

import br.com.ewaste.usuario.api.PerfilUsuario;
import br.com.ewaste.usuario.api.UsuarioLogado;
import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long empresaId;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, unique = true, length = 150)
  private String email;

  @Column(nullable = false, length = 100)
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PerfilUsuario perfil;

  @Column(nullable = false)
  private boolean ativo = true;

  protected Usuario() {}

  public Usuario(
      Long empresaId, String nome, String email, String senhaHash, PerfilUsuario perfil) {
    this.empresaId = empresaId;
    this.nome = nome;
    this.email = email;
    this.senhaHash = senhaHash;
    this.perfil = perfil;
  }

  public String getEmail() {
    return email;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public PerfilUsuario getPerfil() {
    return perfil;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public UsuarioLogado resumo() {
    return new UsuarioLogado(id, empresaId, nome, perfil);
  }
}
