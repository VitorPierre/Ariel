package model;

import java.time.LocalDate;

/** Cobrança gerada na devolução: atraso (multa), dano ou perda de peça. O valor entra no saldo da locação. */
public class Ocorrencia {

    public enum Tipo {
        ATRASO("Atraso"), DANO("Dano"), PERDA("Perda");

        private final String nome;
        Tipo(String nome) { this.nome = nome; }
        @Override public String toString() { return nome; }
    }

    public final Tipo tipo;
    public final String descricao;
    public final double valor;
    public final LocalDate data;
    public final String registradoPor;

    public Ocorrencia(Tipo tipo, String descricao, double valor, LocalDate data, String registradoPor) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.registradoPor = registradoPor;
    }
}
