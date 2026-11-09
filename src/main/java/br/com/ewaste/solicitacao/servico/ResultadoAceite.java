package br.com.ewaste.solicitacao.servico;

/** Desfechos esperados de uma tentativa de aceite (nenhum deles é erro do sistema). */
public enum ResultadoAceite {
  /** Esta coletora venceu: a solicitação ficou ACEITA para ela. */
  ACEITO,
  /** Outra coletora aceitou antes (ou a solicitação não está mais ABERTA). */
  JA_ACEITA,
  /** A homologação da coletora não está ativa (ex.: suspensa depois do alerta). */
  HOMOLOGACAO_INATIVA
}
