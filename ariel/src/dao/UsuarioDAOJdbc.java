package dao;

import model.Perfil;
import model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação MySQL de UsuarioDAO, contra a tabela `Usuario` do Ariel_corrigido_fks.sql
 * (idUsuario, Nome, Login, senha, perfilAcesso). `perfilAcesso` guarda Perfil.name() (ADMIN/FUNCIONARIO),
 * não o texto de exibição, pra não quebrar se o texto do enum mudar.
 */
public class UsuarioDAOJdbc implements UsuarioDAO {

    @Override public List<Usuario> listar() {
        String sql = "SELECT idUsuario, Nome, Login, senha, perfilAcesso FROM Usuario";
        List<Usuario> r = new ArrayList<>();
        try (Connection con = ConexaoFabrica.abrir();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) r.add(mapear(rs));
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui listar os usuários.", e);
        }
        return r;
    }

    @Override public Usuario buscarPorLogin(String login) {
        if (login == null) return null;
        String sql = "SELECT idUsuario, Nome, Login, senha, perfilAcesso FROM Usuario WHERE Login = ?";
        try (Connection con = ConexaoFabrica.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui buscar o usuário \"" + login + "\".", e);
        }
    }

    @Override public void salvar(Usuario usuario) {
        if (usuario.idUsuario() == null) inserir(usuario); else atualizar(usuario);
    }

    private void inserir(Usuario usuario) {
        String sql = "INSERT INTO Usuario (Nome, Login, senha, perfilAcesso) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexaoFabrica.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.nome());
            ps.setString(2, usuario.login());
            ps.setString(3, usuario.senhaHash());
            ps.setString(4, usuario.perfil().name());
            ps.executeUpdate();
            try (ResultSet gerado = ps.getGeneratedKeys()) {
                if (gerado.next()) usuario.vincularIdUsuario(gerado.getInt(1));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui salvar o usuário \"" + usuario.login() + "\".", e);
        }
    }

    private void atualizar(Usuario usuario) {
        String sql = "UPDATE Usuario SET Nome = ?, Login = ?, senha = ?, perfilAcesso = ? WHERE idUsuario = ?";
        try (Connection con = ConexaoFabrica.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario.nome());
            ps.setString(2, usuario.login());
            ps.setString(3, usuario.senhaHash());
            ps.setString(4, usuario.perfil().name());
            ps.setInt(5, usuario.idUsuario());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui atualizar o usuário \"" + usuario.login() + "\".", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario(rs.getString("Nome"), rs.getString("Login"), rs.getString("senha"),
                Perfil.valueOf(rs.getString("perfilAcesso")));
        u.vincularIdUsuario(rs.getInt("idUsuario"));
        return u;
    }
}
