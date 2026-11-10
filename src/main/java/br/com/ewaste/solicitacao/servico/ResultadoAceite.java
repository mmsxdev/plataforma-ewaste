package br.com.ewaste.solicitacao.servico;

/** Desfechos esperados de uma tentativa de aceite (nenhum deles é erro do sistema). */
public enum ResultadoAceite {
  /** Esta coletora venceu: a solicitação ficou ACEITA para ela. */
  ACEITO,
  /** Outra coletora aceitou antes (ou a solicitação não está mais ABERTA). */
  JA_ACEITA,
  /** A própria coletora já tinha vencido (ex.: clique duplo no botão); nada muda. */
  JA_ERA_SUA,
  /** A solicitação não está mais ABERTA e ninguém a aceitou (ex.: cancelada ou expirada). */
  INDISPONIVEL,
  /** A homologação da coletora não está ativa (ex.: suspensa depois do alerta). */
  HOMOLOGACAO_INATIVA
}
