package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.solicitacao.api.SolicitacaoConsulta;
import br.com.ewaste.solicitacao.api.SolicitacaoResumo;
import br.com.ewaste.solicitacao.api.TransicaoSolicitacao;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

/**
 * Implementação PROVISÓRIA (PR 0): não encontra nada e não muda nada, só registra no log. Nos
 * testes, prefira mockar as interfaces. Apague quando a implementação real entrar.
 */
@Fallback
@Component
class SolicitacaoProvisoria implements SolicitacaoConsulta, TransicaoSolicitacao {

  private static final Logger log = LoggerFactory.getLogger(SolicitacaoProvisoria.class);

  @Override
  public Optional<SolicitacaoResumo> buscar(Long id) {
    return Optional.empty();
  }

  @Override
  public void marcarColetada(Long solicitacaoId) {
    log.warn("[provisório] marcarColetada({}) ainda não implementado", solicitacaoId);
  }

  @Override
  public void marcarConcluida(Long solicitacaoId) {
    log.warn("[provisório] marcarConcluida({}) ainda não implementado", solicitacaoId);
  }
}
