package br.com.ewaste.mapa.servico;

import br.com.ewaste.mapa.api.PontoResumo;
import br.com.ewaste.mapa.api.PontosColeta;
import br.com.ewaste.mapa.dominio.ArvoreAvl;
import br.com.ewaste.mapa.repositorio.PontoColetaRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PontosColetaService implements PontosColeta {
  private final PontoColetaRepository pontos;

  public PontosColetaService(PontoColetaRepository pontos) {
    this.pontos = pontos;
  }

  private ArvoreAvl<Long, PontoResumo> carregarArvore() {
    ArvoreAvl<Long, PontoResumo> arvore = new ArvoreAvl<>();
    // Registros vindos do banco entram na árvore antes de qualquer busca, filtro ou exibição.
    for (PontoResumo ponto : pontos.listarAtivos()) arvore.inserir(ponto.id(), ponto);
    return arvore;
  }

  @Override
  public List<PontoResumo> consultar(String cidade, String tipo) {
    String filtro = cidade == null ? "" : cidade.strip().toLowerCase(Locale.ROOT);
    return carregarArvore().emOrdem().stream()
        .filter(p -> p.cidade().toLowerCase(Locale.ROOT).contains(filtro))
        .filter(p -> tipo == null || tipo.isBlank() || p.categorias().contains(tipo))
        .toList();
  }

  @Override
  public Optional<PontoResumo> buscar(Long id) {
    return id == null ? Optional.empty() : carregarArvore().buscar(id);
  }

  @Override
  @Transactional
  public void validar(Long id, List<String> tipos) {
    if (id == null) return; // solicitações legadas e coleta no endereço continuam válidas
    if (!pontos.travarAtivo(id))
      throw new IllegalArgumentException("Ponto de coleta indisponível.");
    PontoResumo ponto =
        buscar(id).orElseThrow(() -> new IllegalArgumentException("Ponto de coleta indisponível."));
    if (tipos.isEmpty()
        || tipos.stream().anyMatch(t -> !ponto.categorias().contains(t) || !pontos.aceita(id, t)))
      throw new IllegalArgumentException(
          "O ponto selecionado não recebe todos os tipos de resíduos informados.");
  }
}
