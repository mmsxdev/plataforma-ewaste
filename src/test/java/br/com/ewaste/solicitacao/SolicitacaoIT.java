package br.com.ewaste.solicitacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.shared.events.SolicitacaoCriadaEvent;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.dominio.TipoResiduo;
import br.com.ewaste.solicitacao.servico.SolicitacaoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Criação e consulta de solicitações pela geradora contra o PostgreSQL real. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, EventosCapturados.class})
class SolicitacaoIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transacao;
  @Autowired SolicitacaoService servico;
  @Autowired EventosCapturados eventos;

  Long geradoraA;
  Long geradoraB;

  @BeforeEach
  void limparBanco() {
    jdbc.execute(
        "truncate alerta_solicitacao, solicitacao_item, solicitacao, usuario, empresa"
            + " restart identity cascade");
    eventos.limpar();
    geradoraA = inserirGeradora("11111111000111");
    geradoraB = inserirGeradora("22222222000122");
  }

  @Test
  void criarGravaAbertaComItensEPublicaOEventoDepoisDoCommit() {
    Long id = servico.criar(novaSolicitacao(geradoraA));

    assertThat(jdbc.queryForObject("select status from solicitacao where id = ?", String.class, id))
        .isEqualTo("ABERTA");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from solicitacao_item where solicitacao_id = ?",
                Integer.class,
                id))
        .isEqualTo(1);
    assertThat(eventos.doTipo(SolicitacaoCriadaEvent.class))
        .containsExactly(new SolicitacaoCriadaEvent(id, geradoraA));
  }

  @Test
  void rollbackNaoPublicaOEvento() {
    transacao.executeWithoutResult(
        status -> {
          servico.criar(novaSolicitacao(geradoraA));
          status.setRollbackOnly();
        });

    assertThat(jdbc.queryForObject("select count(*) from solicitacao", Integer.class)).isZero();
    assertThat(eventos.doTipo(SolicitacaoCriadaEvent.class)).isEmpty();
  }

  @Test
  void geradoraSoEnxergaAsProprias() {
    Long daA = servico.criar(novaSolicitacao(geradoraA));

    assertThat(servico.buscarDaGeradora(daA, geradoraA).getItens()).hasSize(1);
    assertThat(servico.buscarDaGeradora(daA, geradoraA).getStatus())
        .isEqualTo(StatusSolicitacao.ABERTA);
    assertThatThrownBy(() -> servico.buscarDaGeradora(daA, geradoraB))
        .isInstanceOf(SolicitacaoNaoEncontradaException.class);
    assertThat(servico.listarDaGeradora(geradoraA)).hasSize(1);
    assertThat(servico.listarDaGeradora(geradoraB)).isEmpty();
  }

  private Solicitacao novaSolicitacao(Long geradora) {
    Solicitacao s =
        Solicitacao.abrir(
            geradora,
            "PCs",
            new BigDecimal("250"),
            false,
            0,
            null,
            "São Paulo",
            "SP",
            -23.56,
            -46.65);
    s.adicionarItem(TipoResiduo.INFORMATICA, 10, new BigDecimal("250"));
    return s;
  }

  private Long inserirGeradora(String cnpj) {
    return jdbc.queryForObject(
        "insert into empresa (tipo, razao_social, cnpj, cidade, uf)"
            + " values ('GERADORA', 'Geradora ' || ?, ?, 'São Paulo', 'SP') returning id",
        Long.class,
        cnpj,
        cnpj);
  }
}
