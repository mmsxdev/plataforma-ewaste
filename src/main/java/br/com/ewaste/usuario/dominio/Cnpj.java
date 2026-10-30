package br.com.ewaste.usuario.dominio;

public final class Cnpj {
  private Cnpj() {}

  public static String normalizar(String valor) {
    return valor == null ? "" : valor.replaceAll("[./\\-\\s]", "");
  }

  public static boolean valido(String valor) {
    String d = normalizar(valor);
    if (!d.matches("[0-9]{14}") || d.chars().distinct().count() == 1) return false;
    return digito(d, 12) == d.charAt(12) - '0' && digito(d, 13) == d.charAt(13) - '0';
  }

  private static int digito(String valor, int tamanho) {
    int soma = 0;
    for (int i = 0; i < tamanho; i++) soma += (valor.charAt(i) - '0') * ((tamanho - 1 - i) % 8 + 2);
    int resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  }
}
