package br.com.ewaste.mapa.repositorio;

import br.com.ewaste.mapa.api.PontoResumo;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PontoColetaRepository {
  private final JdbcTemplate jdbc;

  public PontoColetaRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<PontoResumo> listarAtivos() {
    return jdbc.query(
        "SELECT id,nome,endereco,cidade,uf,tipos_aceitos,horario FROM ponto_descarte WHERE ativo = true",
        (rs, row) ->
            new PontoResumo(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("endereco"),
                rs.getString("cidade"),
                rs.getString("uf"),
                rs.getString("tipos_aceitos"),
                rs.getString("horario")));
  }

  public boolean aceita(Long id, String tipo) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject("SELECT ponto_aceita_tipo(?, ?)", Boolean.class, id, tipo));
  }

  /** Mantém disponibilidade e categorias estáveis até o commit da solicitação. */
  public boolean travarAtivo(Long id) {
    return !jdbc.queryForList(
            "SELECT id FROM ponto_descarte WHERE id = ? AND ativo = true FOR SHARE", Long.class, id)
        .isEmpty();
  }
}
