package dao;

import model.Catalogo;
import model.Usuario;

import java.sql.*;
import java.util.*;

/**
 * Implementação MySQL de CatalogoDAO, contra Categoria/Tema/Serviço/Produto/Pacote/Pacote_has_Produto/
 * Tema_has_Pacote do Ariel_corrigido_fks.sql + as colunas de dao/sql/Ariel_alteracoes_dao.sql.
 *
 * Igual ao ClienteDAOJdbc: Categoria, Tema, Serviço e Produto são "do usuário logado"
 * (Usuario_idUsuario faz parte da PK deles nesse schema) — por isso o usuário atual entra no
 * construtor. `Pacote` é a ÚNICA tabela de catálogo sem Usuario_idUsuario no schema original
 * (fica visível pra todo mundo); não inventei escopo novo pra ela aqui, só reproduzi o que o SQL diz.
 */
public class CatalogoDAOJdbc implements CatalogoDAO {

    private final Usuario usuarioAtual;

    public CatalogoDAOJdbc(Usuario usuarioAtual) {
        if (usuarioAtual == null || usuarioAtual.idUsuario() == null)
            throw new IllegalArgumentException("usuarioAtual precisa estar logado e já salvo no banco (idUsuario != null).");
        this.usuarioAtual = usuarioAtual;
    }

    // ================= Categorias =================

    @Override public List<Catalogo.Categoria> listarCategorias() {
        String sql = "SELECT idCategoria, Nome, descricao, ativo FROM Categoria WHERE Usuario_idUsuario = ?";
        List<Catalogo.Categoria> r = new ArrayList<>();
        try (Connection con = ConexaoFabrica.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Catalogo.Categoria c = new Catalogo.Categoria(rs.getString("Nome"), rs.getString("descricao"));
                    c.ativo = rs.getBoolean("ativo");
                    c.idCategoria = rs.getInt("idCategoria");
                    r.add(c);
                }
            }
        } catch (SQLException e) { throw new RepositorioException("Não consegui listar as categorias.", e); }
        return r;
    }

    @Override public void salvarCategoria(Catalogo.Categoria categoria) {
        try (Connection con = ConexaoFabrica.abrir()) {
            if (categoria.idCategoria == null) {
                String sql = "INSERT INTO Categoria (Nome, Usuario_idUsuario, descricao, ativo) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, categoria.nome);
                    ps.setInt(2, usuarioAtual.idUsuario());
                    ps.setString(3, categoria.descricao);
                    ps.setBoolean(4, categoria.ativo);
                    ps.executeUpdate();
                    try (ResultSet g = ps.getGeneratedKeys()) { if (g.next()) categoria.idCategoria = g.getInt(1); }
                }
            } else {
                String sql = "UPDATE Categoria SET Nome=?, descricao=?, ativo=? WHERE idCategoria=? AND Usuario_idUsuario=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, categoria.nome);
                    ps.setString(2, categoria.descricao);
                    ps.setBoolean(3, categoria.ativo);
                    ps.setInt(4, categoria.idCategoria);
                    ps.setInt(5, usuarioAtual.idUsuario());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) { throw new RepositorioException("Não consegui salvar a categoria \"" + categoria.nome + "\".", e); }
    }

    @Override public void excluirCategoria(Catalogo.Categoria categoria) {
        if (categoria.idCategoria == null) return;
        try (Connection con = ConexaoFabrica.abrir();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Categoria WHERE idCategoria=? AND Usuario_idUsuario=?")) {
            ps.setInt(1, categoria.idCategoria);
            ps.setInt(2, usuarioAtual.idUsuario());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RepositorioException("Não consegui excluir a categoria \"" + categoria.nome + "\".", e); }
    }

    // ================= Temas =================

    @Override public List<Catalogo.Tema> listarTemas() {
        String sql = "SELECT idTema, nome, descricao, ativo FROM Tema WHERE Usuario_idUsuario = ?";
        List<Catalogo.Tema> r = new ArrayList<>();
        try (Connection con = ConexaoFabrica.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Catalogo.Tema t = new Catalogo.Tema(rs.getString("nome"), rs.getString("descricao"));
                    t.ativo = rs.getBoolean("ativo");
                    t.idTema = rs.getInt("idTema");
                    r.add(t);
                }
            }
        } catch (SQLException e) { throw new RepositorioException("Não consegui listar os temas.", e); }
        return r;
    }

    @Override public void salvarTema(Catalogo.Tema tema) {
        try (Connection con = ConexaoFabrica.abrir()) {
            if (tema.idTema == null) {
                String sql = "INSERT INTO Tema (nome, Usuario_idUsuario, descricao, ativo) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, tema.nome);
                    ps.setInt(2, usuarioAtual.idUsuario());
                    ps.setString(3, tema.descricao);
                    ps.setBoolean(4, tema.ativo);
                    ps.executeUpdate();
                    try (ResultSet g = ps.getGeneratedKeys()) { if (g.next()) tema.idTema = g.getInt(1); }
                }
            } else {
                String sql = "UPDATE Tema SET nome=?, descricao=?, ativo=? WHERE idTema=? AND Usuario_idUsuario=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, tema.nome);
                    ps.setString(2, tema.descricao);
                    ps.setBoolean(3, tema.ativo);
                    ps.setInt(4, tema.idTema);
                    ps.setInt(5, usuarioAtual.idUsuario());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) { throw new RepositorioException("Não consegui salvar o tema \"" + tema.nome + "\".", e); }
    }

    @Override public void excluirTema(Catalogo.Tema tema) {
        if (tema.idTema == null) return;
        try (Connection con = ConexaoFabrica.abrir();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Tema WHERE idTema=? AND Usuario_idUsuario=?")) {
            ps.setInt(1, tema.idTema);
            ps.setInt(2, usuarioAtual.idUsuario());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RepositorioException("Não consegui excluir o tema \"" + tema.nome + "\".", e); }
    }

    // ================= Serviços =================
    // tipoServiço guarda o Servico.nome (não criei coluna redundante); distanciaServiço não é usado pelo Java.

    @Override public List<Catalogo.Servico> listarServicos() {
        String sql = "SELECT idServiço, tipoServiço, valor, descricao, ativo FROM Serviço WHERE Usuario_idUsuario = ?";
        List<Catalogo.Servico> r = new ArrayList<>();
        try (Connection con = ConexaoFabrica.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Catalogo.Servico s = new Catalogo.Servico(rs.getString("tipoServiço"), rs.getString("descricao"), rs.getDouble("valor"));
                    s.ativo = rs.getBoolean("ativo");
                    s.idServico = rs.getInt("idServiço");
                    r.add(s);
                }
            }
        } catch (SQLException e) { throw new RepositorioException("Não consegui listar os serviços.", e); }
        return r;
    }

    @Override public void salvarServico(Catalogo.Servico servico) {
        try (Connection con = ConexaoFabrica.abrir()) {
            if (servico.idServico == null) {
                String sql = "INSERT INTO Serviço (tipoServiço, valor, distanciaServiço, Usuario_idUsuario, descricao, ativo) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, servico.nome);
                    ps.setDouble(2, servico.valor);
                    ps.setString(3, "");
                    ps.setInt(4, usuarioAtual.idUsuario());
                    ps.setString(5, servico.descricao);
                    ps.setBoolean(6, servico.ativo);
                    ps.executeUpdate();
                    try (ResultSet g = ps.getGeneratedKeys()) { if (g.next()) servico.idServico = g.getInt(1); }
                }
            } else {
                String sql = "UPDATE Serviço SET tipoServiço=?, valor=?, descricao=?, ativo=? WHERE idServiço=? AND Usuario_idUsuario=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, servico.nome);
                    ps.setDouble(2, servico.valor);
                    ps.setString(3, servico.descricao);
                    ps.setBoolean(4, servico.ativo);
                    ps.setInt(5, servico.idServico);
                    ps.setInt(6, usuarioAtual.idUsuario());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) { throw new RepositorioException("Não consegui salvar o serviço \"" + servico.nome + "\".", e); }
    }

    @Override public void excluirServico(Catalogo.Servico servico) {
        if (servico.idServico == null) return;
        try (Connection con = ConexaoFabrica.abrir();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Serviço WHERE idServiço=? AND Usuario_idUsuario=?")) {
            ps.setInt(1, servico.idServico);
            ps.setInt(2, usuarioAtual.idUsuario());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RepositorioException("Não consegui excluir o serviço \"" + servico.nome + "\".", e); }
    }

    // ================= Itens (produtos + pacotes) =================

    @Override public List<Catalogo.Item> listarItens() {
        try (Connection con = ConexaoFabrica.abrir()) {
            Map<Integer, Catalogo.Categoria> categoriasPorId = new HashMap<>();
            for (Catalogo.Categoria c : listarCategorias()) categoriasPorId.put(c.idCategoria, c);
            Map<Integer, Catalogo.Tema> temasPorId = new HashMap<>();
            for (Catalogo.Tema t : listarTemas()) temasPorId.put(t.idTema, t);

            List<Catalogo.Item> produtos = listarProdutos(con, categoriasPorId);
            Map<Integer, Catalogo.Item> produtosPorId = new HashMap<>();
            for (Catalogo.Item p : produtos) produtosPorId.put(p.idProduto, p);

            List<Catalogo.Item> pacotes = listarPacotes(con, temasPorId, produtosPorId);

            List<Catalogo.Item> todos = new ArrayList<>(produtos);
            todos.addAll(pacotes);
            return todos;
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui listar o catálogo.", e);
        }
    }

    private List<Catalogo.Item> listarProdutos(Connection con, Map<Integer, Catalogo.Categoria> categoriasPorId) throws SQLException {
        String sql = "SELECT idProduto, nome, `descrição`, cor, valor, quantidade, ativo, indisponivel, Categoria_idCategoria "
                + "FROM Produto WHERE Usuario_idUsuario = ?";
        List<Catalogo.Item> r = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Catalogo.Item i = new Catalogo.Item(rs.getString("nome"), Catalogo.Tipo.PRODUTO,
                            rs.getDouble("valor"), rs.getInt("quantidade"));
                    i.descricao = rs.getString("descrição");
                    i.cor = rs.getString("cor");
                    i.ativo = rs.getBoolean("ativo");
                    i.indisponivel = rs.getInt("indisponivel");
                    i.categoria = categoriasPorId.get(rs.getInt("Categoria_idCategoria"));
                    i.idProduto = rs.getInt("idProduto");
                    r.add(i);
                }
            }
        }
        return r;
    }

    private List<Catalogo.Item> listarPacotes(Connection con, Map<Integer, Catalogo.Tema> temasPorId,
                                              Map<Integer, Catalogo.Item> produtosPorId) throws SQLException {
        List<Catalogo.Item> r = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT idPacote, nome, `descrição`, `preço`, disponivel FROM Pacote");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Catalogo.Item i = new Catalogo.Item(rs.getString("nome"), Catalogo.Tipo.PACOTE, rs.getDouble("preço"), 0);
                i.descricao = rs.getString("descrição");
                i.ativo = rs.getBoolean("disponivel");
                i.idPacote = rs.getInt("idPacote");
                i.tema = buscarTemaDoPacote(con, i.idPacote, temasPorId);
                carregarComponentes(con, i, produtosPorId);
                r.add(i);
            }
        }
        return r;
    }

    private Catalogo.Tema buscarTemaDoPacote(Connection con, int idPacote, Map<Integer, Catalogo.Tema> temasPorId) throws SQLException {
        String sql = "SELECT Tema_idTema FROM Tema_has_Pacote WHERE Pacote_idPacote = ? AND Tema_Usuario_idUsuario = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPacote);
            ps.setInt(2, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? temasPorId.get(rs.getInt("Tema_idTema")) : null;
            }
        }
    }

    private void carregarComponentes(Connection con, Catalogo.Item pacote, Map<Integer, Catalogo.Item> produtosPorId) throws SQLException {
        String sql = "SELECT Produto_idProduto, quantidade FROM Pacote_has_Produto WHERE Pacote_idPacote = ? AND Produto_Usuario_idUsuario = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pacote.idPacote);
            ps.setInt(2, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Catalogo.Item produto = produtosPorId.get(rs.getInt("Produto_idProduto"));
                    if (produto != null) pacote.componentes.put(produto, rs.getInt("quantidade"));
                }
            }
        }
    }

    @Override public void salvarItem(Catalogo.Item item) {
        if (item.tipo == Catalogo.Tipo.PRODUTO) salvarProduto(item); else salvarPacote(item);
    }

    private void salvarProduto(Catalogo.Item item) {
        Integer idCategoria = item.categoria != null ? item.categoria.idCategoria : null;
        try (Connection con = ConexaoFabrica.abrir()) {
            if (item.idProduto == null) {
                String sql = "INSERT INTO Produto (nome, `descrição`, cor, valor, quantidade, Usuario_idUsuario, "
                        + "Categoria_idCategoria, Categoria_Usuario_idUsuario, ativo, indisponivel) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, item.nome);
                    ps.setString(2, item.descricao);
                    ps.setString(3, item.cor);
                    ps.setDouble(4, item.preco);
                    ps.setInt(5, item.estoque);
                    ps.setInt(6, usuarioAtual.idUsuario());
                    ps.setInt(7, idCategoria == null ? 0 : idCategoria);
                    ps.setInt(8, usuarioAtual.idUsuario());
                    ps.setBoolean(9, item.ativo);
                    ps.setInt(10, item.indisponivel);
                    ps.executeUpdate();
                    try (ResultSet g = ps.getGeneratedKeys()) { if (g.next()) item.idProduto = g.getInt(1); }
                }
            } else {
                String sql = "UPDATE Produto SET nome=?, `descrição`=?, cor=?, valor=?, quantidade=?, "
                        + "Categoria_idCategoria=?, Categoria_Usuario_idUsuario=?, ativo=?, indisponivel=? "
                        + "WHERE idProduto=? AND Usuario_idUsuario=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, item.nome);
                    ps.setString(2, item.descricao);
                    ps.setString(3, item.cor);
                    ps.setDouble(4, item.preco);
                    ps.setInt(5, item.estoque);
                    ps.setInt(6, idCategoria == null ? 0 : idCategoria);
                    ps.setInt(7, usuarioAtual.idUsuario());
                    ps.setBoolean(8, item.ativo);
                    ps.setInt(9, item.indisponivel);
                    ps.setInt(10, item.idProduto);
                    ps.setInt(11, usuarioAtual.idUsuario());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) { throw new RepositorioException("Não consegui salvar o produto \"" + item.nome + "\".", e); }
    }

    private void salvarPacote(Catalogo.Item item) {
        try (Connection con = ConexaoFabrica.abrir()) {
            if (item.idPacote == null) {
                String sql = "INSERT INTO Pacote (nome, `descrição`, `preço`, disponivel) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, item.nome);
                    ps.setString(2, item.descricao);
                    ps.setDouble(3, item.preco);
                    ps.setBoolean(4, item.ativo);
                    ps.executeUpdate();
                    try (ResultSet g = ps.getGeneratedKeys()) { if (g.next()) item.idPacote = g.getInt(1); }
                }
            } else {
                String sql = "UPDATE Pacote SET nome=?, `descrição`=?, `preço`=?, disponivel=? WHERE idPacote=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, item.nome);
                    ps.setString(2, item.descricao);
                    ps.setDouble(3, item.preco);
                    ps.setBoolean(4, item.ativo);
                    ps.setInt(5, item.idPacote);
                    ps.executeUpdate();
                }
            }
            salvarTemaDoPacote(con, item);
            salvarComponentes(con, item);
        } catch (SQLException e) { throw new RepositorioException("Não consegui salvar o pacote \"" + item.nome + "\".", e); }
    }

    private void salvarTemaDoPacote(Connection con, Catalogo.Item pacote) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM Tema_has_Pacote WHERE Pacote_idPacote = ?")) {
            del.setInt(1, pacote.idPacote);
            del.executeUpdate();
        }
        if (pacote.tema == null || pacote.tema.idTema == null) return;
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Tema_has_Pacote (Tema_idTema, Tema_Usuario_idUsuario, Pacote_idPacote) VALUES (?, ?, ?)")) {
            ps.setInt(1, pacote.tema.idTema);
            ps.setInt(2, usuarioAtual.idUsuario());
            ps.setInt(3, pacote.idPacote);
            ps.executeUpdate();
        }
    }

    private void salvarComponentes(Connection con, Catalogo.Item pacote) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM Pacote_has_Produto WHERE Pacote_idPacote = ?")) {
            del.setInt(1, pacote.idPacote);
            del.executeUpdate();
        }
        String sql = "INSERT INTO Pacote_has_Produto (Pacote_idPacote, Produto_idProduto, Produto_Usuario_idUsuario, "
                + "Produto_Categoria_idCategoria, Produto_Categoria_Usuario_idUsuario, quantidade) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Map.Entry<Catalogo.Item, Integer> e : pacote.componentes.entrySet()) {
                Catalogo.Item produto = e.getKey();
                if (produto.idProduto == null) continue;   // produto precisa estar salvo antes do pacote que o usa
                int idCategoria = produto.categoria != null && produto.categoria.idCategoria != null ? produto.categoria.idCategoria : 0;
                ps.setInt(1, pacote.idPacote);
                ps.setInt(2, produto.idProduto);
                ps.setInt(3, usuarioAtual.idUsuario());
                ps.setInt(4, idCategoria);
                ps.setInt(5, usuarioAtual.idUsuario());
                ps.setInt(6, e.getValue());
                ps.executeUpdate();
            }
        }
    }

    @Override public void excluirItem(Catalogo.Item item) {
        try (Connection con = ConexaoFabrica.abrir()) {
            if (item.tipo == Catalogo.Tipo.PRODUTO && item.idProduto != null) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM Produto WHERE idProduto=? AND Usuario_idUsuario=?")) {
                    ps.setInt(1, item.idProduto);
                    ps.setInt(2, usuarioAtual.idUsuario());
                    ps.executeUpdate();
                }
            } else if (item.tipo == Catalogo.Tipo.PACOTE && item.idPacote != null) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM Pacote WHERE idPacote=?")) {
                    ps.setInt(1, item.idPacote);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) { throw new RepositorioException("Não consegui excluir \"" + item.nome + "\".", e); }
    }
}
