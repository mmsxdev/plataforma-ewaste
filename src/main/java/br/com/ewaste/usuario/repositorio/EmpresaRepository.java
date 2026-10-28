package br.com.ewaste.usuario.repositorio;

import br.com.ewaste.usuario.api.TipoEmpresa;
import br.com.ewaste.usuario.dominio.Empresa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
  boolean existsByCnpj(String cnpj);

  List<Empresa> findByTipoOrderById(TipoEmpresa tipo);
}
