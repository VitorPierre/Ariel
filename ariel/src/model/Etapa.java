package model;

/** Etapa de uma locação no painel: define em qual coluna o cartão aparece. */
public enum Etapa {
    ORCAMENTO("Orçamento"), LOCADO("Locado"), DEVOLVIDO("Devolvido");

    public final String nome;

    Etapa(String nome) { this.nome = nome; }
}
