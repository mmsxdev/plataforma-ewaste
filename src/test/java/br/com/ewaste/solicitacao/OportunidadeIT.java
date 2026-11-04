package br.com.ewaste.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.servico.Oportunidade;
import br.com.ewaste.solicitacao.servico.OportunidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Oportunidades da coletora contra o PostgreSQL real: só o que foi alertado e está ABERTO. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OportunidadeIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired OportunidadeService oportunidades;

  Long geradora;
  Long coletoraA;
  Long coletoraB;

  @BeforeEach
  void prepararBanco() {
    jdbc.execute(
        "truncate alerta_solicitacao, solicitacao_item, solicitacao, usuario, empresa"
            + " restart identity cascade");
    geradora = inserirEmpresa("GERADORA", "11222333000181");
    coletoraA = inserirEmpresa("COLETORA", "22333444000181");
    coletoraB = inserirEmpresa("COLETORA", "33444555000181");
  }

  @Test
  void listaSoAsAbertasAlertadasParaAColetoraDaMaisPertoParaAMaisLonge() {
    Long longe = inserirSolicitacao("ABERTA");
    Long perto = inserirSolicitacao("ABERTA");
    Long jaAceita = inserirSolicitacao("ACEITA");
    Long soDaOutra = inserirSolicitacao("ABERTA");
    alertar(longe, coletoraA, 30.0);
    alertar(perto, coletoraA, 4.5);
    alertar(jaAceita, coletoraA, 1.0);
    alertar(soDaOutra, coletoraB, 2.0);

    assertThat(oportunidades.listarAbertas(coletoraA))
        .extracting(o -> o.solicitacao().getId())
        .containsExactly(perto, longe); // a ACEITA sumiu e a da outra coletora não aparece
    assertThat(oportunidades.listarAbertas(coletoraB))
        .extracting(o -> o.solicitacao().getId())
        .containsExactly(soDaOutra);
  }

  @Test
  void coletoraSemAlertaNaoAbreODetalhe() {
    Long id = inserirSolicitacao("ABERTA");
    alertar(id, coletoraA, 5.0);

    Oportunidade o = oportunidades.buscar(id, coletoraA);
    assertThat(o.distanciaKm()).isEqualByComparingTo("5.00");

    assertThatThrownBy(() -> oportunidades.buscar(id, coletoraB))
        .isInstanceOf(SolicitacaoNaoEncontradaException.class);
  }

  @Test
  void coletoraSemNenhumAlertaTemListaVazia() {
    inserirSolicitacao("ABERTA");

    assertThat(oportunidades.listarAbertas(coletoraA)).isEmpty();
  }

  private void alertar(Long solicitacao, Long coletora, double km) {
    jdbc.update(
        "insert into alerta_solicitacao (solicitacao_id, coletora_id, distancia_km) values (?,?,?)",
        solicitacao,
        coletora,
        km);
  }

  private Long inserirSolicitacao(String status) {
    return jdbc.queryForObject(
        "insert into solicitacao (geradora_id, peso_estimado_kg, cidade, uf, latitude, longitude,"
            + " status) values (?, 100, 'São Paulo', 'SP', -23.5, -46.6, ?) returning id",
        Long.class,
        geradora,
        status);
  }

  private Long inserirEmpresa(String tipo, String cnpj) {
    return jdbc.queryForObject(
        "insert into empresa (tipo, razao_social, cnpj, cidade, uf)"
            + " values (?, 'Empresa ' || ?, ?, 'São Paulo', 'SP') returning id",
        Long.class,
        tipo,
        cnpj,
        cnpj);
  }
}
