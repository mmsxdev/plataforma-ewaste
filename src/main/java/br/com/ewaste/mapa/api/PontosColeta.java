package br.com.ewaste.mapa.api;

import java.util.List;
import java.util.Optional;

public interface PontosColeta {
  List<PontoResumo> consultar(String cidade, String tipo);

  Optional<PontoResumo> buscar(Long id);

  void validar(Long id, List<String> tipos);
}
