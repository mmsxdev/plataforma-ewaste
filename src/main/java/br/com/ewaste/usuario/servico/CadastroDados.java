package br.com.ewaste.usuario.servico;

import br.com.ewaste.usuario.dominio.Cnpj;
import jakarta.validation.constraints.*;

/**
 * Dados de entrada; o tipo da empresa e o perfil são definidos pela rota, nunca pelo formulário.
 */
public record CadastroDados(
    @NotBlank(message = "Informe a razão social.") @Size(max = 150) String razaoSocial,
    @NotBlank @Size(max = 30) String cnpj,
    @NotBlank @Size(max = 20) String telefone,
    @NotBlank @Size(max = 200) String endereco,
    @NotBlank @Size(max = 100) String cidade,
    @NotBlank
        @Pattern(
            regexp =
                "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
            message = "Selecione uma UF válida.")
        String uf,
    @NotNull(message = "Informe a latitude.") @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @NotNull(message = "Informe a longitude.") @DecimalMin("-180") @DecimalMax("180")
        Double longitude,
    @Min(1) Integer raioAtendimentoKm,
    @NotBlank @Size(max = 120) String nome,
    @NotBlank @Email(message = "Informe um e-mail válido.") @Size(max = 150) String email,
    @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha) {

  @AssertTrue(message = "Informe um CNPJ válido, com dígitos verificadores.")
  public boolean isCnpjValido() {
    return Cnpj.valido(cnpj);
  }

  @AssertTrue(message = "A senha deve ocupar no máximo 72 bytes em UTF-8.")
  public boolean isSenhaCompativel() {
    return senha == null || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
  }

  public static CadastroDados vazio() {
    return new CadastroDados(
        null, null, null, null, null, null, null, null, null, null, null, null);
  }
}
