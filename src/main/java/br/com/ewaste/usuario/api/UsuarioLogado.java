package br.com.ewaste.usuario.api;

/**
 * Quem está usando o sistema agora.
 *
 * @param empresaId empresa do usuário; {@code null} para ADMIN
 */
public record UsuarioLogado(Long usuarioId, Long empresaId, String nome, PerfilUsuario perfil) {}
