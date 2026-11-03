package br.com.ewaste.roteamento.repositorio;

import br.com.ewaste.roteamento.dominio.AlertaSolicitacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela alerta_solicitacao (Spring Data gera a implementação a partir dos nomes). */
public interface AlertaSolicitacaoRepository
    extends JpaRepository<AlertaSolicitacao, AlertaSolicitacao.Chave> {

  boolean existsBySolicitacaoId(Long solicitacaoId);

  long countBySolicitacaoId(Long solicitacaoId);

  Optional<AlertaSolicitacao> findBySolicitacaoIdAndColetoraId(Long solicitacaoId, Long coletoraId);

  List<AlertaSolicitacao> findByColetoraIdOrderByDistanciaKm(Long coletoraId);
}
