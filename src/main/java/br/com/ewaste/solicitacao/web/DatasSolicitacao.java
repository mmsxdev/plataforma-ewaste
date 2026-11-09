package br.com.ewaste.solicitacao.web;

import br.com.ewaste.solicitacao.dominio.Solicitacao;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Formata datas nos templates do módulo, no fuso de São Paulo. Uso: {@code
 * ${@datasSolicitacao.dataHora(s.criadaEm)}}.
 */
@Component("datasSolicitacao")
public class DatasSolicitacao {

  private static final DateTimeFormatter DATA_HORA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(Solicitacao.FUSO);

  /** Data e hora locais, ou "—" quando ainda não aconteceu. */
  public String dataHora(Instant instante) {
    return instante == null ? "—" : DATA_HORA.format(instante);
  }
}
