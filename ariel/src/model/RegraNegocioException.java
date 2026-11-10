package model;

/**
 * Erro de REGRA DE NEGÓCIO (ex.: "cliente inadimplente", "senha muito curta").
 * O controller lança; a tela captura e mostra a mensagem ao usuário.
 * Assim a tela não precisa conhecer a regra: só exibe o texto.
 */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) { super(mensagem); }
}
