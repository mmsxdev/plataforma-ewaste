package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.solicitacao.api.SolicitacaoConsulta;
import br.com.ewaste.solicitacao.api.SolicitacaoResumo;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação real da consulta usada pelo rastreio (Dev 4) e pelas notificações (Dev 5).
 * Substitui a SolicitacaoProvisoria (que não encontrava nada).
 */
@Service
@Transactional(readOnly = true)
class SolicitacaoConsultaService implements SolicitacaoConsulta {

  private final SolicitacaoRepository solicitacoes;

  SolicitacaoConsultaService(SolicitacaoRepository solicitacoes) {
    this.solicitacoes = solicitacoes;
  }

  @Override
  public Optional<SolicitacaoResumo> buscar(Long id) {
    return solicitacoes.findById(id).map(SolicitacaoConsultaService::resumir);
  }

  static SolicitacaoResumo resumir(Solicitacao s) {
    return new SolicitacaoResumo(
        s.getId(),
        s.getGeradoraId(),
        s.getColetoraId(),
        s.getStatus(),
        s.getPesoEstimadoKg(),
        s.isContemMidias(),
        s.getQtdMidias(),
        s.getCidade(),
        s.getUf(),
        s.getLatitude(),
        s.getLongitude(),
        s.getAgendadaPara() == null
            ? null
            : s.getAgendadaPara().atZone(Solicitacao.FUSO).toOffsetDateTime());
  }
}
