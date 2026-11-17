package model;

/**
 * Regra de negócio violada por um CAMPO específico do formulário (ex.: CPF inválido).
 * O controller diz qual campo; a tela usa isso só para colocar o cursor no lugar certo.
 */
public class ValidacaoException extends RegraNegocioException {
    public final String campo;

    public ValidacaoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }
}
