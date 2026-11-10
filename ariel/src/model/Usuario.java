package model;

/** MODEL: usuário do sistema. A senha nunca é guardada em texto: só o hash (veja util/Senha). */
public class Usuario {
    private String nome;
    private String login;
    private String senhaHash;
    private final Perfil perfil;

    public Usuario(String nome, String login, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.login = login;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public String nome() { return nome; }
    public String login() { return login; }
    public String senhaHash() { return senhaHash; }
    public Perfil perfil() { return perfil; }
    public boolean ehAdmin() { return perfil == Perfil.ADMIN; }

    public void alterarNome(String nome) { this.nome = nome; }
    public void alterarLogin(String login) { this.login = login; }
    public void alterarSenhaHash(String hash) { this.senhaHash = hash; }

    @Override public String toString() { return nome + " (" + login + ")"; }
}
