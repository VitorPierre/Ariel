package model;

import java.time.LocalDate;

/** Um pagamento recebido de uma locação (entrada, saldo, ou multa/indenização). */
public record Pagamento(LocalDate data, double valor, Forma forma, Tipo tipo) {

    public enum Forma {
        PIX("Pix"), DINHEIRO("Dinheiro"), CARTAO_CREDITO("Cartão de crédito");

        private final String nome;
        Forma(String nome) { this.nome = nome; }
        @Override public String toString() { return nome; }
    }

    public enum Tipo {
        ENTRADA("Entrada"), SALDO("Saldo"), COBRANCA("Multa/indenização");

        public final String nome;
        Tipo(String nome) { this.nome = nome; }
    }
}
