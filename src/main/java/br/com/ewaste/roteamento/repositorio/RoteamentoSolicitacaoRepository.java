package br.com.ewaste.roteamento.repositorio;

import br.com.ewaste.roteamento.dominio.RoteamentoSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso à tabela roteamento_solicitacao. */
public interface RoteamentoSolicitacaoRepository
    extends JpaRepository<RoteamentoSolicitacao, Long> {

  /**
   * Registra que a solicitação foi roteada, se ainda não estiver registrada. Uma instrução só: com
   * duas entregas do mesmo evento ao mesmo tempo, o PostgreSQL deixa só uma inserir.
   *
   * @return 1 na primeira vez; 0 se a solicitação já tinha sido roteada
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO roteamento_solicitacao (solicitacao_id) VALUES (:solicitacaoId)"
              + " ON CONFLICT (solicitacao_id) DO NOTHING",
      nativeQuery = true)
  int registrarSeNovo(@Param("solicitacaoId") Long solicitacaoId);
}
