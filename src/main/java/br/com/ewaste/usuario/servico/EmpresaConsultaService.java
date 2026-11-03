package br.com.ewaste.usuario.servico;

import br.com.ewaste.usuario.api.*;
import br.com.ewaste.usuario.dominio.Empresa;
import br.com.ewaste.usuario.repositorio.EmpresaRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmpresaConsultaService implements EmpresaConsulta {
  private final EmpresaRepository empresas;

  public EmpresaConsultaService(EmpresaRepository empresas) {
    this.empresas = empresas;
  }

  @Override
  public Optional<EmpresaResumo> buscar(Long id) {
    return empresas.findById(id).map(Empresa::resumo);
  }

  @Override
  public List<EmpresaResumo> listarColetoras() {
    return empresas.findByTipoOrderById(TipoEmpresa.COLETORA).stream()
        .map(Empresa::resumo)
        .toList();
  }
}
