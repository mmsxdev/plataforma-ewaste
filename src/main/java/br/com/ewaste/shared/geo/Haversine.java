package br.com.ewaste.shared.geo;

/**
 * Distância em linha reta entre dois pontos na Terra (fórmula de Haversine). Dono: Dev 3 — Davi.
 */
public final class Haversine {

  private static final double RAIO_TERRA_KM = 6371.0;

  private Haversine() {}

  public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
    return 2 * RAIO_TERRA_KM * Math.asin(Math.sqrt(a));
  }
}
