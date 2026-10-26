package br.com.ewaste.usuario.servico;

import br.com.ewaste.usuario.api.EmpresaConsulta;
import br.com.ewaste.usuario.api.EmpresaResumo;
import br.com.ewaste.usuario.api.TipoEmpresa;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Fallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementação PROVISÓRIA (PR 0), lendo a tabela empresa direto por SQL. {@code @Fallback}: assim
 * que o Dev 1 criar a implementação real (um @Service comum), o Spring passa a usar a real. Depois
 * disso, apague esta classe.
 */
@Fallback
@Component
class EmpresaConsultaProvisoria implements EmpresaConsulta {

  private static final String SELECT =
      "select id, tipo, razao_social, cnpj, cidade, uf, latitude, longitude, raio_atendimento_km"
          + " from empresa";

  private final JdbcTemplate jdbc;

  EmpresaConsultaProvisoria(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<EmpresaResumo> buscar(Long id) {
    return jdbc.query(SELECT + " where id = ?", this::mapear, id).stream().findFirst();
  }

  @Override
  public List<EmpresaResumo> listarColetoras() {
    return jdbc.query(SELECT + " where tipo = 'COLETORA' order by id", this::mapear);
  }

  private EmpresaResumo mapear(ResultSet rs, int linha) throws SQLException {
    return new EmpresaResumo(
        rs.getLong("id"),
        TipoEmpresa.valueOf(rs.getString("tipo")),
        rs.getString("razao_social"),
        rs.getString("cnpj"),
        rs.getString("cidade"),
        rs.getString("uf"),
        rs.getObject("latitude", Double.class),
        rs.getObject("longitude", Double.class),
        rs.getObject("raio_atendimento_km", Integer.class));
  }
}
