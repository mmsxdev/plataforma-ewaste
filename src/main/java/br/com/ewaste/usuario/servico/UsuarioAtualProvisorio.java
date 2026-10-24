package br.com.ewaste.usuario.servico;

import br.com.ewaste.usuario.api.PerfilUsuario;
import br.com.ewaste.usuario.api.UsuarioAtual;
import br.com.ewaste.usuario.api.UsuarioLogado;
import org.springframework.context.annotation.Fallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Implementação PROVISÓRIA (PR 0). Ver {@link EmpresaConsultaProvisoria}. */
@Fallback
@Component
class UsuarioAtualProvisorio implements UsuarioAtual {

  private final JdbcTemplate jdbc;

  UsuarioAtualProvisorio(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public UsuarioLogado atual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      throw new IllegalStateException("Nenhum usuário autenticado");
    }
    return jdbc
        .query(
            "select id, empresa_id, nome, perfil from usuario where email = ?",
            (rs, linha) ->
                new UsuarioLogado(
                    rs.getLong("id"),
                    rs.getObject("empresa_id", Long.class),
                    rs.getString("nome"),
                    PerfilUsuario.valueOf(rs.getString("perfil"))),
            auth.getName())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Usuário não encontrado: " + auth.getName()));
  }
}
