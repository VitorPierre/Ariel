package model;

/**
 * Regra de negócio que impede uma ação e cuja solução está em OUTRO lugar do sistema.
 * O controller diz ONDE resolver (cliente, produto ou a própria reserva); a tela só abre o lugar certo.
 */
public class PendenciaException extends RegraNegocioException {

    public enum Onde { CLIENTE, PRODUTO, RESERVA }

    public final Onde onde;
    public final Catalogo.Item produto;      // preenchido quando onde == PRODUTO

    public PendenciaException(Onde onde, String mensagem) { this(onde, null, mensagem); }

    public PendenciaException(Onde onde, Catalogo.Item produto, String mensagem) {
        super(mensagem);
        this.onde = onde;
        this.produto = produto;
    }
}
