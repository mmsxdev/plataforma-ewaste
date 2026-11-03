package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.roteamento.api.AlertaConsulta;
import br.com.ewaste.roteamento.api.AlertaResumo;
import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O que a coletora enxerga: só as solicitações para as quais ela foi alertada pelo roteamento. A de
 * outra coletora (ou sem alerta) responde 404.
 */
@Service
@Transactional(readOnly = true)
public class OportunidadeService {

  private final SolicitacaoRepository solicitacoes;
  private final AlertaConsulta alertas;

  OportunidadeService(SolicitacaoRepository solicitacoes, AlertaConsulta alertas) {
    this.solicitacoes = solicitacoes;
    this.alertas = alertas;
  }

  /** Solicitações ABERTAS alertadas para a coletora, da mais perto para a mais longe. */
  public List<Oportunidade> listarAbertas(Long coletoraId) {
    Map<Long, BigDecimal> distancias =
        alertas.listarDaColetora(coletoraId).stream()
            .collect(Collectors.toMap(AlertaResumo::solicitacaoId, AlertaResumo::distanciaKm));
    if (distancias.isEmpty()) {
      return List.of();
    }
    return solicitacoes.findByIdInAndStatus(distancias.keySet(), StatusSolicitacao.ABERTA).stream()
        .map(s -> new Oportunidade(s, distancias.get(s.getId())))
        .sorted(Comparator.comparing(Oportunidade::distanciaKm))
        .toList();
  }

  /** Detalhe, em qualquer status, desde que a coletora tenha sido alertada. */
  public Oportunidade buscar(Long solicitacaoId, Long coletoraId) {
    BigDecimal distancia =
        alertas
            .buscar(solicitacaoId, coletoraId)
            .map(AlertaResumo::distanciaKm)
            .orElseThrow(() -> new SolicitacaoNaoEncontradaException(solicitacaoId));
    return solicitacoes
        .findComItens(solicitacaoId)
        .map(s -> new Oportunidade(s, distancia))
        .orElseThrow(() -> new SolicitacaoNaoEncontradaException(solicitacaoId));
  }
}
