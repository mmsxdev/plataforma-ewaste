package br.com.ewaste.roteamento.servico;

import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.roteamento.dominio.AlertaSolicitacao;
import br.com.ewaste.roteamento.dominio.ColetoraNoRaio;
import br.com.ewaste.roteamento.dominio.FiltroRegiao;
import br.com.ewaste.roteamento.repositorio.AlertaSolicitacaoRepository;
import br.com.ewaste.roteamento.repositorio.RoteamentoSolicitacaoRepository;
import br.com.ewaste.shared.events.AlertasEnviadosEvent;
import br.com.ewaste.shared.events.SemColetoraNaRegiaoEvent;
import br.com.ewaste.shared.events.SolicitacaoCriadaEvent;
import br.com.ewaste.solicitacao.api.SolicitacaoConsulta;
import br.com.ewaste.solicitacao.api.SolicitacaoResumo;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.EmpresaResumo;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Broadcast da solicitação nova para as coletoras aptas da região (RF03, RN02, RN03). Grava um
 * alerta por coletora e avisa os outros módulos por evento; a solicitação continua ABERTA.
 */
@Component
class RoteamentoListener {

  private static final Logger log = LoggerFactory.getLogger(RoteamentoListener.class);

  private final SolicitacaoConsulta solicitacoes;
  private final ColetorasAptas coletorasAptas;
  private final EmpresaConsulta empresas;
  private final AlertaSolicitacaoRepository alertas;
  private final RoteamentoSolicitacaoRepository roteamentos;
  private final ApplicationEventPublisher eventos;

  RoteamentoListener(
      SolicitacaoConsulta solicitacoes,
      ColetorasAptas coletorasAptas,
      EmpresaConsulta empresas,
      AlertaSolicitacaoRepository alertas,
      RoteamentoSolicitacaoRepository roteamentos,
      ApplicationEventPublisher eventos) {
    this.solicitacoes = solicitacoes;
    this.coletorasAptas = coletorasAptas;
    this.empresas = empresas;
    this.alertas = alertas;
    this.roteamentos = roteamentos;
    this.eventos = eventos;
  }

  /**
   * Roda depois do commit da criação (a solicitação já existe no banco e, se a criação tivesse
   * falhado, ninguém seria alertado). Como aquela transação já terminou, abre uma nova para gravar.
   * Os eventos publicados aqui chegam aos ouvintes depois do commit desta nova transação.
   */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void aoCriarSolicitacao(SolicitacaoCriadaEvent evento) {
    Long id = evento.solicitacaoId();
    SolicitacaoResumo solicitacao = solicitacoes.buscar(id).orElse(null);
    if (solicitacao == null || solicitacao.status() != StatusSolicitacao.ABERTA) {
      log.warn("Roteamento ignorado: solicitação {} inexistente ou não está ABERTA", id);
      return;
    }
    // Idempotente: o roteamento fica registrado mesmo sem coletora no raio (zero alertas). Se o
    // mesmo evento chegar de novo, o registro já existe (0 linhas) e nada é alertado nem notificado
    if (roteamentos.registrarSeNovo(id) == 0) {
      return;
    }

    List<ColetoraNoRaio> noRaio =
        FiltroRegiao.filtrar(
            solicitacao.latitude(), solicitacao.longitude(), coletorasAptasComEndereco());

    Instant agora = Instant.now();
    alertas.saveAll(
        noRaio.stream()
            .map(c -> new AlertaSolicitacao(id, c.coletoraId(), c.distanciaKm(), agora))
            .toList());

    if (noRaio.isEmpty()) {
      eventos.publishEvent(new SemColetoraNaRegiaoEvent(id, evento.geradoraId()));
    } else {
      eventos.publishEvent(
          new AlertasEnviadosEvent(id, noRaio.stream().map(ColetoraNoRaio::coletoraId).toList()));
    }
  }

  /**
   * RN02: só as aptas (homologadas, documentos em dia). Busca todas as coletoras numa consulta só e
   * cruza em memória, em vez de uma consulta por coletora apta (N+1).
   */
  private List<EmpresaResumo> coletorasAptasComEndereco() {
    Set<Long> aptas = new HashSet<>(coletorasAptas.listarAptas());
    return empresas.listarColetoras().stream().filter(c -> aptas.contains(c.id())).toList();
  }
}
