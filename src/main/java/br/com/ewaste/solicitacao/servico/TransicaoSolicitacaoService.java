package br.com.ewaste.solicitacao.servico;

import br.com.ewaste.solicitacao.api.TransicaoSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import br.com.ewaste.solicitacao.dominio.SolicitacaoNaoEncontradaException;
import br.com.ewaste.solicitacao.repositorio.SolicitacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transições pedidas pelo rastreio (Dev 4). A validação fica na entidade: transição inválida lança
 * TransicaoInvalidaException (uma IllegalStateException, como o contrato promete). Substitui a
 * SolicitacaoProvisoria (que só registrava no log).
 */
@Service
@Transactional
class TransicaoSolicitacaoService implements TransicaoSolicitacao {

  private final SolicitacaoRepository solicitacoes;

  TransicaoSolicitacaoService(SolicitacaoRepository solicitacoes) {
    this.solicitacoes = solicitacoes;
  }

  @Override
  public void marcarColetada(Long solicitacaoId) {
    buscar(solicitacaoId).marcarColetada();
  }

  @Override
  public void marcarConcluida(Long solicitacaoId) {
    buscar(solicitacaoId).marcarConcluida();
  }

  /** A entidade fica gerenciada pela transação: a mudança de status é gravada no commit. */
  private Solicitacao buscar(Long id) {
    return solicitacoes.findById(id).orElseThrow(() -> new SolicitacaoNaoEncontradaException(id));
  }
}
