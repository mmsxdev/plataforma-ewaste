package br.com.ewaste.homologacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.ewaste.TestcontainersConfiguration;
import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.homologacao.api.StatusHomologacao;
import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.dominio.TipoDocumentoHomologacao;
import br.com.ewaste.homologacao.repositorio.HomologacaoRepository;
import br.com.ewaste.shared.events.EmpresaCadastradaEvent;
import br.com.ewaste.usuario.api.TipoEmpresa;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Homologação contra o PostgreSQL real (Testcontainers): listener do cadastro e consulta de aptas.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class HomologacaoIT {

  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transacao;
  @Autowired ApplicationEventPublisher eventos;
  @Autowired HomologacaoRepository homologacoes;
  @Autowired ColetorasAptas aptas;

  static final LocalDate VALIDA = LocalDate.now().plusYears(1);
  static final LocalDate VENCIDA = LocalDate.now().minusDays(5);

  Long admin;

  @BeforeEach
  void limparBanco() {
    jdbc.execute(
        "truncate documento_homologacao, homologacao, usuario, empresa restart identity cascade");
    // analisado_por referencia usuario: precisa de um admin real para aprovar/reprovar
    admin =
        jdbc.queryForObject(
            "insert into usuario (nome, email, senha_hash, perfil)"
                + " values ('Admin', 'admin@teste.com', 'x', 'ADMIN') returning id",
            Long.class);
  }

  // ---- Listener do EmpresaCadastradaEvent ----

  @Test
  void cadastroDeColetoraCriaHomologacaoPendente() {
    long coletora = inserirEmpresa("COLETORA", "22333444000181");

    publicarDentroDeTransacao(new EmpresaCadastradaEvent(coletora, TipoEmpresa.COLETORA));

    assertThat(homologacoes.findComDocumentosByEmpresaId(coletora))
        .get()
        .extracting(Homologacao::getStatus)
        .isEqualTo(StatusHomologacao.PENDENTE);
  }

  @Test
  void cadastroDeGeradoraNaoCriaHomologacao() {
    long geradora = inserirEmpresa("GERADORA", "11222333000181");

    publicarDentroDeTransacao(new EmpresaCadastradaEvent(geradora, TipoEmpresa.GERADORA));

    assertThat(homologacoes.count()).isZero();
  }

  @Test
  void eventoRepetidoNaoDuplicaHomologacao() {
    long coletora = inserirEmpresa("COLETORA", "22333444000181");

    publicarDentroDeTransacao(new EmpresaCadastradaEvent(coletora, TipoEmpresa.COLETORA));
    publicarDentroDeTransacao(new EmpresaCadastradaEvent(coletora, TipoEmpresa.COLETORA));

    assertThat(homologacoes.count()).isEqualTo(1);
  }

  // ---- ColetorasAptas real ----

  @Test
  void soAprovadasComDocumentosValidosSaoAptas() {
    long apta = coletoraAprovadaCom(VALIDA, "10000000000101");
    long vencida = coletoraAprovadaCom(VENCIDA, "10000000000202");
    long pendente = inserirEmpresa("COLETORA", "10000000000303");
    homologacoes.save(Homologacao.pendente(pendente));
    long reprovada = inserirEmpresa("COLETORA", "10000000000404");
    Homologacao h = Homologacao.pendente(reprovada);
    h.reprovar(admin, "Documento ilegível");
    homologacoes.save(h);
    long semHomologacao = inserirEmpresa("COLETORA", "10000000000505");

    assertThat(aptas.listarAptas()).containsExactly(apta);
    assertThat(aptas.isApta(apta)).isTrue();
    assertThat(aptas.isApta(vencida)).isFalse();
    assertThat(aptas.isApta(pendente)).isFalse();
    assertThat(aptas.isApta(reprovada)).isFalse();
    assertThat(aptas.isApta(semHomologacao)).isFalse();
  }

  // ---- Auxiliares ----

  /** Publica como o Dev 1 fará: dentro da transação do cadastro, que então é confirmada. */
  private void publicarDentroDeTransacao(Object evento) {
    transacao.executeWithoutResult(status -> eventos.publishEvent(evento));
  }

  private long coletoraAprovadaCom(LocalDate validade, String cnpj) {
    long empresa = inserirEmpresa("COLETORA", cnpj);
    Homologacao h = Homologacao.pendente(empresa);
    h.adicionarDocumento(TipoDocumentoHomologacao.LICENCA_AMBIENTAL, "h/a.pdf", "a.pdf", validade);
    h.adicionarDocumento(TipoDocumentoHomologacao.CADASTRO_IBAMA, "h/b.pdf", "b.pdf", validade);
    h.aprovar(admin);
    homologacoes.save(h);
    return empresa;
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
