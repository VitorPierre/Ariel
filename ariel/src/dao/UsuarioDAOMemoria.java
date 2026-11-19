package dao;

import model.Usuario;

import java.util.ArrayList;
import java.util.List;

/** Guarda os usuários na própria lista que o UsuarioController já usava — comportamento idêntico ao de hoje. */
public class UsuarioDAOMemoria implements UsuarioDAO {

    private final List<Usuario> usuarios;

    /** @param usuarios a MESMA lista que o controller usa (ela é compartilhada, não copiada). */
    public UsuarioDAOMemoria(List<Usuario> usuarios) { this.usuarios = usuarios; }

    @Override public List<Usuario> listar() { return new ArrayList<>(usuarios); }

    @Override public Usuario buscarPorLogin(String login) {
        if (login == null) return null;
        for (Usuario u : usuarios) if (u.login().equalsIgnoreCase(login.trim())) return u;
        return null;
    }

    @Override public void salvar(Usuario usuario) {
        if (!usuarios.contains(usuario)) usuarios.add(usuario);
        // já é o mesmo objeto em memória: alterações feitas pelo controller já estão "salvas".
    }
}
