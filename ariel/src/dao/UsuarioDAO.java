package dao;

import model.Usuario;

import java.util.List;

/** Acesso a `Usuario`. Quem decide as regras (login único, senha mínima...) continua sendo o UsuarioController. */
public interface UsuarioDAO {
    List<Usuario> listar();
    Usuario buscarPorLogin(String login);
    /** Insere (idUsuario() == null) ou atualiza. */
    void salvar(Usuario usuario);
}
