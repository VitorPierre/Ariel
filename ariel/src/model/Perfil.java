package model;

/** Perfil de acesso: o administrador cria usuários; o funcionário só altera os próprios dados. */
public enum Perfil {
    ADMIN("Administrador"), FUNCIONARIO("Funcionário");

    private final String nome;
    Perfil(String nome) { this.nome = nome; }
    @Override public String toString() { return nome; }
}
