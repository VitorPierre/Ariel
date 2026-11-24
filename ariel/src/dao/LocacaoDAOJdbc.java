package dao;

import model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação MySQL de LocacaoDAO, contra Orçamento/Locação/Pagamento do Ariel_corrigido_fks.sql
 * + as colunas de dao/sql/Ariel_alteracoes_dao.sql.
 *
 * Duas coisas que o schema obriga e o Java não separa:
 *  1) `Locação.Orçamento_idOrçamento` é NOT NULL: toda locação precisa de uma linha em `Orçamento`
 *     por trás, mesmo que na Locacao.java "orçamento" e "locação" sejam o MESMO objeto (o que muda
 *     é só a etapa). Este DAO mantém as duas linhas em sincronia a cada salvar().
 *  2) Usuario_idUsuario: mesmo caso do ClienteDAOJdbc/CatalogoDAOJdbc — vem do usuário logado,
 *     passado no construtor, porque Locacao.java não guarda essa informação.
 *
 * NÃO coberto ainda (sem tabela adequada no schema atual — ver Ariel_alteracoes_dao.sql e o chat
 * onde isso foi explicado):
 *  - itens da locação (Locacao.itens — quais produtos/pacotes e quantidades): não existe
 *    "Locação_has_Produto" no schema, só "Produto_has_Orçamento" (sem quantidade nem desconto).
 *  - serviços contratados (Locacao.servicos): "Orçamento_has_Serviço" existe mas não guarda o
 *    valor contratado (pode ser diferente do valor de catálogo) — persistir ali perderia esse dado.
 *  - documentos (Documento, BLOB), ocorrências (Ocorrencia), pessoas autorizadas por locação,
 *    e o controle de devolução parcial (resolvidos / devolucoesParciais).
 * Continuam funcionando normalmente em memória; só não vão (ainda) para o MySQL.
 */
public class LocacaoDAOJdbc implements LocacaoDAO {

    private final Usuario usuarioAtual;
    private final List<Cliente> clientesConhecidos;

    public LocacaoDAOJdbc(Usuario usuarioAtual) {
        this(usuarioAtual, null);
    }

    public LocacaoDAOJdbc(Usuario usuarioAtual, List<Cliente> clientesConhecidos) {
        if (usuarioAtual == null || usuarioAtual.idUsuario() == null)
            throw new IllegalArgumentException("usuarioAtual precisa estar logado e já salvo no banco (idUsuario != null).");
        this.usuarioAtual = usuarioAtual;
        this.clientesConhecidos = clientesConhecidos;
    }

    @Override public List<Locacao> listar() {
        String sql = "SELECT l.idLocação, l.valorFinal, l.observações, l.dataEvento, l.pessoaAutorizada, "
                + "l.Cliente_idCliente, l.etapa, l.validade, l.retirada, l.devolucao, l.devolvidoEm, "
                + "l.arquivada, l.ocultaNoPainel, l.retiradoPor, l.devolvidoPor, l.emissao, l.modalidade, "
                + "l.motivoArquivo, l.autorizadoInadimplentePor, l.contratoPendente "
                + "FROM Locação l WHERE l.Usuario_idUsuario = ?";
        List<Locacao> r = new ArrayList<>();
        try (Connection con = ConexaoFabrica.abrir(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) r.add(mapear(con, rs));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui listar as locações.", e);
        }
        return r;
    }

    private Locacao mapear(Connection con, ResultSet rs) throws SQLException {
        int idCliente = rs.getInt("Cliente_idCliente");
        Cliente cliente = buscarCliente(con, idCliente);
        LocalDate evento = toLocalDate(rs.getDate("dataEvento"));
        Locacao l = new Locacao(rs.getInt("idLocação"), cliente, Etapa.valueOf(rs.getString("etapa")), evento,
                toLocalDate(rs.getDate("validade")), toLocalDate(rs.getDate("retirada")),
                toLocalDate(rs.getDate("devolucao")), toLocalDate(rs.getDate("devolvidoEm")), new ArrayList<>());
        l.arquivada = rs.getBoolean("arquivada");
        l.ocultaNoPainel = rs.getBoolean("ocultaNoPainel");
        l.retiradoPor = rs.getString("retiradoPor");
        l.devolvidoPor = rs.getString("devolvidoPor");
        l.emissao = toLocalDate(rs.getDate("emissao"));
        l.modalidade = rs.getString("modalidade");
        l.motivoArquivo = rs.getString("motivoArquivo");
        l.autorizadoInadimplentePor = rs.getString("autorizadoInadimplentePor");
        l.contratoPendente = rs.getBoolean("contratoPendente");
        l.pagamentos.addAll(listarPagamentos(con, rs.getInt("idLocação"), idCliente));
        return l;
    }

    private Cliente buscarCliente(Connection con, int idCliente) throws SQLException {
        if (clientesConhecidos != null) {
            for (Cliente c : clientesConhecidos) {
                if (c.idCliente != null && c.idCliente == idCliente) return c;
            }
        }
        String sql = "SELECT idCliente, Nome, CPF, Inadiplente, rg, instagram, whatsapp, endereco, observacoes, ativo "
                + "FROM Cliente WHERE idCliente = ? AND Usuario_idUsuario = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
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
    }

    private List<Pagamento> listarPagamentos(Connection con, int idLocacao, int idCliente) throws SQLException {
        String sql = "SELECT tipo, valorPagamento, dataPagamento, forma FROM Pagamento "
                + "WHERE Locação_idLocação = ? AND Locação_Cliente_idCliente = ? AND Locação_Cliente_Usuario_idUsuario = ?";
        List<Pagamento> r = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idLocacao);
            ps.setInt(2, idCliente);
            ps.setInt(3, usuarioAtual.idUsuario());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    r.add(new Pagamento(toLocalDate(rs.getDate("dataPagamento")), rs.getDouble("valorPagamento"),
                            Pagamento.Forma.valueOf(rs.getString("forma")), Pagamento.Tipo.valueOf(rs.getString("tipo"))));
                }
            }
        }
        return r;
    }

    @Override public void salvar(Locacao locacao) {
        try (Connection con = ConexaoFabrica.abrir()) {
            int idOrcamento = salvarOrcamento(con, locacao);
            salvarLocacao(con, locacao, idOrcamento);
            salvarPagamentos(con, locacao);
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui salvar a locação " + locacao.codigo() + ".", e);
        }
    }

    /** Mantém a linha de `Orçamento` por trás da locação (ver nota na classe). Devolve o idOrçamento. */
    private int salvarOrcamento(Connection con, Locacao l) throws SQLException {
        Integer idCliente = l.cliente.idCliente;
        if (idCliente == null) throw new RepositorioException("Salve o cliente antes de salvar a locação \"" + l.codigo() + "\".");
        try (PreparedStatement existe = con.prepareStatement(
                "SELECT Orçamento_idOrçamento FROM Locação WHERE idLocação = ?")) {
            existe.setInt(1, l.id);
            try (ResultSet rs = existe.executeQuery()) {
                if (rs.next()) {
                    int idOrcamento = rs.getInt(1);
                    String upd = "UPDATE Orçamento SET entrada=?, dataEmissão=?, dataValidadeOrçamento=?, "
                            + "valorFinalOrçamento=?, desconto=? WHERE idOrçamento=? AND Cliente_idCliente=? AND Cliente_Usuario_idUsuario=?";
                    try (PreparedStatement ps = con.prepareStatement(upd)) {
                        preencherOrcamento(ps, l);
                        ps.setInt(6, idOrcamento);
                        ps.setInt(7, idCliente);
                        ps.setInt(8, usuarioAtual.idUsuario());
                        ps.executeUpdate();
                    }
                    return idOrcamento;
                }
            }
        }
        String ins = "INSERT INTO Orçamento (entrada, dataEmissão, dataValidadeOrçamento, valorFinalOrçamento, desconto, "
                + "Cliente_idCliente, Cliente_Usuario_idUsuario, Usuario_idUsuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            preencherOrcamento(ps, l);
            ps.setInt(6, idCliente);
            ps.setInt(7, usuarioAtual.idUsuario());
            ps.setInt(8, usuarioAtual.idUsuario());
            ps.executeUpdate();
            try (ResultSet g = ps.getGeneratedKeys()) { g.next(); return g.getInt(1); }
        }
    }

    private void preencherOrcamento(PreparedStatement ps, Locacao l) throws SQLException {
        double entrada = 0;
        for (Pagamento p : l.pagamentos) if (p.tipo() == Pagamento.Tipo.ENTRADA) entrada += p.valor();
        ps.setDouble(1, entrada);
        ps.setDate(2, toSqlDate(l.emissao));
        ps.setDate(3, toSqlDate(l.validade));
        ps.setDouble(4, l.totalProdutos() + l.totalServicos());
        ps.setDouble(5, l.totalDescontos());
    }

    private void salvarLocacao(Connection con, Locacao l, int idOrcamento) throws SQLException {
        Integer idCliente = l.cliente.idCliente;
        try (PreparedStatement existe = con.prepareStatement("SELECT 1 FROM Locação WHERE idLocação = ?")) {
            existe.setInt(1, l.id);
            boolean ja_existe;
            try (ResultSet rs = existe.executeQuery()) { ja_existe = rs.next(); }
            if (ja_existe) {
                String sql = "UPDATE Locação SET valorFinal=?, observações=?, dataEvento=?, pessoaAutorizada=?, "
                        + "etapa=?, validade=?, retirada=?, devolucao=?, devolvidoEm=?, arquivada=?, ocultaNoPainel=?, "
                        + "retiradoPor=?, devolvidoPor=?, emissao=?, modalidade=?, motivoArquivo=?, "
                        + "autorizadoInadimplentePor=?, contratoPendente=? WHERE idLocação=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    preencherLocacao(ps, l);
                    ps.setInt(19, l.id);
                    ps.executeUpdate();
                }
            } else {
                String sql = "INSERT INTO Locação (idLocação, valorFinal, observações, dataEvento, pessoaAutorizada, "
                        + "Cliente_idCliente, Cliente_Usuario_idUsuario, Orçamento_idOrçamento, Usuario_idUsuario, "
                        + "etapa, validade, retirada, devolucao, devolvidoEm, arquivada, ocultaNoPainel, retiradoPor, "
                        + "devolvidoPor, emissao, modalidade, motivoArquivo, autorizadoInadimplentePor, contratoPendente) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, l.id);
                    ps.setDouble(2, l.totalProdutos() + l.totalServicos() - l.totalDescontos());
                    ps.setString(3, "");
                    ps.setDate(4, toSqlDate(l.evento));
                    ps.setString(5, l.retiradoPor == null ? "" : l.retiradoPor);
                    ps.setInt(6, idCliente);
                    ps.setInt(7, usuarioAtual.idUsuario());
                    ps.setInt(8, idOrcamento);
                    ps.setInt(9, usuarioAtual.idUsuario());
                    ps.setString(10, l.etapa.name());
                    ps.setDate(11, toSqlDate(l.validade));
                    ps.setDate(12, toSqlDate(l.retirada));
                    ps.setDate(13, toSqlDate(l.devolucao));
                    ps.setDate(14, toSqlDate(l.devolvidoEm));
                    ps.setBoolean(15, l.arquivada);
                    ps.setBoolean(16, l.ocultaNoPainel);
                    ps.setString(17, l.retiradoPor);
                    ps.setString(18, l.devolvidoPor);
                    ps.setDate(19, toSqlDate(l.emissao));
                    ps.setString(20, l.modalidade);
                    ps.setString(21, l.motivoArquivo);
                    ps.setString(22, l.autorizadoInadimplentePor);
                    ps.setBoolean(23, l.contratoPendente);
                    ps.executeUpdate();
                }
            }
        }
    }

    private void preencherLocacao(PreparedStatement ps, Locacao l) throws SQLException {
        ps.setDouble(1, l.totalProdutos() + l.totalServicos() - l.totalDescontos());
        ps.setString(2, "");
        ps.setDate(3, toSqlDate(l.evento));
        ps.setString(4, l.retiradoPor == null ? "" : l.retiradoPor);
        ps.setString(5, l.etapa.name());
        ps.setDate(6, toSqlDate(l.validade));
        ps.setDate(7, toSqlDate(l.retirada));
        ps.setDate(8, toSqlDate(l.devolucao));
        ps.setDate(9, toSqlDate(l.devolvidoEm));
        ps.setBoolean(10, l.arquivada);
        ps.setBoolean(11, l.ocultaNoPainel);
        ps.setString(12, l.retiradoPor);
        ps.setString(13, l.devolvidoPor);
        ps.setDate(14, toSqlDate(l.emissao));
        ps.setString(15, l.modalidade);
        ps.setString(16, l.motivoArquivo);
        ps.setString(17, l.autorizadoInadimplentePor);
        ps.setBoolean(18, l.contratoPendente);
    }

    /** Substitui todos os pagamentos da locação (Pagamento é um record imutável: não tem "update", só recria). */
    private void salvarPagamentos(Connection con, Locacao l) throws SQLException {
        Integer idCliente = l.cliente.idCliente;
        try (PreparedStatement del = con.prepareStatement(
                "DELETE FROM Pagamento WHERE Locação_idLocação=? AND Locação_Cliente_idCliente=? AND Locação_Cliente_Usuario_idUsuario=?")) {
            del.setInt(1, l.id);
            del.setInt(2, idCliente);
            del.setInt(3, usuarioAtual.idUsuario());
            del.executeUpdate();
        }
        String ins = "INSERT INTO Pagamento (tipo, valorPagamento, Locação_idLocação, Locação_Cliente_idCliente, "
                + "Locação_Cliente_Usuario_idUsuario, dataPagamento, forma) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(ins)) {
            for (Pagamento p : l.pagamentos) {
                ps.setString(1, p.tipo().name());
                ps.setDouble(2, p.valor());
                ps.setInt(3, l.id);
                ps.setInt(4, idCliente);
                ps.setInt(5, usuarioAtual.idUsuario());
                ps.setDate(6, toSqlDate(p.data()));
                ps.setString(7, p.forma().name());
                ps.executeUpdate();
            }
        }
    }

    private static LocalDate toLocalDate(Date d) { return d == null ? null : d.toLocalDate(); }
    private static Date toSqlDate(LocalDate d) { return d == null ? null : Date.valueOf(d); }
}
