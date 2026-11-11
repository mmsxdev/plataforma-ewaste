package br.com.ewaste.solicitacao.repositorio;

import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import br.com.ewaste.solicitacao.dominio.Solicitacao;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /** Solicitação só se esta coletora venceu o aceite. */
  Optional<Solicitacao> findByIdAndColetoraId(Long id, Long coletoraId);

  List<Solicitacao> findByColetoraIdOrderByAceitaEmDesc(Long coletoraId);

  List<Solicitacao> findByColetoraIdAndStatusInOrderByAceitaEmDesc(
      Long coletoraId, Collection<StatusSolicitacao> status);

  /**
   * Aceite atômico (RF04 / RN04). Só altera a linha se ela ainda estiver ABERTA, numa única
   * instrução: o PostgreSQL trava a linha, e quem chegar depois reavalia o WHERE e altera 0 linhas.
   * Nada de "ler o status, checar no Java e salvar", que deixaria duas coletoras vencerem.
   *
   * <p>{@code version + 1} mantém o optimistic locking coerente; {@code clearAutomatically}
   * descarta as entidades em memória, que estariam com o status antigo.
   *
   * @return 1 se esta coletora venceu, 0 se a solicitação já não estava ABERTA
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          UPDATE solicitacao
             SET status = 'ACEITA', coletora_id = :coletoraId,
                 aceita_em = now(), version = version + 1
           WHERE id = :id AND status = 'ABERTA'
          """,
      nativeQuery = true)
  int aceitarSeAberta(@Param("id") Long id, @Param("coletoraId") Long coletoraId);
}
