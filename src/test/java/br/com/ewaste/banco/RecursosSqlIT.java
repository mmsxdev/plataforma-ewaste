package br.com.ewaste.banco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.ewaste.TestcontainersConfiguration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Recursos acadêmicos executados somente no PostgreSQL descartável de Testcontainers. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RecursosSqlIT {
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transacao;

  Long ponto;
  Long geradora;
  Long coletora;
  Long usuario;
  Long solicitacao;

  @BeforeEach
  void preparar() {
    jdbc.execute("TRUNCATE empresa, ponto_descarte RESTART IDENTITY CASCADE");
    geradora = empresa("GERADORA", "11222333000181");
    coletora = empresa("COLETORA", "22333444000181");
    usuario =
        jdbc.queryForObject(
            "INSERT INTO usuario (empresa_id, nome, email, senha_hash, perfil)"
                + " VALUES (?, 'Operador fictício', 'sql@teste.invalid', 'hash', 'COLETORA')"
                + " RETURNING id",
            Long.class,
            coletora);
    ponto =
        jdbc.queryForObject(
            "INSERT INTO ponto_descarte (nome, cidade, uf, latitude, longitude, tipos_aceitos)"
                + " VALUES ('Ponto fictício SQL', 'Goiânia', 'GO', -16.68, -49.25,"
                + " 'INFORMATICA, TELEFONIA') RETURNING id",
            Long.class);
    solicitacao =
        jdbc.queryForObject(
            "INSERT INTO solicitacao"
                + " (geradora_id, coletora_id, ponto_descarte_id, peso_estimado_kg, cidade, uf,"
                + " latitude, longitude, status)"
                + " VALUES (?, ?, ?, 10, 'Goiânia', 'GO', -16.68, -49.25, 'COLETADA') RETURNING id",
            Long.class,
            geradora,
            coletora,
            ponto);
  }

  @Test
  void funcaoExigePontoAtivoECategoriaExplicitamenteAceita() {
    assertThat(aceita("INFORMATICA")).isTrue();
    assertThat(aceita(" telefonia ")).isTrue();
    assertThat(aceita("BATERIA")).isFalse();
    assertThat(aceita("CATEGORIA_INVENTADA")).isFalse();
    assertThat(aceita(null)).isFalse();
    jdbc.update("UPDATE ponto_descarte SET ativo = false WHERE id = ?", ponto);
    assertThat(aceita("INFORMATICA")).isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT ponto_aceita_tipo(?, ?)", Boolean.class, -1L, "INFORMATICA"))
        .isFalse();
  }

  @Test
  void funcaoNaoInterpretaListaVaziaComoAceitacaoIrrestrita() {
    jdbc.update("UPDATE ponto_descarte SET tipos_aceitos = NULL WHERE id = ?", ponto);
    assertThat(aceita("INFORMATICA")).isFalse();
    jdbc.update("UPDATE ponto_descarte SET tipos_aceitos = '' WHERE id = ?", ponto);
    assertThat(aceita("OUTRO")).isFalse();
  }

  @Test
  void viewAgregaItensEDocumentosSemMultiplicarQuantidadesNosJoins() {
    jdbc.update(
        "INSERT INTO solicitacao_item (solicitacao_id, tipo_residuo, quantidade, identificacao)"
            + " VALUES (?, 'INFORMATICA', 2, 'Notebooks fictícios'),"
            + " (?, 'TELEFONIA', 3, 'Telefones fictícios')",
        solicitacao,
        solicitacao);
    jdbc.update(
        "INSERT INTO documento_emitido (solicitacao_id, tipo, numero, arquivo_ref)"
            + " VALUES (?, 'CDF', 'SQL-CDF', 'ficticio/cdf.pdf'),"
            + " (?, 'LAUDO_TECNICO', 'SQL-LAUDO', 'ficticio/laudo.pdf')",
        solicitacao,
        solicitacao);
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo) VALUES (?, 'CRIADA'), (?, 'COLETADA')",
        solicitacao,
        solicitacao);

    var linha =
        jdbc.queryForMap(
            "SELECT * FROM vw_acompanhamento_solicitacao WHERE solicitacao_id = ?", solicitacao);
    assertThat(linha.get("ponto_nome")).isEqualTo("Ponto fictício SQL");
    assertThat(linha.get("linhas_itens")).isEqualTo(2L);
    assertThat(linha.get("quantidade_total")).isEqualTo(5L);
    assertThat(linha.get("documentos_emitidos")).isEqualTo(2L);
    assertThat(linha.get("ultimo_evento_em")).isNotNull();
  }

  @Test
  void procedureRegistraUmaDestinacaoERecusaRepeticao() {
    destinar(" Reciclagem fictícia ");
    assertThat(
            jdbc.queryForObject(
                "SELECT descricao FROM evento_rastreio WHERE solicitacao_id = ? AND tipo = 'DESTINADA'",
                String.class,
                solicitacao))
        .isEqualTo("Reciclagem fictícia");
    assertThatThrownBy(() -> destinar("Segunda tentativa"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("já foi registrada");
    assertThat(quantidadeDestinacoes()).isEqualTo(1);
  }

  @Test
  void procedureRejeitaEstadoDescricaoEAtorInvalidosSemGravar() {
    assertThatThrownBy(() -> destinar("\t\n"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("Descreva a destinação");
    jdbc.update("UPDATE usuario SET ativo = false WHERE id = ?", usuario);
    assertThatThrownBy(() -> destinar("Reciclagem fictícia"))
        .isInstanceOf(DataAccessException.class)
        .satisfies(
            ex ->
                assertThat(
                        ((java.sql.SQLException) ((DataAccessException) ex).getMostSpecificCause())
                            .getSQLState())
                    .isEqualTo("42501"));
    jdbc.update("UPDATE usuario SET ativo = true WHERE id = ?", usuario);
    jdbc.update("UPDATE solicitacao SET status = 'AGENDADA' WHERE id = ?", solicitacao);
    assertThatThrownBy(() -> destinar("Reciclagem fictícia"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("estado atual");
    assertThat(quantidadeDestinacoes()).isZero();
  }

  @Test
  void duasChamadasConcorrentesDaProcedureGravamUmaUnicaDestinacao() throws Exception {
    var largada = new CountDownLatch(1);
    List<String> resultados;
    try (var pool = Executors.newFixedThreadPool(2)) {
      var primeira = pool.submit(() -> destinarAoMesmoTempo(largada));
      var segunda = pool.submit(() -> destinarAoMesmoTempo(largada));
      largada.countDown();
      resultados = List.of(primeira.get(), segunda.get());
    }
    assertThat(resultados.stream().filter("OK"::equals).count()).isEqualTo(1);
    assertThat(resultados.stream().filter(r -> r.contains("já foi registrada")).count())
        .isEqualTo(1);
    assertThat(quantidadeDestinacoes()).isEqualTo(1);
  }

  @Test
  void triggerAuditaSomenteMudancaEfetivaEParticipaDoRollback() {
    jdbc.update("UPDATE solicitacao SET status = 'CONCLUIDA' WHERE id = ?", solicitacao);
    jdbc.update("UPDATE solicitacao SET status = 'CONCLUIDA' WHERE id = ?", solicitacao);
    var auditoria =
        jdbc.queryForList(
            "SELECT * FROM solicitacao_auditoria WHERE solicitacao_id = ?", solicitacao);
    assertThat(auditoria).hasSize(1);
    assertThat(auditoria.getFirst().get("status_anterior")).isEqualTo("COLETADA");
    assertThat(auditoria.getFirst().get("status_novo")).isEqualTo("CONCLUIDA");
    assertThat(auditoria.getFirst().get("usuario_banco")).isNotNull();
    assertThat(auditoria.getFirst().get("transacao_id")).isNotNull();

    transacao.executeWithoutResult(
        estado -> {
          jdbc.update("UPDATE solicitacao SET status = 'CANCELADA' WHERE id = ?", solicitacao);
          estado.setRollbackOnly();
        });
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM solicitacao WHERE id = ?", String.class, solicitacao))
        .isEqualTo("CONCLUIDA");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM solicitacao_auditoria WHERE solicitacao_id = ?",
                Integer.class,
                solicitacao))
        .isEqualTo(1);
  }

  @Test
  void rollbackDaProcedureNaoDeixaEventoParcial() {
    transacao.executeWithoutResult(
        estado -> {
          destinar("Reciclagem fictícia");
          estado.setRollbackOnly();
        });
    assertThat(quantidadeDestinacoes()).isZero();
  }

  private Long empresa(String tipo, String cnpj) {
    return jdbc.queryForObject(
        "INSERT INTO empresa (tipo, razao_social, cnpj, cidade, uf)"
            + " VALUES (?, 'Empresa fictícia SQL', ?, 'Goiânia', 'GO') RETURNING id",
        Long.class,
        tipo,
        cnpj);
  }

  private boolean aceita(String tipo) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject("SELECT ponto_aceita_tipo(?, ?)", Boolean.class, ponto, tipo));
  }

  private void destinar(String descricao) {
    jdbc.update("CALL registrar_destinacao_pi(?, ?, ?)", solicitacao, usuario, descricao);
  }

  private int quantidadeDestinacoes() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM evento_rastreio WHERE solicitacao_id = ? AND tipo = 'DESTINADA'",
        Integer.class,
        solicitacao);
  }

  private String destinarAoMesmoTempo(CountDownLatch largada) throws InterruptedException {
    largada.await();
    try {
      destinar("Reciclagem fictícia");
      return "OK";
    } catch (DataAccessException ex) {
      return ex.getMostSpecificCause().getMessage();
    }
  }
}
