package br.com.ewaste.solicitacao.dominio;

import static br.com.ewaste.solicitacao.api.StatusSolicitacao.ABERTA;
import static br.com.ewaste.solicitacao.api.StatusSolicitacao.ACEITA;
import static br.com.ewaste.solicitacao.api.StatusSolicitacao.AGENDADA;
import static br.com.ewaste.solicitacao.api.StatusSolicitacao.CANCELADA;
import static br.com.ewaste.solicitacao.api.StatusSolicitacao.COLETADA;
import static br.com.ewaste.solicitacao.api.StatusSolicitacao.CONCLUIDA;
import static br.com.ewaste.solicitacao.api.StatusSolicitacao.EXPIRADA;

import br.com.ewaste.solicitacao.api.StatusSolicitacao;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Regra única do ciclo de vida da solicitação. Todo caminho que muda o status passa por aqui.
 *
 * <pre>
 * ABERTA -> ACEITA -> AGENDADA -> COLETADA -> CONCLUIDA
 * ABERTA -> CANCELADA | EXPIRADA
 * </pre>
 *
 * A regra fica no domínio, e não no enum {@link StatusSolicitacao}, porque o enum está no pacote
 * api (contrato entre módulos).
 */
public final class TransicoesSolicitacao {

  /** Destinos permitidos a partir de cada status. Os status fora do mapa são finais. */
  private static final Map<StatusSolicitacao, Set<StatusSolicitacao>> PERMITIDAS =
      Map.of(
          ABERTA, EnumSet.of(ACEITA, CANCELADA, EXPIRADA),
          ACEITA, EnumSet.of(AGENDADA),
          AGENDADA, EnumSet.of(COLETADA),
          COLETADA, EnumSet.of(CONCLUIDA));

  private TransicoesSolicitacao() {}

  public static boolean podeIrPara(StatusSolicitacao origem, StatusSolicitacao destino) {
    return PERMITIDAS.getOrDefault(origem, Set.of()).contains(destino);
  }

  /** Lança {@link TransicaoInvalidaException} se a transição não for permitida. */
  public static void exigir(StatusSolicitacao origem, StatusSolicitacao destino) {
    if (!podeIrPara(origem, destino)) {
      throw new TransicaoInvalidaException(origem, destino);
    }
  }
}
