package br.com.ewaste.homologacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.homologacao.api.StatusHomologacao;
import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao;
import br.com.ewaste.homologacao.repositorio.HomologacaoRepository;
import br.com.ewaste.homologacao.servico.HomologacaoService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fluxo completo contra PostgreSQL real: a coletora pede análise, o admin aprova e a solicitação
 * que já estava ABERTA no raio passa a chegar para ela (antes ficava sem alerta para sempre).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FluxoHomologacaoIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired HomologacaoRepository homologacoes;
  @Autowired HomologacaoService servico;

  long admin;
  long coletora;
  long solicitacaoPerto;
  long solicitacaoLonge;

  @BeforeEach
  void prepararDados() {
    jdbc.execute(
        "truncate documento_homologacao, homologacao, solicitacao, usuario, empresa"
            + " restart identity cascade");
    admin =
        jdbc.queryForObject(
            "insert into usuario (nome, email, senha_hash, perfil)"
                + " values ('Admin', 'admin@teste.com', 'x', 'ADMIN') returning id",
            Long.class);
    long geradora = empresa("GERADORA", "11222333000181", -16.73, -49.35);
    coletora = empresa("COLETORA", "22333444000181", -16.6538, -49.2442);
    solicitacaoPerto = solicitacao(geradora, -16.7290, -49.3474); // ~14 km
    solicitacaoLonge = solicitacao(geradora, -23.5629, -46.6544); // São Paulo
    Homologacao h = Homologacao.pendente(coletora);
    h.adicionarDocumento(
        TipoDocumentoHomologacao.LICENCA_AMBIENTAL,
        "h/a.pdf",
        "a.pdf",
        LocalDate.now().plusYears(1));
    h.adicionarDocumento(TipoDocumentoHomologacao.CADASTRO_IBAMA, "h/b.pdf", "b.pdf", null);
    homologacoes.save(h);
  }

  @Test
  void aprovarAlertaSolicitacoesAbertasNoRaio() {
    servico.enviarParaAnalise(coletora);
    Long id = homologacoes.findComDocumentosByEmpresaId(coletora).orElseThrow().getId();
    assertThat(servico.todas())
        .extracting(Homologacao::getStatus)
        .contains(StatusHomologacao.EM_ANALISE);

    servico.aprovar(id, admin);

    assertThat(alertas(solicitacaoPerto)).isEqualTo(1);
    assertThat(alertas(solicitacaoLonge)).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from evento_rastreio where solicitacao_id = ? and tipo = 'ALERTAS_ENVIADOS'",
                Long.class,
                solicitacaoPerto))
        .isEqualTo(1);
  }

  @Test
  void reprovarNaoGeraAlertas() {
    servico.enviarParaAnalise(coletora);
    Long id = homologacoes.findComDocumentosByEmpresaId(coletora).orElseThrow().getId();

    servico.reprovar(id, admin, "Licença ilegível");

    assertThat(homologacoes.findComDocumentosById(id).orElseThrow().getStatus())
        .isEqualTo(StatusHomologacao.REPROVADA);
    assertThat(alertas(solicitacaoPerto)).isZero();
  }

  private long alertas(long solicitacao) {
    return jdbc.queryForObject(
        "select count(*) from alerta_solicitacao where solicitacao_id = ? and coletora_id = ?",
        Long.class,
        solicitacao,
        coletora);
  }

  private long empresa(String tipo, String cnpj, double lat, double lng) {
    return jdbc.queryForObject(
        "insert into empresa (tipo, razao_social, cnpj, cidade, uf, latitude, longitude,"
            + " raio_atendimento_km) values (?, 'Empresa ' || ?, ?, 'Goiânia', 'GO', ?, ?, 30)"
            + " returning id",
        Long.class,
        tipo,
        cnpj,
        cnpj,
        lat,
        lng);
  }

  private long solicitacao(long geradora, double lat, double lng) {
    return jdbc.queryForObject(
        "insert into solicitacao (geradora_id, peso_estimado_kg, cidade, uf, latitude, longitude)"
            + " values (?, 10, 'Goiânia', 'GO', ?, ?) returning id",
        Long.class,
        geradora,
        lat,
        lng);
  }
}
