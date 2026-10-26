package br.com.ewaste.usuario.api;

/** Perfil de acesso. O papel do Spring Security é "ROLE_" + nome (ex.: ROLE_GERADORA). */
public enum PerfilUsuario {
  GERADORA,
  COLETORA,
  ADMIN
}
