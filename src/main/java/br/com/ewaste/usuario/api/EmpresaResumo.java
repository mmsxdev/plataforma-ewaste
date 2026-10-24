package br.com.ewaste.usuario.api;

/**
 * Dados públicos de uma empresa para outros módulos.
 *
 * @param raioAtendimentoKm só preenchido para coletoras
 */
public record EmpresaResumo(
    Long id,
    TipoEmpresa tipo,
    String razaoSocial,
    String cnpj,
    String cidade,
    String uf,
    Double latitude,
    Double longitude,
    Integer raioAtendimentoKm) {}
