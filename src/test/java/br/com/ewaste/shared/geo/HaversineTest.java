package br.com.ewaste.shared.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class HaversineTest {

  @Test
  void mesmoPontoTemDistanciaZero() {
    assertThat(Haversine.distanciaKm(-23.55, -46.63, -23.55, -46.63)).isZero();
  }

  @Test
  void saoPauloAoRioDeJaneiroTemCercaDe360Km() {
    // Praça da Sé (SP) -> Cinelândia (RJ): ~357 km em linha reta
    double km = Haversine.distanciaKm(-23.5503, -46.6339, -22.9110, -43.1759);
    assertThat(km).isCloseTo(357, within(5.0));
  }

  @Test
  void centrosDeSaoPauloERioTemCercaDe361Km() {
    // Coordenadas de referência das cidades (spec da frente do Dev 3)
    double km = Haversine.distanciaKm(-23.5505, -46.6333, -22.9068, -43.1729);
    assertThat(km).isCloseTo(361, within(3.0));
  }

  @Test
  void saoPauloABeloHorizonteTemCercaDe491Km() {
    double km = Haversine.distanciaKm(-23.5505, -46.6333, -19.9167, -43.9345);
    assertThat(km).isCloseTo(491, within(3.0));
  }

  @Test
  void distanciaEhSimetrica() {
    double ida = Haversine.distanciaKm(-23.5, -46.6, -22.9, -43.2);
    double volta = Haversine.distanciaKm(-22.9, -43.2, -23.5, -46.6);
    assertThat(ida).isCloseTo(volta, within(1e-9));
  }
}
