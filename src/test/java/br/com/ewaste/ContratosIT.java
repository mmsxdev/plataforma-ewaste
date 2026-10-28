package br.com.ewaste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.TipoEmpresa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Garante que o esquema do PR 0 e as implementações provisórias funcionam juntos. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ContratosIT {

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  @Autowired EmpresaConsulta empresas;
  @Autowired ColetorasAptas aptas;

  @BeforeEach
  void limparBanco() {
    jdbc.execute(
        "truncate lancamento, notificacao, documento_emitido, evento_rastreio, midia_destruida,"
            + " alerta_solicitacao, solicitacao_item, solicitacao, documento_homologacao,"
            + " homologacao, assinatura, usuario, empresa restart identity cascade");
  }

  @Test
  void loginProvisorioUsaTabelaUsuario() throws Exception {
    long empresa = inserirEmpresa("GERADORA", "11222333000181");
    jdbc.update(
        "insert into usuario (empresa_id, nome, email, senha_hash, perfil) values (?,?,?,?,?)",
        empresa,
        "Teste",
        "teste@teste.com",
        encoder.encode("senha123"),
        "GERADORA");

    mvc.perform(formLogin().user("teste@teste.com").password("senha123"))
        .andExpect(authenticated().withRoles("GERADORA"));
    mvc.perform(formLogin().user("teste@teste.com").password("errada"))
        .andExpect(unauthenticated());
  }

  @Test
  void consultaDeEmpresasEColetorasAptas() {
    inserirEmpresa("GERADORA", "11222333000181");
    long coletora = inserirEmpresa("COLETORA", "22333444000181");

    assertThat(empresas.buscar(coletora)).get().extracting("tipo").isEqualTo(TipoEmpresa.COLETORA);
    assertThat(empresas.listarColetoras()).hasSize(1);
    // ColetorasAptas real (Dev 2): coletora sem homologação aprovada não é apta
    assertThat(aptas.listarAptas()).isEmpty();
  }

  @Test
  void bancoImpedeTaxaDeMatchDuplicada() {
    long geradora = inserirEmpresa("GERADORA", "11222333000181");
    long coletora = inserirEmpresa("COLETORA", "22333444000181");
    Long solicitacao =
        jdbc.queryForObject(
            "insert into solicitacao (geradora_id, peso_estimado_kg, cidade, uf, latitude, longitude)"
                + " values (?, 250, 'São Paulo', 'SP', -23.5, -46.6) returning id",
            Long.class,
            geradora);
    String sql =
        "insert into lancamento (empresa_id, tipo, solicitacao_id, valor, competencia)"
            + " values (?, 'TAXA_MATCH', ?, 10, date_trunc('month', now()))";
    jdbc.update(sql, coletora, solicitacao);

    assertThatThrownBy(() -> jdbc.update(sql, coletora, solicitacao))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private long inserirEmpresa(String tipo, String cnpj) {
    return jdbc.queryForObject(
        "insert into empresa (tipo, razao_social, cnpj, cidade, uf, latitude, longitude,"
            + " raio_atendimento_km) values (?, 'Empresa ' || ?, ?, 'São Paulo', 'SP', -23.55,"
            + " -46.63, 50) returning id",
        Long.class,
        tipo,
        cnpj,
        cnpj);
  }
}
