package util;

/** Utilitário de CPF: só dígitos e validação dos dígitos verificadores. */
public final class Cpf {
    private Cpf() { }

    /** Mantém só os números do texto. */
    public static String digitos(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) if (Character.isDigit(ch)) sb.append(ch);
        return sb.toString();
    }

    /** Confere os dois dígitos verificadores do CPF. */
    public static boolean valido(String cpf) {
        String d = digitos(cpf);
        if (d.length() != 11) return false;
        boolean todosIguais = true;
        for (int i = 1; i < 11; i++) if (d.charAt(i) != d.charAt(0)) { todosIguais = false; break; }
        if (todosIguais) return false;
        return digitoVerificador(d.substring(0, 9)) == d.charAt(9) - '0'
                && digitoVerificador(d.substring(0, 10)) == d.charAt(10) - '0';
    }

    private static int digitoVerificador(String base) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) soma += (base.charAt(i) - '0') * (base.length() + 1 - i);
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
