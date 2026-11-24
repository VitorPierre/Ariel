package dao;

import model.Cliente;
import model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação MySQL de ClienteDAO, contra `Cliente` do Ariel_corrigido_fks.sql + as colunas
 * acrescentadas em dao/sql/Ariel_alteracoes_dao.sql (rg, instagram, whatsapp, endereco, observacoes, ativo).
 *
 * `Usuario_idUsuario` faz parte da PK composta de Cliente nesse schema, mas Cliente.java não guarda
 * "de qual usuário é este cliente" — essa ideia não existe no model hoje. Por isso este DAO recebe o
 * usuário logado no construtor (quem chama já sabe quem é, veio do LoginController) e usa esse id
 * como o "dono" de todo cliente que salva/lê, sem precisar inventar um campo novo em Cliente.java.
 *
 * NÃO coberto ainda (sem tabela no schema atual): Cliente.autorizadas (pessoas autorizadas a
 * retirar/devolver) — fica só em memória por enquanto.
 */
public class ClienteDAOJdbc implements ClienteDAO {

    private final Usuario usuarioAtual;

    public ClienteDAOJdbc(Usuario usuarioAtual) {
        if (usuarioAtual == null || usuarioAtual.idUsuario() == null)
            throw new IllegalArgumentException("usuarioAtual precisa estar logado e já salvo no banco (idUsuario != null).");
        this.usuarioAtual = usuarioAtual;
    }

    private static final String COLUNAS =
        "idCliente, Nome, CPF, Inadiplente, rg, instagram, whatsapp, endereco, observacoes, ativo";

    @Override public List<Cliente> listar() {
        String sql = "SELECT " + COLUNAS + " FROM Cliente WHERE Usuario_idUsuario = ?";
        List<Cliente> r = new ArrayList<>();
        try (Connection con = ConexaoFabrica.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) r.add(mapear(rs)); }
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui listar os clientes.", e);
        }
        return r;
    }

    @Override public void salvar(Cliente cliente) {
        if (cliente.idCliente == null) inserir(cliente); else atualizar(cliente);
    }

    private void inserir(Cliente c) {
        String sql = "INSERT INTO Cliente (Nome, CPF, Inadiplente, Usuario_idUsuario, rg, instagram, whatsapp, endereco, observacoes, ativo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConexaoFabrica.abrir();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preencher(ps, c, false);
                ps.executeUpdate();
                try (ResultSet gerado = ps.getGeneratedKeys()) {
                    if (gerado.next()) c.idCliente = gerado.getInt(1);
                }
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui salvar o cliente \"" + c.nome + "\".", e);
        }
    }

    private void atualizar(Cliente c) {
        String sql = "UPDATE Cliente SET Nome=?, CPF=?, Inadiplente=?, Usuario_idUsuario=?, rg=?, instagram=?, "
                + "whatsapp=?, endereco=?, observacoes=?, ativo=? WHERE idCliente=? AND Usuario_idUsuario=?";
        try (Connection con = ConexaoFabrica.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, c, true);
            ps.setInt(11, c.idCliente);
            ps.setInt(12, usuarioAtual.idUsuario());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui atualizar o cliente \"" + c.nome + "\".", e);
        }
    }

    private void preencher(PreparedStatement ps, Cliente c, boolean update) throws SQLException {
        ps.setString(1, c.nome);
        ps.setString(2, c.cpf);
        ps.setBoolean(3, c.inadimplenteManual);
        ps.setInt(4, usuarioAtual.idUsuario());
        ps.setString(5, c.rg);
        ps.setString(6, c.instagram);
        ps.setString(7, c.whatsapp);
        ps.setString(8, c.endereco);
        ps.setString(9, c.observacoes);
        ps.setBoolean(10, c.ativo);
    }

    @Override public void excluir(Cliente cliente) {
        if (cliente.idCliente == null) return;    // nunca foi salvo: nada a fazer
        String sql = "DELETE FROM Cliente WHERE idCliente = ? AND Usuario_idUsuario = ?";
        try (Connection con = ConexaoFabrica.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cliente.idCliente);
            ps.setInt(2, usuarioAtual.idUsuario());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui excluir o cliente \"" + cliente.nome + "\".", e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.idCliente = rs.getInt("idCliente");
        c.nome = rs.getString("Nome");
        c.cpf = rs.getString("CPF");
        c.inadimplenteManual = rs.getBoolean("Inadiplente");
        c.rg = rs.getString("rg");
        c.instagram = rs.getString("instagram");
        c.whatsapp = rs.getString("whatsapp");
        c.endereco = rs.getString("endereco");
        c.observacoes = rs.getString("observacoes");
        c.ativo = rs.getBoolean("ativo");
        return c;
    }
}
