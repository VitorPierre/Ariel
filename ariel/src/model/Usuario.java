package model;

/** MODEL: usuário do sistema. A senha nunca é guardada em texto: só o hash (veja util/Senha). */
public class Usuario {
    private String nome;
    private String login;
    private String senhaHash;
    private final Perfil perfil;
    /** Id da linha em `Usuario` no banco. null = ainda não foi salvo no MySQL (só existe em memória). Uso exclusivo do DAO. */
    private Integer idUsuario;

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

    public Integer idUsuario() { return idUsuario; }
    /** Chamado só pelo DAO depois do INSERT (quando idUsuario ainda é null). */
    public void vincularIdUsuario(int id) { this.idUsuario = id; }

    public void alterarNome(String nome) { this.nome = nome; }
    public void alterarLogin(String login) { this.login = login; }
    public void alterarSenhaHash(String hash) { this.senhaHash = hash; }

    @Override public String toString() { return nome + " (" + login + ")"; }
}
