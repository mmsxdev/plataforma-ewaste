package br.com.ewaste.usuario.api;

import java.util.List;
import java.util.Optional;

/** Consulta de empresas para outros módulos. Implementação: Dev 1 — Miguel. */
public interface EmpresaConsulta {

  Optional<EmpresaResumo> buscar(Long id);

  List<EmpresaResumo> listarColetoras();
}
