package br.com.ewaste.usuario;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.dominio.*;
import br.com.ewaste.usuario.repositorio.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class IdentidadeIT {
  @Autowired EmpresaRepository empresas;
  @Autowired UsuarioRepository usuarios;
  @Autowired EmpresaConsulta consulta;
  @Autowired UsuarioAtual atual;
  @Autowired PasswordEncoder encoder;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;

  @BeforeEach
  void isolarDados() {
    // O PostgreSQL desfaz o TRUNCATE no rollback de cada teste.
    jdbc.execute("truncate table empresa, usuario restart identity cascade");
  }

  @ParameterizedTest
  @EnumSource(PerfilUsuario.class)
  void autenticaCadaPerfilComEmailSemDiferencaDeCaixa(PerfilUsuario perfil) throws Exception {
    Long empresaId =
        perfil == PerfilUsuario.ADMIN
            ? null
            : empresa(TipoEmpresa.valueOf(perfil.name()), "04252011000110").getId();
    usuario(empresaId, perfil);
    mvc.perform(formLogin().user(" CONTA@identidade.test ").password("senha123"))
        .andExpect(authenticated().withRoles(perfil.name()));
    mvc.perform(formLogin().user("conta@identidade.test").password("incorreta"))
        .andExpect(unauthenticated());
  }

  @Test
  void contaInativaNaoConsegueEntrar() throws Exception {
    usuario(null, PerfilUsuario.ADMIN);
    jdbc.update("update usuario set ativo = false where email = 'conta@identidade.test'");
    entityManager.clear();
    mvc.perform(formLogin().user("conta@identidade.test").password("senha123"))
        .andExpect(unauthenticated());
  }

  @Test
  @WithMockUser(username = "conta@identidade.test", roles = "COLETORA")
  void usuarioAtualRetornaIdentidadeEIdDaEmpresaPersistidos() {
    Empresa empresa = empresa(TipoEmpresa.COLETORA, "04252011000110");
    Usuario usuario = usuario(empresa.getId(), PerfilUsuario.COLETORA);
    entityManager.clear();
    assertThat(atual.atual()).isEqualTo(usuario.resumo());
    jdbc.update("update usuario set ativo = false where email = 'conta@identidade.test'");
    entityManager.clear();
    assertThatThrownBy(atual::atual).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void consultaEmpresaAusenteEListaSomenteColetorasEmOrdemDeId() {
    empresa(TipoEmpresa.GERADORA, "11222333000181");
    Empresa primeira = empresa(TipoEmpresa.COLETORA, "04252011000110");
    Empresa segunda = empresa(TipoEmpresa.COLETORA, "22333444000181");
    entityManager.clear();
    assertThat(consulta.buscar(-1L)).isEmpty();
    assertThat(consulta.buscar(primeira.getId())).contains(primeira.resumo());
    assertThat(consulta.listarColetoras()).containsExactly(primeira.resumo(), segunda.resumo());
    assertThat(consulta.buscar(primeira.getId()).orElseThrow().raioAtendimentoKm()).isEqualTo(50);
  }

  private Empresa empresa(TipoEmpresa tipo, String cnpj) {
    return empresas.saveAndFlush(
        new Empresa(
            tipo,
            "Empresa teste",
            cnpj,
            "11999999999",
            "Rua A",
            "São Paulo",
            "SP",
            -23.5,
            -46.6,
            50));
  }

  private Usuario usuario(Long empresaId, PerfilUsuario perfil) {
    return usuarios.saveAndFlush(
        new Usuario(
            empresaId, "Miguel", "conta@identidade.test", encoder.encode("senha123"), perfil));
  }
}
