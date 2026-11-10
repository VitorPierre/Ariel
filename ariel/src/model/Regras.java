package model;

/**
 * Valores das regras de negócio em um só lugar (Documento de Requisitos v0.5).
 * Mudou a regra? Altere aqui.
 */
public final class Regras {
    private Regras() { }

    /** RF18 / RN02: todo orçamento vale 30 dias, sem exceção (o sistema expira sozinho). */
    public static final int DIAS_VALIDADE_ORCAMENTO = 30;
    /** Avisa "vence em X dias" a partir de X dias. */
    public static final int DIAS_ALERTA_VENCIMENTO = 3;
    /**
     * O sinal (entrada) é VARIÁVEL e definido em cada orçamento (RF04): não existe percentual mínimo fixo.
     * Estes são só os atalhos da tela "Novo orçamento" (10%, 20% ou 50% do total).
     */
    public static final double[] SINAIS_RAPIDOS = {0.10, 0.20, 0.50};
    /** Atalho que já vem marcado (o protótipo mostra 20%). */
    public static final double SINAL_PADRAO = 0.20;

    // Obs.: não há valores de multa aqui. Multas e indenizações NUNCA são calculadas nem sugeridas
    // pelo sistema: a proprietária digita o valor (RN05, RN06, UC11).

    public static final int LOGIN_MIN = 3;
    public static final int SENHA_MIN = 6;
    /** Segurança do login: após N erros seguidos, trava por alguns segundos. */
    public static final int MAX_TENTATIVAS_LOGIN = 5;
    public static final int BLOQUEIO_LOGIN_SEGUNDOS = 30;
}
