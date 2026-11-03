package br.com.ewaste.usuario.servico;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.ewaste.usuario.api.PerfilUsuario;
import br.com.ewaste.usuario.dominio.Usuario;
import br.com.ewaste.usuario.repositorio.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class IdentidadeServiceTest {
  UsuarioRepository usuarios = mock(UsuarioRepository.class);
  IdentidadeService service = new IdentidadeService(usuarios);

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void consultaUsuarioAutenticadoSemEmpresaParaAdmin() {
    when(usuarios.findByEmailIgnoreCase("admin@teste.com"))
        .thenReturn(
            Optional.of(
                new Usuario(null, "Admin", "admin@teste.com", "hash", PerfilUsuario.ADMIN)));
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "admin@teste.com", null, java.util.List.of()));
    assertThat(service.atual().empresaId()).isNull();
    assertThat(service.atual().perfil()).isEqualTo(PerfilUsuario.ADMIN);
    assertThat(service.loadUserByUsername(" admin@teste.com ").getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  void rejeitaAusenciaDeSessao() {
    assertThatThrownBy(service::atual).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void loginInexistenteNaoRevelaDados() {
    assertThatThrownBy(() -> service.loadUserByUsername("ausente@teste.com"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}
