package br.com.ewaste.homologacao.servico;

import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.EmpresaResumo;
import java.util.List;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Component;

/**
 * Implementação PROVISÓRIA (PR 0): considera TODAS as coletoras aptas, para o Dev 3 poder testar o
 * roteamento antes da homologação existir. Apague quando a implementação real entrar.
 */
@Fallback
@Component
class ColetorasAptasProvisoria implements ColetorasAptas {

  private final EmpresaConsulta empresas;

  ColetorasAptasProvisoria(EmpresaConsulta empresas) {
    this.empresas = empresas;
  }

  @Override
  public boolean isApta(Long coletoraId) {
    return listarAptas().contains(coletoraId);
  }

  @Override
  public List<Long> listarAptas() {
    return empresas.listarColetoras().stream().map(EmpresaResumo::id).toList();
  }
}
