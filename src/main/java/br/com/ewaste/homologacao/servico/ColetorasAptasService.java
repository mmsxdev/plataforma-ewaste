package br.com.ewaste.homologacao.servico;

import br.com.ewaste.homologacao.api.ColetorasAptas;
import br.com.ewaste.homologacao.api.StatusHomologacao;
import br.com.ewaste.homologacao.dominio.Homologacao;
import br.com.ewaste.homologacao.repositorio.HomologacaoRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação real do filtro de homologação, usado pelo roteamento do Dev 3 para decidir quem
 * recebe alertas. Substitui a ColetorasAptasProvisoria (que considerava todas aptas).
 */
@Service
@Transactional(readOnly = true)
class ColetorasAptasService implements ColetorasAptas {

  /** Fuso usado para "hoje" ao comparar com a validade dos documentos. */
  static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  private final HomologacaoRepository homologacoes;

  ColetorasAptasService(HomologacaoRepository homologacoes) {
    this.homologacoes = homologacoes;
  }

  /** Sem homologação cadastrada = não apta. */
  @Override
  public boolean isApta(Long coletoraId) {
    return homologacoes
        .findComDocumentosByEmpresaId(coletoraId)
        .map(h -> h.isApta(hoje()))
        .orElse(false);
  }

  /** Parte só das APROVADAS e aplica a regra de documentos de cada uma. */
  @Override
  public List<Long> listarAptas() {
    LocalDate hoje = hoje();
    return homologacoes.findComDocumentosByStatus(StatusHomologacao.APROVADA).stream()
        .filter(h -> h.isApta(hoje))
        .map(Homologacao::getEmpresaId)
        .sorted()
        .toList();
  }

  private static LocalDate hoje() {
    return LocalDate.now(FUSO);
  }
}
