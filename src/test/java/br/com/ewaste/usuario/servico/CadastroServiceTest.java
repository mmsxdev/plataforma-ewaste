package br.com.ewaste.usuario.servico;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.ewaste.shared.events.EmpresaCadastradaEvent;
import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.dominio.*;
import br.com.ewaste.usuario.repositorio.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CadastroServiceTest {
  @Mock EmpresaRepository empresas;
  @Mock UsuarioRepository usuarios;
  @Mock ApplicationEventPublisher eventos;
  BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  CadastroService service;

  @BeforeEach
  void iniciar() {
    service = new CadastroService(empresas, usuarios, encoder, eventos);
  }

  static CadastroDados dados(Integer raio) {
    return new CadastroDados(
        "Empresa",
        "11.222.333/0001-81",
        "11999999999",
        "Rua A",
        "São Paulo",
        "SP",
        -23.5,
        -46.6,
        raio,
        "Miguel",
        "MIGUEL@teste.com",
        "senha123");
  }

  @Test
  void cadastraComSenhaProtegidaEPublicaEvento() {
    Empresa empresa = mock(Empresa.class);
    when(empresa.getId()).thenReturn(42L);
    when(empresas.saveAndFlush(any())).thenReturn(empresa);
    assertThat(service.cadastrar(TipoEmpresa.COLETORA, dados(50))).isEqualTo(42L);
    ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarios).saveAndFlush(usuario.capture());
    assertThat(usuario.getValue().getEmail()).isEqualTo("miguel@teste.com");
    assertThat(usuario.getValue().getPerfil()).isEqualTo(PerfilUsuario.COLETORA);
    assertThat(encoder.matches("senha123", usuario.getValue().getSenhaHash())).isTrue();
    verify(eventos).publishEvent(new EmpresaCadastradaEvent(42L, TipoEmpresa.COLETORA));
  }

  @Test
  void recusaCnpjDuplicadoAntesDeGravar() {
    when(empresas.existsByCnpj("11222333000181")).thenReturn(true);
    assertThatThrownBy(() -> service.cadastrar(TipoEmpresa.GERADORA, dados(null)))
        .isInstanceOf(CadastroDuplicadoException.class)
        .hasMessageContaining("CNPJ");
    verify(empresas, never()).saveAndFlush(any());
    verifyNoInteractions(usuarios, eventos);
  }

  @Test
  void recusaEmailDuplicadoAntesDeGravar() {
    when(usuarios.existsByEmailIgnoreCase("miguel@teste.com")).thenReturn(true);
    assertThatThrownBy(() -> service.cadastrar(TipoEmpresa.GERADORA, dados(null)))
        .isInstanceOf(CadastroDuplicadoException.class)
        .hasMessageContaining("e-mail");
    verify(empresas, never()).saveAndFlush(any());
    verifyNoInteractions(eventos);
  }

  @Test
  void exigeRaioDaColetora() {
    assertThatThrownBy(() -> service.cadastrar(TipoEmpresa.COLETORA, dados(null)))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(empresas, usuarios, eventos);
  }
}
