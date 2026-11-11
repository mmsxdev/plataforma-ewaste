package br.com.ewaste.rastreio.servico;

import br.com.ewaste.armazenamento.api.Armazenamento;
import br.com.ewaste.documento.api.DadosEmissao;
import br.com.ewaste.documento.api.EmissorDocumentos;
import br.com.ewaste.rastreio.api.EventoResumo;
import br.com.ewaste.rastreio.api.RastreioConsulta;
import br.com.ewaste.shared.events.ColetaRegistradaEvent;
import br.com.ewaste.solicitacao.api.SolicitacaoConsulta;
import br.com.ewaste.solicitacao.api.SolicitacaoResumo;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.api.TransicaoSolicitacao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RastreioService implements RastreioConsulta {
  private static final Set<String> TIPOS = Set.of("HDD", "SSD", "FITA", "OUTRO");
  private static final Set<String> METODOS = Set.of("TRITURACAO", "DESMAGNETIZACAO");
  private static final Set<String> NIVEIS = Set.of("H-4", "H-5");

  private final JdbcTemplate jdbc;
  private final SolicitacaoConsulta solicitacoes;
  private final TransicaoSolicitacao transicoes;
  private final Armazenamento armazenamento;
  private final EmissorDocumentos documentos;
  private final ApplicationEventPublisher eventos;

  public RastreioService(
      JdbcTemplate jdbc,
      SolicitacaoConsulta solicitacoes,
      TransicaoSolicitacao transicoes,
      Armazenamento armazenamento,
      EmissorDocumentos documentos,
      ApplicationEventPublisher eventos) {
    this.jdbc = jdbc;
    this.solicitacoes = solicitacoes;
    this.transicoes = transicoes;
    this.armazenamento = armazenamento;
    this.documentos = documentos;
    this.eventos = eventos;
  }

  @Transactional(readOnly = true)
  public List<SolicitacaoResumo> listarDaColetora(Long coletoraId) {
    return solicitacoes.listarDaColetora(coletoraId);
  }

  @Transactional(readOnly = true)
  public SolicitacaoResumo buscarDaColetora(Long id, Long coletoraId) {
    SolicitacaoResumo s = buscar(id);
    if (!coletoraId.equals(s.coletoraId())) {
      throw naoEncontrada();
    }
    return s;
  }

  @Override
  @Transactional(readOnly = true)
  public List<EventoResumo> listar(Long solicitacaoId) {
    return jdbc.query(
        """
        SELECT e.id, e.tipo, e.descricao, e.peso_real_kg, e.evidencia_ref,
               COALESCE(u.nome, 'Sistema') AS autor, e.registrado_em
          FROM evento_rastreio e LEFT JOIN usuario u ON u.id = e.registrado_por
         WHERE e.solicitacao_id = ? ORDER BY e.registrado_em, e.id
        """,
        (rs, row) ->
            new EventoResumo(
                rs.getLong("id"),
                rs.getString("tipo"),
                rs.getString("descricao"),
                rs.getBigDecimal("peso_real_kg"),
                rs.getString("evidencia_ref"),
                rs.getString("autor"),
                rs.getTimestamp("registrado_em").toInstant()),
        solicitacaoId);
  }

  @Transactional(readOnly = true)
  public List<MidiaResumo> listarMidias(Long solicitacaoId) {
    return jdbc.query(
        """
        SELECT id, numero_serie, tipo, metodo, nivel, evidencia_ref, destruida_em
          FROM midia_destruida WHERE solicitacao_id = ? ORDER BY id
        """,
        (rs, row) ->
            new MidiaResumo(
                rs.getLong("id"),
                rs.getString("numero_serie"),
                rs.getString("tipo"),
                rs.getString("metodo"),
                rs.getString("nivel"),
                rs.getString("evidencia_ref"),
                rs.getTimestamp("destruida_em").toInstant()),
        solicitacaoId);
  }

  public void registrarColeta(
      Long id, Long coletoraId, Long usuarioId, BigDecimal pesoRealKg, MultipartFile foto) {
    SolicitacaoResumo s = buscarDaColetora(id, coletoraId);
    BigDecimal peso = validarPeso(pesoRealKg);
    exigirStatus(s, StatusSolicitacao.AGENDADA);
    transicoes.marcarColetada(id);
    String evidencia = guardarFoto(foto, id);
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo, descricao, peso_real_kg, evidencia_ref, registrado_por) VALUES (?, 'COLETADA', 'Coleta registrada', ?, ?, ?)",
        id,
        peso,
        evidencia,
        usuarioId);
    eventos.publishEvent(new ColetaRegistradaEvent(id, s.geradoraId()));
  }

  public void registrarMidia(
      Long id,
      Long coletoraId,
      Long usuarioId,
      String numeroSerie,
      String tipo,
      String metodo,
      String nivel,
      MultipartFile foto) {
    SolicitacaoResumo s = buscarDaColetora(id, coletoraId);
    exigirStatus(s, StatusSolicitacao.COLETADA);
    if (!s.contemMidias()) {
      throw new IllegalArgumentException("Esta solicitação não contém mídias.");
    }
    if (numeroSerie == null
        || numeroSerie.isBlank()
        || numeroSerie.length() > 100
        || !TIPOS.contains(tipo)
        || !METODOS.contains(metodo)
        || !NIVEIS.contains(nivel)) {
      throw new IllegalArgumentException("Confira os dados da mídia destruída.");
    }
    travarSeColetada(id);
    if (listarMidias(id).size() >= s.qtdMidias()) {
      throw new IllegalArgumentException("Todas as mídias já foram registradas.");
    }
    String evidencia = guardarFoto(foto, id);
    jdbc.update(
        "INSERT INTO midia_destruida (solicitacao_id, numero_serie, tipo, metodo, nivel, evidencia_ref, destruida_em) VALUES (?, ?, ?, ?, ?, ?, ?)",
        id,
        numeroSerie.strip(),
        tipo,
        metodo,
        nivel.strip(),
        evidencia,
        Timestamp.from(Instant.now()));
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo, descricao, registrado_por) VALUES (?, 'MIDIAS_DESTRUIDAS', ?, ?)",
        id,
        "Mídia destruída: " + numeroSerie.strip(),
        usuarioId);
  }

  public void registrarDestinacao(Long id, Long coletoraId, Long usuarioId, String destinacao) {
    SolicitacaoResumo s = buscarDaColetora(id, coletoraId);
    exigirStatus(s, StatusSolicitacao.COLETADA);
    if (destinacao == null || destinacao.isBlank() || destinacao.length() > 500) {
      throw new IllegalArgumentException("Descreva a destinação final.");
    }
    travarSeColetada(id);
    if (existeEvento(id, "DESTINADA")) {
      throw new IllegalArgumentException("A destinação já foi registrada.");
    }
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo, descricao, registrado_por) VALUES (?, 'DESTINADA', ?, ?)",
        id,
        destinacao.strip(),
        usuarioId);
  }

  public void concluir(Long id, Long coletoraId, Long usuarioId) {
    SolicitacaoResumo s = buscarDaColetora(id, coletoraId);
    exigirStatus(s, StatusSolicitacao.COLETADA);
    travarSeColetada(id);
    List<EventoResumo> historico = listar(id);
    BigDecimal peso =
        historico.stream()
            .filter(e -> e.tipo().equals("COLETADA"))
            .map(EventoResumo::pesoRealKg)
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Registre a coleta antes de concluir."));
    String destinacao =
        historico.stream()
            .filter(e -> e.tipo().equals("DESTINADA"))
            .map(EventoResumo::descricao)
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Registre a destinação antes de concluir."));
    List<MidiaResumo> midias = listarMidias(id);
    if (s.contemMidias() && midias.size() != s.qtdMidias()) {
      throw new IllegalArgumentException("Registre todas as mídias destruídas antes de concluir.");
    }
    transicoes.marcarConcluida(id);
    jdbc.update(
        "INSERT INTO evento_rastreio (solicitacao_id, tipo, descricao, registrado_por) VALUES (?, 'CONCLUIDA', 'Coleta concluída', ?)",
        id,
        usuarioId);
    List<DadosEmissao.Midia> dadosMidias =
        midias.stream()
            .map(m -> new DadosEmissao.Midia(m.numeroSerie(), m.tipo(), m.metodo(), m.nivel()))
            .toList();
    documentos.emitir(new DadosEmissao(id, s.geradoraId(), peso, destinacao, dadosMidias));
  }

  @Transactional(readOnly = true)
  public Resource evidenciaDaColeta(Long id, Long eventoId, Long coletoraId) {
    buscarDaColetora(id, coletoraId);
    return carregarEvidencia(
        "SELECT evidencia_ref FROM evento_rastreio WHERE id = ? AND solicitacao_id = ?",
        eventoId,
        id);
  }

  @Transactional(readOnly = true)
  public Resource evidenciaDaMidia(Long id, Long midiaId, Long coletoraId) {
    buscarDaColetora(id, coletoraId);
    return carregarEvidencia(
        "SELECT evidencia_ref FROM midia_destruida WHERE id = ? AND solicitacao_id = ?",
        midiaId,
        id);
  }

  private Resource carregarEvidencia(String sql, Long itemId, Long solicitacaoId) {
    List<String> caminhos = jdbc.query(sql, (rs, row) -> rs.getString(1), itemId, solicitacaoId);
    if (caminhos.isEmpty() || caminhos.getFirst() == null) {
      throw naoEncontrada();
    }
    return armazenamento.carregar(caminhos.getFirst());
  }

  private String guardarFoto(MultipartFile foto, Long id) {
    if (foto == null || foto.isEmpty()) {
      return null;
    }
    if (!Set.of("image/jpeg", "image/png").contains(foto.getContentType())) {
      throw new IllegalArgumentException("Envie uma foto JPG ou PNG.");
    }
    return armazenamento.salvar(foto, "rastreio/" + id).caminho();
  }

  private boolean existeEvento(Long id, String tipo) {
    return jdbc.queryForObject(
            "SELECT count(*) FROM evento_rastreio WHERE solicitacao_id = ? AND tipo = ?",
            Integer.class,
            id,
            tipo)
        > 0;
  }

  private void travarSeColetada(Long id) {
    String status =
        jdbc.queryForObject(
            "SELECT status FROM solicitacao WHERE id = ? FOR UPDATE", String.class, id);
    if (!StatusSolicitacao.COLETADA.name().equals(status)) {
      throw new IllegalArgumentException(
          "Esta operação não está disponível no estado atual da coleta.");
    }
  }

  private static BigDecimal validarPeso(BigDecimal valor) {
    if (valor == null) {
      throw new IllegalArgumentException(
          "Informe um peso real maior que zero, com até duas casas decimais.");
    }
    BigDecimal peso;
    try {
      peso = valor.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException(
          "Informe um peso real maior que zero, com até duas casas decimais.");
    }
    if (peso.signum() <= 0 || peso.precision() > 10) {
      throw new IllegalArgumentException(
          "Informe um peso real maior que zero, com até duas casas decimais.");
    }
    return peso;
  }

  private SolicitacaoResumo buscar(Long id) {
    return solicitacoes.buscar(id).orElseThrow(RastreioService::naoEncontrada);
  }

  private static void exigirStatus(SolicitacaoResumo s, StatusSolicitacao status) {
    if (s.status() != status) {
      throw new IllegalArgumentException(
          "Esta operação não está disponível no estado atual da coleta.");
    }
  }

  private static ResponseStatusException naoEncontrada() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Coleta não encontrada.");
  }
}
