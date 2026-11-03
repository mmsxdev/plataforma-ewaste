package br.com.ewaste.solicitacao.repositorio;

import br.com.ewaste.solicitacao.dominio.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela solicitacao (Spring Data gera a implementação a partir dos nomes). */
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {}
