package br.com.ewaste.solicitacao.repositorio;

import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acesso à tabela solicitacao (Spring Data gera a implementação a partir dos nomes). */
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

  /** Solicitações de uma geradora, as mais novas primeiro. */
  List<Solicitacao> findByGeradoraIdOrderByCriadaEmDesc(Long geradoraId);

  /**
   * Solicitação só se for da geradora informada, já com os itens carregados (a tela de detalhe
   * mostra os itens e o open-in-view está desligado).
   */
  @Query(
      "select s from Solicitacao s left join fetch s.itens"
          + " where s.id = :id and s.geradoraId = :geradoraId")
  Optional<Solicitacao> findDaGeradoraComItens(Long id, Long geradoraId);

  @Query("select s from Solicitacao s left join fetch s.itens where s.id = :id")
  Optional<Solicitacao> findComItens(Long id);

  List<Solicitacao> findByIdInAndStatus(Collection<Long> ids, StatusSolicitacao status);
}
