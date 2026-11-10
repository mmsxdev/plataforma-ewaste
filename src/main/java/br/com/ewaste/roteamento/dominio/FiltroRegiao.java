package br.com.ewaste.roteamento.dominio;

import br.com.ewaste.shared.geo.Haversine;
import br.com.ewaste.usuario.api.EmpresaResumo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Regra de região (RN03): a coletora recebe o alerta se o local da solicitação está dentro do raio
 * de atendimento dela. Classe pura (sem Spring nem banco) para ser testada com dados montados à
 * mão.
 */
public final class FiltroRegiao {

  private FiltroRegiao() {}

  /**
   * Coletoras cujo raio alcança o ponto, da mais perto para a mais longe. Distância igual ao raio
   * entra (limite incluído). Coletora sem coordenadas ou sem raio é ignorada.
   */
  public static List<ColetoraNoRaio> filtrar(
      double latitude, double longitude, Collection<EmpresaResumo> coletoras) {
    List<ColetoraNoRaio> noRaio = new ArrayList<>();
    for (EmpresaResumo c : coletoras) {
      if (c.latitude() == null || c.longitude() == null || c.raioAtendimentoKm() == null) {
        continue;
      }
      double distancia = Haversine.distanciaKm(latitude, longitude, c.latitude(), c.longitude());
      if (distancia <= c.raioAtendimentoKm()) {
        noRaio.add(new ColetoraNoRaio(c.id(), distancia));
      }
    }
    noRaio.sort(Comparator.comparingDouble(ColetoraNoRaio::distanciaKm));
    return noRaio;
  }
}
