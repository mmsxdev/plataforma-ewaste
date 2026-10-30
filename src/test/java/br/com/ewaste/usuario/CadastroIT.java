package br.com.ewaste.usuario;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.shared.events.EmpresaCadastradaEvent;
import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.servico.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
@Transactional
class CadastroIT {
  @Autowired CadastroService cadastro;
  @Autowired EmpresaConsulta empresas;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  @Autowired MockMvc mvc;
  @Autowired ApplicationEvents eventos;
  @Autowired jakarta.persistence.EntityManager entityManager;

  private CadastroDados dados(String email) {
    return new CadastroDados(
        "Empresa Integração",
        "04.252.011/0001-10",
        "11999999999",
        "Rua A",
        "São Paulo",
        "SP",
        -23.5,
        -46.6,
        50,
        "Responsável",
        email,
        "senha123");
  }

  @Test
  void cadastroPersisteEmpresaUsuarioBCryptEEvento() throws Exception {
    Long id = cadastro.cadastrar(TipoEmpresa.COLETORA, dados("COLETORA@integracao.com"));
    assertThat(empresas.buscar(id)).get().extracting("raioAtendimentoKm").isEqualTo(50);
    assertThat(empresas.listarColetoras()).extracting("id").contains(id);
    String hash =
        jdbc.queryForObject("select senha_hash from usuario where empresa_id=?", String.class, id);
    assertThat(encoder.matches("senha123", hash)).isTrue();
    assertThat(eventos.stream(EmpresaCadastradaEvent.class))
        .contains(new EmpresaCadastradaEvent(id, TipoEmpresa.COLETORA));
    mvc.perform(formLogin().user("COLETORA@integracao.com").password("senha123"))
        .andExpect(authenticated().withRoles("COLETORA"));
    mvc.perform(formLogin().user("coletora@integracao.com").password("errada"))
        .andExpect(unauthenticated());
    jdbc.update("update usuario set ativo=false where empresa_id=?", id);
    entityManager.clear();
    mvc.perform(formLogin().user("coletora@integracao.com").password("senha123"))
        .andExpect(unauthenticated());
  }

  @Test
  void rejeitaDuplicidadeSemCriarEmpresaAdicional() {
    cadastro.cadastrar(TipoEmpresa.GERADORA, dados("teste@integracao.com"));
    assertThatThrownBy(
            () -> cadastro.cadastrar(TipoEmpresa.GERADORA, dados("outro@integracao.com")))
        .isInstanceOf(CadastroDuplicadoException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from empresa where cnpj='04252011000110'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void bancoImpedeEmailDuplicadoIgnorandoCaixa() {
    cadastro.cadastrar(TipoEmpresa.GERADORA, dados("teste@integracao.com"));
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into usuario(nome,email,senha_hash,perfil) values ('Outro', 'TESTE@integracao.com','hash','ADMIN')"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void formularioInvalidoNaoGravaNoBanco() throws Exception {
    mvc.perform(post("/cadastro/geradora").with(csrf()).param("email", "invalido"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasErrors("cadastro"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from usuario where email='invalido'", Integer.class))
        .isZero();
  }

  @Test
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void cadastroDaColetoraCriaHomologacaoPendenteAposCommit() {
    Long id = cadastro.cadastrar(TipoEmpresa.COLETORA, dados("fluxo@integracao.com"));
    try {
      assertThat(
              jdbc.queryForObject(
                  "select status from homologacao where empresa_id=?", String.class, id))
          .isEqualTo("PENDENTE");
    } finally {
      jdbc.update("delete from homologacao where empresa_id=?", id);
      jdbc.update("delete from usuario where empresa_id=?", id);
      jdbc.update("delete from empresa where id=?", id);
    }
  }
}
