package br.com.ewaste.mapa.api;

import java.util.List;
import java.util.Optional;

public interface PontosColeta {
  List<PontoResumo> consultar(String cidade, String tipo);

  Optional<PontoResumo> buscar(Long id);

  /** Consulta de referência histórica: inclui pontos que deixaram de aceitar novos pedidos. */
  Optional<PontoResumo> buscarParaHistorico(Long id);

  void validar(Long id, List<String> tipos);
}
