package br.com.ewaste.roteamento.servico;

import br.com.ewaste.roteamento.api.AlertaConsulta;
import br.com.ewaste.roteamento.api.AlertaResumo;
import br.com.ewaste.roteamento.dominio.AlertaSolicitacao;
import br.com.ewaste.roteamento.repositorio.AlertaSolicitacaoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de alertas para o módulo solicitacao. Só depende do próprio repositório. */
@Service
@Transactional(readOnly = true)
class AlertaConsultaService implements AlertaConsulta {

  private final AlertaSolicitacaoRepository alertas;

  AlertaConsultaService(AlertaSolicitacaoRepository alertas) {
    this.alertas = alertas;
  }

  @Override
  public boolean foiAlertada(Long solicitacaoId, Long coletoraId) {
    return buscar(solicitacaoId, coletoraId).isPresent();
  }

  @Override
  public Optional<AlertaResumo> buscar(Long solicitacaoId, Long coletoraId) {
    return alertas
        .findBySolicitacaoIdAndColetoraId(solicitacaoId, coletoraId)
        .map(AlertaConsultaService::resumir);
  }

  @Override
  public List<AlertaResumo> listarDaColetora(Long coletoraId) {
    return alertas.findByColetoraIdOrderByDistanciaKm(coletoraId).stream()
        .map(AlertaConsultaService::resumir)
        .toList();
  }

  @Override
  public long contarAlertas(Long solicitacaoId) {
    return alertas.countBySolicitacaoId(solicitacaoId);
  }

  private static AlertaResumo resumir(AlertaSolicitacao a) {
    return new AlertaResumo(
        a.getSolicitacaoId(), a.getColetoraId(), a.getDistanciaKm(), a.getEnviadoEm());
  }
}
