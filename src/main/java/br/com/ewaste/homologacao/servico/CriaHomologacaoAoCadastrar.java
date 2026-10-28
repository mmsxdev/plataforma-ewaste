package br.com.ewaste.homologacao.servico;

import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.repositorio.HomologacaoRepository;
import br.com.ewaste.shared.events.EmpresaCadastradaEvent;
import br.com.ewaste.usuario.api.TipoEmpresa;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Quando uma coletora termina o cadastro (evento publicado pelo Dev 1), cria a homologação dela com
 * status PENDENTE. Geradoras não passam por homologação.
 */
@Component
class CriaHomologacaoAoCadastrar {

  private final HomologacaoRepository homologacoes;

  CriaHomologacaoAoCadastrar(HomologacaoRepository homologacoes) {
    this.homologacoes = homologacoes;
  }

  /**
   * Roda depois do commit do cadastro (a empresa já existe no banco). Como a transação do cadastro
   * já terminou, abre uma nova (REQUIRES_NEW) para conseguir gravar.
   */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void aoCadastrarEmpresa(EmpresaCadastradaEvent evento) {
    if (evento.tipo() != TipoEmpresa.COLETORA) {
      return;
    }
    // Idempotente: se o evento chegar duas vezes, não tenta criar de novo (empresa_id é único)
    if (!homologacoes.existsByEmpresaId(evento.empresaId())) {
      homologacoes.save(Homologacao.pendente(evento.empresaId()));
    }
  }
}
