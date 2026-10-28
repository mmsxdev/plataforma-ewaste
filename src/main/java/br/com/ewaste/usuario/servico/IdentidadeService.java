package br.com.ewaste.usuario.servico;

import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.repositorio.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IdentidadeService implements UserDetailsService, UsuarioAtual {
  private final UsuarioRepository usuarios;

  public IdentidadeService(UsuarioRepository usuarios) {
    this.usuarios = usuarios;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    var usuario =
        usuarios
            .findByEmailIgnoreCase(email.strip())
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    return User.withUsername(usuario.getEmail())
        .password(usuario.getSenhaHash())
        .roles(usuario.getPerfil().name())
        .disabled(!usuario.isAtivo())
        .build();
  }

  @Override
  public UsuarioLogado atual() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
      throw new IllegalStateException("Nenhum usuário autenticado.");
    return usuarios
        .findByEmailIgnoreCase(auth.getName())
        .filter(u -> u.isAtivo())
        .orElseThrow(() -> new IllegalStateException("Usuário indisponível."))
        .resumo();
  }
}
