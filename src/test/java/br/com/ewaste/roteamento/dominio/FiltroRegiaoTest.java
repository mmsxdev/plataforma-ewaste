package br.com.ewaste.roteamento.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.ewaste.usuario.api.EmpresaResumo;
import br.com.ewaste.usuario.api.TipoEmpresa;
import java.util.List;
import org.junit.jupiter.api.Test;

class FiltroRegiaoTest {

  // Local da solicitação: Av. Paulista (SP)
  static final double LAT = -23.5614;
  static final double LON = -46.6559;

  @Test
  void coletoraDentroDoRaioEhAlertadaComADistancia() {
    // Mooca, a ~5,8 km, raio de 50 km
    var mooca = coletora(2L, -23.5505, -46.6000, 50);

    List<ColetoraNoRaio> resultado = FiltroRegiao.filtrar(LAT, LON, List.of(mooca));

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).coletoraId()).isEqualTo(2L);
    assertThat(resultado.get(0).distanciaKm()).isCloseTo(5.83, within(0.05));
  }

  @Test
  void coletoraForaDoRaioNaoEhAlertada() {
    // Rio de Janeiro, a ~356 km, raio de 40 km
    var rio = coletora(3L, -22.8800, -43.2500, 40);

    assertThat(FiltroRegiao.filtrar(LAT, LON, List.of(rio))).isEmpty();
  }

  @Test
  void distanciaIgualAoRaioEntra() {
    // Mesmo ponto: distância exatamente 0 (sem erro de arredondamento) e raio 0 => no limite
    var noLimite = coletora(4L, LAT, LON, 0);

    assertThat(FiltroRegiao.filtrar(LAT, LON, List.of(noLimite)))
        .extracting(ColetoraNoRaio::coletoraId)
        .containsExactly(4L);
  }

  @Test
  void raioUmPoucoMenorQueADistanciaNaoEntra() {
    // Mooca fica a ~5,8 km: raio de 5 km não alcança, de 6 km alcança
    assertThat(FiltroRegiao.filtrar(LAT, LON, List.of(coletora(2L, -23.5505, -46.6000, 5))))
        .isEmpty();
    assertThat(FiltroRegiao.filtrar(LAT, LON, List.of(coletora(2L, -23.5505, -46.6000, 6))))
        .hasSize(1);
  }

  @Test
  void coletoraSemCoordenadasOuSemRaioEhIgnorada() {
    var semLatitude = coletora(5L, null, -46.6, 50);
    var semLongitude = coletora(6L, -23.5, null, 50);
    var semRaio = coletora(7L, -23.5505, -46.6000, null);

    assertThat(FiltroRegiao.filtrar(LAT, LON, List.of(semLatitude, semLongitude, semRaio)))
        .isEmpty();
  }

  @Test
  void resultadoVemDaMaisPertoParaAMaisLonge() {
    var longe = coletora(10L, -23.70, -46.80, 100); // ~21 km
    var perto = coletora(11L, -23.5505, -46.6000, 100); // ~5,8 km
    var foraDoRaio = coletora(12L, -22.88, -43.25, 100); // ~356 km

    assertThat(FiltroRegiao.filtrar(LAT, LON, List.of(longe, perto, foraDoRaio)))
        .extracting(ColetoraNoRaio::coletoraId)
        .containsExactly(11L, 10L);
  }

  private static EmpresaResumo coletora(Long id, Double lat, Double lon, Integer raio) {
    return new EmpresaResumo(
        id, TipoEmpresa.COLETORA, "Coletora " + id, "000", "Cidade", "SP", lat, lon, raio);
  }
}
