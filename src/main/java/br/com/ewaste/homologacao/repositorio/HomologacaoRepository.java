package br.com.ewaste.homologacao.repositorio;

import br.com.ewaste.homologacao.api.StatusHomologacao;
import br.com.ewaste.homologacao.dominio.Homologacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acesso à tabela homologacao (Spring Data gera a implementação a partir dos nomes). */
public interface HomologacaoRepository extends JpaRepository<Homologacao, Long> {

  /** Homologação de uma coletora, já com os documentos carregados. */
  @Query("select h from Homologacao h left join fetch h.documentos where h.empresaId = :empresaId")
  Optional<Homologacao> findComDocumentosByEmpresaId(Long empresaId);

  boolean existsByEmpresaId(Long empresaId);

  /**
   * Homologações em um status, com os documentos carregados numa única consulta (evita uma consulta
   * extra por coletora ao checar a validade dos documentos).
   */
  @Query(
      "select distinct h from Homologacao h left join fetch h.documentos where h.status = :status")
  List<Homologacao> findComDocumentosByStatus(StatusHomologacao status);
}
