package br.com.ewaste.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.solicitacao.api.SolicitacaoConsulta;
import br.com.ewaste.solicitacao.api.SolicitacaoResumo;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.api.TransicaoSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.TipoResiduo;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * As implementações reais de SolicitacaoConsulta e TransicaoSolicitacao (usadas pelos Devs 4 e 5)
 * contra o PostgreSQL real. Também confere o mapeamento JPA das tabelas solicitacao e
 * solicitacao_item.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SolicitacaoContratosIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired SolicitacaoRepository solicitacoes;
  @Autowired SolicitacaoConsulta consulta;
  @Autowired TransicaoSolicitacao transicao;

  Long geradora;

  @BeforeEach
  void limparBanco() {
    jdbc.execute(
        "truncate alerta_solicitacao, solicitacao_item, solicitacao, usuario, empresa"
            + " restart identity cascade");
    geradora =
        jdbc.queryForObject(
            "insert into empresa (tipo, razao_social, cnpj, cidade, uf)"
                + " values ('GERADORA', 'Geradora Teste', '11222333000181', 'São Paulo', 'SP')"
                + " returning id",
            Long.class);
  }

  @Test
  void consultaDevolveResumoDaSolicitacaoGravada() {
    Solicitacao nova =
        Solicitacao.abrir(
            geradora,
            "HDs de servidores",
            new BigDecimal("250.50"),
            true,
            5,
            "Av. Paulista, 1000",
            "São Paulo",
            "SP",
            -23.5614,
            -46.6559);
    nova.adicionarItem(TipoResiduo.MIDIA_ARMAZENAMENTO, 5, new BigDecimal("10"));
    Long id = solicitacoes.save(nova).getId();

    SolicitacaoResumo resumo = consulta.buscar(id).orElseThrow();

    assertThat(resumo.geradoraId()).isEqualTo(geradora);
    assertThat(resumo.status()).isEqualTo(StatusSolicitacao.ABERTA);
    assertThat(resumo.coletoraId()).isNull();
    assertThat(resumo.pesoEstimadoKg()).isEqualByComparingTo("250.50");
    assertThat(resumo.contemMidias()).isTrue();
    assertThat(resumo.qtdMidias()).isEqualTo(5);
    assertThat(resumo.uf()).isEqualTo("SP");
    assertThat(resumo.agendadaPara()).isNull();
    assertThat(
            jdbc.queryForObject(
                "select tipo_residuo from solicitacao_item where solicitacao_id = ?",
                String.class,
                id))
        .isEqualTo("MIDIA_ARMAZENAMENTO");
  }

  @Test
  void consultaDeIdInexistenteVemVazia() {
    assertThat(consulta.buscar(999L)).isEmpty();
  }

  @Test
  void transicoesDoRastreioSaoGravadasNoBanco() {
    Long id = inserirSolicitacao("AGENDADA");

    transicao.marcarColetada(id);
    assertThat(statusNoBanco(id)).isEqualTo("COLETADA");

    transicao.marcarConcluida(id);
    assertThat(statusNoBanco(id)).isEqualTo("CONCLUIDA");
  }

  @Test
  void transicaoInvalidaLancaIllegalStateENaoMudaNada() {
    Long id = inserirSolicitacao("ABERTA");

    // Como o Dev 4 vê o contrato: só conhece IllegalStateException
    assertThatThrownBy(() -> transicao.marcarColetada(id))
        .isInstanceOf(IllegalStateException.class);
    assertThat(statusNoBanco(id)).isEqualTo("ABERTA");
  }

  private Long inserirSolicitacao(String status) {
    return jdbc.queryForObject(
        "insert into solicitacao (geradora_id, peso_estimado_kg, cidade, uf, latitude, longitude,"
            + " status) values (?, 100, 'São Paulo', 'SP', -23.5, -46.6, ?) returning id",
        Long.class,
        geradora,
        status);
  }

  private String statusNoBanco(Long id) {
    return jdbc.queryForObject("select status from solicitacao where id = ?", String.class, id);
  }
}
