package model;

/**
 * Regra especial (UC22): cliente inadimplente NÃO é bloqueado de vez; um funcionário ou o
 * administrador pode AUTORIZAR a nova locação. O controller lança esta exceção para a tela
 * perguntar "autoriza?"; se sim, a tela repete a operação informando o responsável.
 */
public class ExigeAutorizacaoException extends RegraNegocioException {
    public ExigeAutorizacaoException(String mensagem) { super(mensagem); }
}
