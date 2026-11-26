package br.com.ewaste.roteamento.servico;

import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.roteamento.dominio.AlertaSolicitacao;
import br.com.ewaste.roteamento.dominio.ColetoraNoRaio;
import br.com.ewaste.roteamento.dominio.FiltroRegiao;
import br.com.ewaste.roteamento.repositorio.AlertaSolicitacaoRepository;
import br.com.ewaste.shared.events.AlertasEnviadosEvent;
import br.com.ewaste.shared.events.ColetoraHomologadaEvent;
import br.com.ewaste.solicitacao.api.SolicitacaoConsulta;
import br.com.ewaste.solicitacao.api.SolicitacaoResumo;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.EmpresaResumo;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * O roteamento de uma solicitação acontece uma vez, quando ela é criada. Uma coletora homologada
 * depois disso nunca veria as solicitações já abertas na sua região. Ao receber a homologação, este
 * ouvinte alerta a coletora sobre as solicitações ainda ABERTAS dentro do raio dela (RN02 + RN03),
 * sem repetir alertas que já existam.
 */
@Component
class AlertaAoHomologar {

  private static final Logger log = LoggerFactory.getLogger(AlertaAoHomologar.class);

  private final SolicitacaoConsulta solicitacoes;
  private final ColetorasAptas coletorasAptas;
  private final EmpresaConsulta empresas;
  private final AlertaSolicitacaoRepository alertas;
  private final ApplicationEventPublisher eventos;

  AlertaAoHomologar(
      SolicitacaoConsulta solicitacoes,
      ColetorasAptas coletorasAptas,
      EmpresaConsulta empresas,
      AlertaSolicitacaoRepository alertas,
      ApplicationEventPublisher eventos) {
    this.solicitacoes = solicitacoes;
    this.coletorasAptas = coletorasAptas;
    this.empresas = empresas;
    this.alertas = alertas;
    this.eventos = eventos;
  }

  /** Roda depois do commit da aprovação, numa transação própria. */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void aoHomologar(ColetoraHomologadaEvent evento) {
    Long coletoraId = evento.coletoraId();
    if (!coletorasAptas.listarAptas().contains(coletoraId)) {
      log.info(
          "Coletora {} aprovada, mas ainda não está apta; nenhum alerta retroativo", coletoraId);
      return;
    }
    EmpresaResumo coletora = empresas.buscar(coletoraId).orElse(null);
    if (coletora == null) {
      return;
    }
    Instant agora = Instant.now();
    for (SolicitacaoResumo s : solicitacoes.listarAbertas()) {
      List<ColetoraNoRaio> noRaio =
          FiltroRegiao.filtrar(s.latitude(), s.longitude(), List.of(coletora));
      if (noRaio.isEmpty()
          || alertas.findBySolicitacaoIdAndColetoraId(s.id(), coletoraId).isPresent()) {
        continue;
      }
      alertas.save(new AlertaSolicitacao(s.id(), coletoraId, noRaio.get(0).distanciaKm(), agora));
      eventos.publishEvent(new AlertasEnviadosEvent(s.id(), List.of(coletoraId)));
    }
  }
}
