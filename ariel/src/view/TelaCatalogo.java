package view;

import controller.CatalogoController;
import controller.PainelController;
import model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * VIEW: tela "Catálogo" (Tela 3 do protótipo), com quatro abas:
 *  Produtos | Pacotes | Temas e categorias | Serviços.
 * Cada aba tem a lista à esquerda e o detalhe/formulário à direita. "Excluir" segue o FA02 da documentação:
 * sem vínculos exclui; com vínculos desativa, preservando o histórico (desativado pode ser reativado).
 * As regras ficam no CatalogoController.
 */
public class TelaCatalogo extends JPanel {

    private static final Catalogo.Tema SEM_TEMA = new Catalogo.Tema("(sem tema)", "");

    private final TelaPrincipal app;
    private final PainelController ctrl;
    private final CatalogoController cat;
    private final TelaPrincipal.Campo busca = new TelaPrincipal.Campo("Buscar produto ou categoria", 12);
    private final JButton btnNovo = TelaPrincipal.botaoPrimario("+ Novo produto", 13);
    private final CardLayout cartas = new CardLayout();
    private final JPanel abas = new JPanel(cartas);
    private final JToggleButton[] botoes = new JToggleButton[4];
    private int abaAtual = 0;
    private boolean atualizando = false;

    private final AbaProdutos produtos = new AbaProdutos();
    private final AbaPacotes pacotes = new AbaPacotes();
    private final PainelSimples temas, categorias, servicos;

    public TelaCatalogo(TelaPrincipal app, PainelController ctrl, CatalogoController cat) {
        super(new BorderLayout());
        this.app = app;
        this.ctrl = ctrl;
        this.cat = cat;
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        temas = new PainelSimples("TEMAS", "Tema", false, false, new Cadastro() {
            public List<?> lista() { return Catalogo.TEMAS; }
            public String nome(Object o) { return ((Catalogo.Tema) o).nome; }
            public String descricao(Object o) { return ((Catalogo.Tema) o).descricao; }
            public boolean ativo(Object o) { return ((Catalogo.Tema) o).ativo; }
            public double valor(Object o) { return 0; }
            public Object salvar(Object ex, String n, String d, double v) { return cat.salvarTema((Catalogo.Tema) ex, n, d); }
        });
        categorias = new PainelSimples("CATEGORIAS", "Categoria", false, false, new Cadastro() {
            public List<?> lista() { return Catalogo.CATEGORIAS; }
            public String nome(Object o) { return ((Catalogo.Categoria) o).nome; }
            public String descricao(Object o) { return ((Catalogo.Categoria) o).descricao; }
            public boolean ativo(Object o) { return ((Catalogo.Categoria) o).ativo; }
            public double valor(Object o) { return 0; }
            public Object salvar(Object ex, String n, String d, double v) { return cat.salvarCategoria((Catalogo.Categoria) ex, n, d); }
        });
        servicos = new PainelSimples("SERVIÇOS", "Serviço", true, true, new Cadastro() {
            public List<?> lista() { return Catalogo.SERVICOS; }
            public String nome(Object o) { return ((Catalogo.Servico) o).nome; }
            public String descricao(Object o) { return ((Catalogo.Servico) o).descricao; }
            public boolean ativo(Object o) { return ((Catalogo.Servico) o).ativo; }
            public double valor(Object o) { return ((Catalogo.Servico) o).valor; }
            public Object salvar(Object ex, String n, String d, double v) { return cat.salvarServico((Catalogo.Servico) ex, n, d, v); }
        });

        // ----- cabeçalho -----
        JPanel cab = new JPanel(new BorderLayout(12, 6));
        cab.setOpaque(false);
        cab.add(TelaPrincipal.rotulo("Catálogo", Font.BOLD, 30), BorderLayout.WEST);
        JPanel dir = new JPanel(new Ui.WrapLayout(FlowLayout.RIGHT, 10, 4));
        dir.setOpaque(false);
        busca.setPreferredSize(new Dimension(230, Tema.ALTURA));
        busca.getDocument().addDocumentListener(Ui.ouvinte(this::atualizar));
        btnNovo.addActionListener(e -> novo());
        dir.add(busca);
        dir.add(btnNovo);
        cab.add(dir, BorderLayout.CENTER);

        // ----- abas -----
        String[] nomes = {"Produtos", "Pacotes", "Temas e categorias", "Serviços"};
        JPanel barra = new JPanel(new Ui.WrapLayout(FlowLayout.LEFT, 8, 6));
        barra.setOpaque(false);
        barra.setBorder(new EmptyBorder(10, -8, 10, 0));
        ButtonGroup g = new ButtonGroup();
        for (int i = 0; i < nomes.length; i++) {
            final int idx = i;
            botoes[i] = Ui.botaoEscolha(nomes[i]);
            botoes[i].addActionListener(e -> trocarAba(idx));
            g.add(botoes[i]);
            barra.add(botoes[i]);
        }
        botoes[0].setSelected(true);

        temas.setMinimumSize(new Dimension(260, 200));
        categorias.setMinimumSize(new Dimension(260, 200));
        JSplitPane temasCategorias = Ui.divisor(temas, categorias, 0.50);
        abas.setOpaque(false);
        abas.add(produtos, "0");
        abas.add(pacotes, "1");
        abas.add(temasCategorias, "2");
        abas.add(servicos, "3");

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.add(cab, BorderLayout.NORTH);
        topo.add(barra, BorderLayout.SOUTH);
        add(topo, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);
        atualizar();
    }

    /** Abre a aba Produtos com o produto selecionado (chamado quando falta estoque para uma retirada). */
    void abrirProduto(Catalogo.Item p) {
        if (p == null) return;
        botoes[0].setSelected(true);
        trocarAba(0);
        produtos.selecionar(p);
    }

    private void trocarAba(int i) {
        abaAtual = i;
        cartas.show(abas, String.valueOf(i));
        btnNovo.setVisible(i != 2);
        btnNovo.setText(i == 0 ? "+ Novo produto" : i == 1 ? "+ Novo pacote" : "+ Novo serviço");
        Container pai = btnNovo.getParent();
        if (pai != null) { pai.revalidate(); pai.repaint(); }
        busca.setText("");
        atualizar();
    }

    private void novo() {
        if (abaAtual == 0) produtos.novo();
        else if (abaAtual == 1) pacotes.novo();
        else if (abaAtual == 3) servicos.novo();
    }

    /** Recarrega as listas (chamado quando algo muda). */
    public void atualizar() {
        if (atualizando) return;
        atualizando = true;
        try {
            String q = busca.getText().trim().toLowerCase();
            produtos.recarregar(q);
            pacotes.recarregar(q);
            temas.recarregar(q);
            categorias.recarregar(q);
            servicos.recarregar(q);
        } finally {
            atualizando = false;
        }
    }

    // ====================================================================================
    //  Auxiliares de tela
    // ====================================================================================
    private static String situacao(boolean ativo) { return ativo ? "Ativo" : "Inativo"; }

    private static JPanel coluna(JComponent... itens) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        for (JComponent c : itens) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            boolean dinamico = c.getLayout() instanceof Ui.WrapLayout || c instanceof JLabel;
            if (!dinamico) c.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(c.getPreferredSize().height, 20)));
            p.add(c);
            p.add(Box.createVerticalStrut(8));
        }
        return p;
    }

    private static JPanel linhaDupla(JComponent a, JComponent b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 10, 0));
        p.setOpaque(false);
        p.add(a);
        p.add(b);
        return p;
    }

    private static TelaPrincipal.Campo campo(String dica) {
        TelaPrincipal.Campo c = new TelaPrincipal.Campo(dica, 13);
        c.setPreferredSize(new Dimension(100, Tema.ALTURA));
        return c;
    }

    private static JScrollPane rolagem(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.getViewport().setBackground(TelaPrincipal.BRANCO);
        return sp;
    }

    private static JScrollPane rolagemSemBorda(JComponent c) {
        JScrollPane sp = new JScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private static void texto(JLabel l, String t) { l.setText("<html>" + t.replace("\n", "<br>") + "</html>"); }

    private static JPanel botoesForm(JButton... bs) {
        JPanel p = new JPanel(new Ui.WrapLayout(FlowLayout.LEFT, 8, 6));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, -8, 0, 0));
        for (JButton b : bs) {
            b.setPreferredSize(new Dimension(112, Tema.ALTURA));
            p.add(b);
        }
        return p;
    }

    private static void atualizarBarraBotoes(JButton b) {
        Container pai = b.getParent();
        if (pai != null) { pai.revalidate(); pai.repaint(); }
    }

    /** Modelo de tabela genérico: uma lista e uma função que transforma o item numa linha. */
    private static class Modelo<T> extends AbstractTableModel {
        final List<T> dados = new ArrayList<>();
        final String[] cols;
        final Function<T, Object[]> linha;
        Modelo(String[] cols, Function<T, Object[]> linha) { this.cols = cols; this.linha = linha; }
        public int getRowCount() { return dados.size(); }
        public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) { return linha.apply(dados.get(r))[c]; }
    }

    // ====================================================================================
    //  Aba PRODUTOS (UC08)
    // ====================================================================================
    private class AbaProdutos extends JPanel {
        final Modelo<Catalogo.Item> modelo = new Modelo<>(new String[]{"Nome", "Categoria", "Valor", "Disp./Total", "Situação"},
                p -> new Object[]{p.nome, p.categoria == null ? "-" : p.categoria.nome, TelaPrincipal.moeda(p.preco),
                        cat.disponivelHoje(p) + "/" + p.estoque, situacao(p.ativo)});
        final JTable tabela = new JTable(modelo);
        final TelaPrincipal.Campo tfNome = campo("Nome"), tfCor = campo("Cor / material / personagem"),
                tfPreco = campo("Valor de locação (R$)"), tfTotal = campo("Quantidade total"), tfIndisp = campo("Quantidade indisponível");
        final JComboBox<Catalogo.Categoria> cbCategoria = new JComboBox<>();
        final JLabel info = TelaPrincipal.rotulo(" ", Font.PLAIN, 11), msg = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
        final JButton salvar = Ui.botaoPreto("Salvar", 13), inativar = TelaPrincipal.botaoCinza("Inativar", 13);
        Catalogo.Item sel;
        boolean carregando;

        AbaProdutos() {
            super(new BorderLayout());
            setOpaque(false);
            TelaClientes.estilizarTabela(tabela, 12, 6);
            int[] larg = {200, 120, 80, 80, 70};
            for (int i = 0; i < larg.length; i++) tabela.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
            tabela.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || carregando) return;
                int i = tabela.getSelectedRow();
                preencher(i >= 0 && i < modelo.dados.size() ? modelo.dados.get(i) : null);
            });
            JPanel lista = Ui.caixa("PRODUTOS", rolagem(tabela));
            lista.setMinimumSize(new Dimension(300, 200));

            TelaPrincipal.estilizarCombo(cbCategoria);
            cbCategoria.setPreferredSize(new Dimension(100, Tema.ALTURA));
            salvar.addActionListener(e -> salvar());
            inativar.addActionListener(e -> alternar());
            JPanel form = coluna(Ui.bloco("Nome", tfNome), Ui.bloco("Cor / Material / Personagem", tfCor),
                    linhaDupla(Ui.bloco("Valor (R$)", tfPreco), Ui.bloco("Categoria", cbCategoria)),
                    linhaDupla(Ui.bloco("Estoque total", tfTotal), Ui.bloco("Indisponíveis", tfIndisp)),
                    info, botoesForm(salvar, inativar), msg);
            JPanel lateral = Ui.caixa("DETALHE DO PRODUTO", rolagemSemBorda(form));
            lateral.setPreferredSize(new Dimension(350, 100));
            lateral.setMinimumSize(new Dimension(300, 200));
            add(Ui.divisor(lista, lateral, 0.58), BorderLayout.CENTER);
            preencher(null);
        }

        void recarregar(String q) {
            Catalogo.Item antes = sel;
            carregando = true;
            modelo.dados.clear();
            for (Catalogo.Item p : Catalogo.produtos())
                if (q.isEmpty() || (p.nome + " " + p.cor + " " + (p.categoria == null ? "" : p.categoria.nome)).toLowerCase().contains(q))
                    modelo.dados.add(p);
            modelo.fireTableDataChanged();
            int i = modelo.dados.indexOf(antes);
            if (i >= 0) tabela.setRowSelectionInterval(i, i);
            carregando = false;
            if (antes != null && i >= 0) rotuloBotoes(antes);
            categoriasAtivas();
        }

        /** Seleciona o produto na lista e mostra o detalhe (estoque total e indisponíveis). */
        void selecionar(Catalogo.Item p) {
            recarregar("");
            int i = modelo.dados.indexOf(p);
            if (i < 0) return;
            tabela.setRowSelectionInterval(i, i);                 // o listener preenche o detalhe
            tabela.scrollRectToVisible(tabela.getCellRect(i, 0, true));
            preencher(p);
        }

        void categoriasAtivas() {
            Object atual = cbCategoria.getSelectedItem();
            cbCategoria.removeAllItems();
            for (Catalogo.Categoria c : Catalogo.CATEGORIAS) if (c.ativo || (sel != null && sel.categoria == c)) cbCategoria.addItem(c);
            if (atual != null) cbCategoria.setSelectedItem(atual);
        }

        void novo() {
            carregando = true;
            tabela.clearSelection();
            carregando = false;
            preencher(null);
            tfNome.requestFocusInWindow();
        }

        void preencher(Catalogo.Item p) {
            sel = p;
            categoriasAtivas();
            msg.setText(" ");
            if (p == null) {
                tfNome.setText(""); tfCor.setText(""); tfPreco.setText(""); tfTotal.setText(""); tfIndisp.setText("0");
                if (cbCategoria.getItemCount() > 0) cbCategoria.setSelectedIndex(0);
                info.setText(" ");
                inativar.setVisible(false);
                atualizarBarraBotoes(inativar);
            } else {
                tfNome.setText(p.nome); tfCor.setText(p.cor); tfPreco.setText(Ui.numero(p.preco));
                tfTotal.setText(String.valueOf(p.estoque)); tfIndisp.setText(String.valueOf(p.indisponivel));
                cbCategoria.setSelectedItem(p.categoria);
                info.setText("Livres hoje: " + cat.disponivelHoje(p) + " · utilizáveis: " + p.estoqueUtil());
                rotuloBotoes(p);
            }
        }

        void rotuloBotoes(Catalogo.Item p) {
            inativar.setVisible(true);
            inativar.setText(p.ativo ? "Excluir" : "Reativar");
            atualizarBarraBotoes(inativar);
        }

        void salvar() {
            try {
                boolean novo = sel == null;
                Catalogo.Item p = cat.salvarProduto(sel, tfNome.getText(), tfCor.getText(), (Catalogo.Categoria) cbCategoria.getSelectedItem(),
                        TelaPrincipal.lerValor(tfPreco.getText()), inteiro(tfTotal.getText()), inteiro(tfIndisp.getText()));
                recarregar(busca.getText().trim().toLowerCase());
                int i = modelo.dados.indexOf(p);
                if (i >= 0) { carregando = true; tabela.setRowSelectionInterval(i, i); carregando = false; }
                preencher(p);
                texto(msg, novo ? Mensagens.MSG001 : Mensagens.MSG004);
            } catch (RegraNegocioException e) {
                texto(msg, e.getMessage());
            }
        }

        void alternar() {
            if (sel == null) return;
            if (sel.ativo) {                                         // UC08 FA02: excluir ou desativar
                if (!confirmarExclusao(this, sel.nome, cat.temVinculos(sel))) return;
                String m = cat.excluirOuDesativar(sel);
                Catalogo.Item antes = sel;
                recarregar(busca.getText().trim().toLowerCase());
                preencher(Catalogo.ITENS.contains(antes) ? antes : null);
                texto(msg, m);
                return;
            }
            cat.alternarAtivo(sel);
            preencher(sel);
            texto(msg, "Produto reativado. (Indisponibilidade por dano não muda sozinha.)");
        }
    }

    /** Confirmação do "Excluir": avisa se, por ter vínculos, o cadastro será desativado em vez de apagado. */
    private static boolean confirmarExclusao(Component pai, String nome, boolean vinculos) {
        String txt = "Excluir \"" + nome + "\"?\n" + (vinculos
                ? "Ele possui vínculos (orçamentos, locações ou outros cadastros): não será apagado, e sim DESATIVADO."
                : "Ele não possui vínculos: será excluído.");
        return JOptionPane.showOptionDialog(pai, txt, "Excluir cadastro", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Excluir", "Cancelar"}, "Cancelar") == 0;
    }

    private static int inteiro(String t) {
        try { return Integer.parseInt(t.trim()); } catch (NumberFormatException e) { return -1; }
    }

    // ====================================================================================
    //  Aba PACOTES (UC12)
    // ====================================================================================
    private class AbaPacotes extends JPanel {
        final Modelo<Catalogo.Item> modelo = new Modelo<>(new String[]{"Nome", "Tema", "Preço", "Disp.", "Situação"},
                p -> new Object[]{p.nome, p.tema == null ? "-" : p.tema.nome, TelaPrincipal.moeda(p.preco),
                        String.valueOf(cat.disponivelHoje(p)), situacao(p.ativo)});
        final JTable tabela = new JTable(modelo);
        final TelaPrincipal.Campo tfNome = campo("Nome do pacote"), tfDesc = campo("Descrição (opcional)"), tfPreco = campo("Preço do pacote (R$)"),
                tfQtd = campo("Qtd");
        final JComboBox<Catalogo.Tema> cbTema = new JComboBox<>();
        final JComboBox<Catalogo.Item> cbProduto = new JComboBox<>();
        final Map<Catalogo.Item, Integer> comp = new LinkedHashMap<>();
        final List<Catalogo.Item> compLista = new ArrayList<>();
        final AbstractTableModel modeloComp = new AbstractTableModel() {
            public int getRowCount() { return compLista.size(); }
            public int getColumnCount() { return 2; }
            @Override public String getColumnName(int c) { return c == 0 ? "Produto" : "Qtd"; }
            public Object getValueAt(int r, int c) { Catalogo.Item p = compLista.get(r); return c == 0 ? p.nome : comp.get(p); }
        };
        final JTable tabelaComp = new JTable(modeloComp);
        final JLabel msg = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
        final JButton salvar = Ui.botaoPreto("Salvar", 13), inativar = TelaPrincipal.botaoCinza("Inativar", 13);
        Catalogo.Item sel;
        boolean carregando;

        AbaPacotes() {
            super(new BorderLayout());
            setOpaque(false);
            TelaClientes.estilizarTabela(tabela, 12, 6);
            int[] larg = {200, 120, 80, 50, 70};
            for (int i = 0; i < larg.length; i++) tabela.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
            tabela.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || carregando) return;
                int i = tabela.getSelectedRow();
                preencher(i >= 0 && i < modelo.dados.size() ? modelo.dados.get(i) : null);
            });
            JLabel dica = TelaPrincipal.rotulo("<html>Pacotes usam o MESMO estoque dos produtos avulsos; cadastrar um pacote não reserva nada.</html>", Font.PLAIN, 11);
            dica.setForeground(TelaPrincipal.CINZA_TEXTO);
            JPanel esq = new JPanel(new BorderLayout(0, 6));
            esq.setOpaque(false);
            esq.add(rolagem(tabela), BorderLayout.CENTER);
            esq.add(dica, BorderLayout.SOUTH);
            JPanel lista = Ui.caixa("PACOTES", esq);
            lista.setMinimumSize(new Dimension(300, 200));

            TelaPrincipal.estilizarCombo(cbTema);
            TelaPrincipal.estilizarCombo(cbProduto);
            cbTema.setPreferredSize(new Dimension(100, Tema.ALTURA));
            cbProduto.setPreferredSize(new Dimension(100, Tema.ALTURA));
            TelaClientes.estilizarTabela(tabelaComp, 12, 4);
            JScrollPane spc = rolagem(tabelaComp);
            spc.setPreferredSize(new Dimension(100, 120));
            tfQtd.setPreferredSize(new Dimension(56, Tema.ALTURA));
            JButton add = TelaPrincipal.botaoCinza("+", 14), rem = TelaPrincipal.botaoCinza("−", 14);
            add.setPreferredSize(new Dimension(44, Tema.ALTURA));
            rem.setPreferredSize(new Dimension(44, Tema.ALTURA));
            add.setToolTipText("Adicionar o produto ao pacote");
            rem.setToolTipText("Remover o produto selecionado");
            add.addActionListener(e -> adicionarComp());
            rem.addActionListener(e -> {
                int i = tabelaComp.getSelectedRow();
                if (i >= 0 && i < compLista.size()) { comp.remove(compLista.remove(i)); modeloComp.fireTableDataChanged(); }
            });
            JPanel linhaAdd = new JPanel(new BorderLayout(6, 0));
            linhaAdd.setOpaque(false);
            linhaAdd.add(cbProduto, BorderLayout.CENTER);
            JPanel dirAdd = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            dirAdd.setOpaque(false);
            dirAdd.add(tfQtd);
            dirAdd.add(add);
            dirAdd.add(rem);
            linhaAdd.add(dirAdd, BorderLayout.EAST);
            salvar.addActionListener(e -> salvar());
            inativar.addActionListener(e -> alternar());
            JPanel form = coluna(Ui.bloco("Nome", tfNome), Ui.bloco("Descrição", tfDesc),
                    linhaDupla(Ui.bloco("Tema", cbTema), Ui.bloco("Preço (R$)", tfPreco)),
                    TelaPrincipal.rotulo("Composição (produtos e quantidades)", Font.BOLD, 11), spc, linhaAdd,
                    botoesForm(salvar, inativar), msg);
            JPanel lateral = Ui.caixa("DETALHE DO PACOTE", rolagemSemBorda(form));
            lateral.setPreferredSize(new Dimension(380, 100));
            lateral.setMinimumSize(new Dimension(330, 200));
            add(Ui.divisor(lista, lateral, 0.54), BorderLayout.CENTER);
            preencher(null);
        }

        void recarregar(String q) {
            Catalogo.Item antes = sel;
            carregando = true;
            modelo.dados.clear();
            for (Catalogo.Item p : Catalogo.pacotes())
                if (q.isEmpty() || (p.nome + " " + p.composicao() + " " + (p.tema == null ? "" : p.tema.nome)).toLowerCase().contains(q))
                    modelo.dados.add(p);
            modelo.fireTableDataChanged();
            int i = modelo.dados.indexOf(antes);
            if (i >= 0) tabela.setRowSelectionInterval(i, i);
            carregando = false;
            opcoes();
        }

        /** Atualiza as caixas de escolha (temas e produtos ativos). */
        void opcoes() {
            Object t = cbTema.getSelectedItem(), pr = cbProduto.getSelectedItem();
            cbTema.removeAllItems();
            cbTema.addItem(SEM_TEMA);
            for (Catalogo.Tema x : Catalogo.TEMAS) if (x.ativo || (sel != null && sel.tema == x)) cbTema.addItem(x);
            if (t != null) cbTema.setSelectedItem(t);
            cbProduto.removeAllItems();
            for (Catalogo.Item p : Catalogo.produtos()) if (p.ativo) cbProduto.addItem(p);
            if (pr != null) cbProduto.setSelectedItem(pr);
        }

        void novo() {
            carregando = true;
            tabela.clearSelection();
            carregando = false;
            preencher(null);
            tfNome.requestFocusInWindow();
        }

        void preencher(Catalogo.Item p) {
            sel = p;
            opcoes();
            msg.setText(" ");
            comp.clear();
            compLista.clear();
            tfQtd.setText("1");
            if (p == null) {
                tfNome.setText(""); tfDesc.setText(""); tfPreco.setText("");
                cbTema.setSelectedItem(SEM_TEMA);
                inativar.setVisible(false);
            } else {
                tfNome.setText(p.nome); tfDesc.setText(p.descricao); tfPreco.setText(Ui.numero(p.preco));
                cbTema.setSelectedItem(p.tema == null ? SEM_TEMA : p.tema);
                comp.putAll(p.componentes);
                compLista.addAll(p.componentes.keySet());
                inativar.setVisible(true);
                inativar.setText(p.ativo ? "Excluir" : "Reativar");
            }
            atualizarBarraBotoes(inativar);
            modeloComp.fireTableDataChanged();
        }

        void adicionarComp() {
            Catalogo.Item p = (Catalogo.Item) cbProduto.getSelectedItem();
            int q = inteiro(tfQtd.getText());
            if (p == null || q <= 0) { texto(msg, Mensagens.MSG011); return; }
            if (!comp.containsKey(p)) compLista.add(p);
            comp.merge(p, q, Integer::sum);
            modeloComp.fireTableDataChanged();
            msg.setText(" ");
        }

        void salvar() {
            try {
                boolean novo = sel == null;
                Catalogo.Tema t = (Catalogo.Tema) cbTema.getSelectedItem();
                Catalogo.Item p = cat.salvarPacote(sel, tfNome.getText(), tfDesc.getText(), t == SEM_TEMA ? null : t,
                        TelaPrincipal.lerValor(tfPreco.getText()), new LinkedHashMap<>(comp));
                recarregar(busca.getText().trim().toLowerCase());
                int i = modelo.dados.indexOf(p);
                if (i >= 0) { carregando = true; tabela.setRowSelectionInterval(i, i); carregando = false; }
                preencher(p);
                texto(msg, novo ? Mensagens.MSG001 : Mensagens.MSG004);
            } catch (RegraNegocioException e) {
                texto(msg, e.getMessage());
            }
        }

        void alternar() {
            if (sel == null) return;
            if (sel.ativo) {                                         // UC12: excluir ou desativar
                if (!confirmarExclusao(this, sel.nome, cat.temVinculos(sel))) return;
                String m = cat.excluirOuDesativar(sel);
                Catalogo.Item antes = sel;
                recarregar(busca.getText().trim().toLowerCase());
                preencher(Catalogo.ITENS.contains(antes) ? antes : null);
                texto(msg, m);
                return;
            }
            cat.alternarAtivo(sel);
            preencher(sel);
            texto(msg, "Pacote reativado.");
        }
    }

    // ====================================================================================
    //  Cadastros simples: temas, categorias e serviços (UC09, UC10, UC06)
    // ====================================================================================
    private interface Cadastro {
        List<?> lista();
        String nome(Object o);
        String descricao(Object o);
        boolean ativo(Object o);
        double valor(Object o);
        Object salvar(Object existente, String nome, String descricao, double valor);
    }

    private class PainelSimples extends JPanel {
        final Cadastro cad;
        final boolean temValor;
        final List<Object> dados = new ArrayList<>();
        final AbstractTableModel modelo;
        final JTable tabela;
        final TelaPrincipal.Campo tfNome, tfDesc, tfValor;
        final JLabel msg = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
        final JButton salvar = Ui.botaoPreto("Salvar", 13), inativar = TelaPrincipal.botaoCinza("Inativar", 13),
                novo = TelaPrincipal.botaoSuave("+ Novo", 13);
        Object sel;
        boolean carregando;

        PainelSimples(String titulo, String rotulo, boolean temValor, boolean ladoALado, Cadastro cad) {
            super(new BorderLayout(0, 10));
            this.cad = cad;
            this.temValor = temValor;
            setOpaque(false);
            tfNome = campo("Nome");
            tfDesc = campo("Descrição");
            tfValor = campo("Valor de referência (R$)");
            String[] cols = temValor ? new String[]{"Nome", "Descrição", "Valor", "Situação"} : new String[]{"Nome", "Descrição", "Situação"};
            modelo = new AbstractTableModel() {
                public int getRowCount() { return dados.size(); }
                public int getColumnCount() { return cols.length; }
                @Override public String getColumnName(int c) { return cols[c]; }
                public Object getValueAt(int r, int c) {
                    Object o = dados.get(r);
                    if (c == 0) return cad.nome(o);
                    if (c == 1) return cad.descricao(o);
                    if (temValor && c == 2) return TelaPrincipal.moeda(cad.valor(o));
                    return situacao(cad.ativo(o));
                }
            };
            tabela = new JTable(modelo);
            TelaClientes.estilizarTabela(tabela, 12, 6);
            tabela.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || carregando) return;
                int i = tabela.getSelectedRow();
                preencher(i >= 0 && i < dados.size() ? dados.get(i) : null);
            });
            novo.addActionListener(e -> novo());
            salvar.addActionListener(e -> salvar());
            inativar.addActionListener(e -> alternar());

            List<JComponent> form = new ArrayList<>();
            form.add(Ui.bloco(rotulo, tfNome));
            form.add(Ui.bloco("Descrição", tfDesc));
            if (temValor) form.add(Ui.bloco("Valor de referência (R$)", tfValor));
            form.add(ladoALado ? botoesForm(salvar, inativar) : botoesForm(novo, salvar, inativar));
            form.add(msg);
            JPanel f = coluna(form.toArray(new JComponent[0]));

            if (ladoALado) {
                JPanel lista = Ui.caixa(titulo, rolagem(tabela));
                lista.setMinimumSize(new Dimension(300, 200));
                JPanel lateral = Ui.caixa("DETALHE DO " + rotulo.toUpperCase(), rolagemSemBorda(f));
                lateral.setPreferredSize(new Dimension(350, 100));
                lateral.setMinimumSize(new Dimension(300, 200));
                add(Ui.divisor(lista, lateral, 0.58), BorderLayout.CENTER);
            } else {
                JScrollPane spTab = rolagem(tabela);
                spTab.setMinimumSize(new Dimension(200, 120));
                JPanel meio = new JPanel(new BorderLayout(0, 10));
                meio.setOpaque(false);
                meio.add(spTab, BorderLayout.CENTER);
                meio.add(f, BorderLayout.SOUTH);
                add(Ui.caixa(titulo, meio), BorderLayout.CENTER);
            }
            preencher(null);
        }

        void recarregar(String q) {
            Object antes = sel;
            carregando = true;
            dados.clear();
            for (Object o : cad.lista())
                if (q.isEmpty() || (cad.nome(o) + " " + cad.descricao(o)).toLowerCase().contains(q)) dados.add(o);
            modelo.fireTableDataChanged();
            int i = dados.indexOf(antes);
            if (i >= 0) tabela.setRowSelectionInterval(i, i);
            carregando = false;
        }

        void novo() {
            carregando = true;
            tabela.clearSelection();
            carregando = false;
            preencher(null);
            tfNome.requestFocusInWindow();
        }

        void preencher(Object o) {
            sel = o;
            msg.setText(" ");
            if (o == null) { tfNome.setText(""); tfDesc.setText(""); tfValor.setText(""); inativar.setVisible(false); }
            else {
                tfNome.setText(cad.nome(o)); tfDesc.setText(cad.descricao(o)); tfValor.setText(Ui.numero(cad.valor(o)));
                inativar.setVisible(true);
                inativar.setText(cad.ativo(o) ? "Excluir" : "Reativar");
            }
            atualizarBarraBotoes(inativar);
        }

        void salvar() {
            try {
                boolean ehNovo = sel == null;
                double v = temValor ? TelaPrincipal.lerValor(tfValor.getText()) : 0;
                if (temValor && tfValor.getText().trim().isEmpty()) v = 0;
                Object o = cad.salvar(sel, tfNome.getText(), tfDesc.getText(), v);
                recarregar(busca.getText().trim().toLowerCase());
                int i = dados.indexOf(o);
                if (i >= 0) { carregando = true; tabela.setRowSelectionInterval(i, i); carregando = false; }
                preencher(o);
                texto(msg, ehNovo ? Mensagens.MSG001 : Mensagens.MSG004);
            } catch (RegraNegocioException e) {
                texto(msg, e.getMessage());
            }
        }

        void alternar() {
            if (sel == null) return;
            if (cad.ativo(sel)) {                                    // UC06/UC09/UC10 FA02: excluir ou desativar
                if (!confirmarExclusao(this, cad.nome(sel), cat.temVinculos(sel))) return;
                String m = cat.excluirOuDesativar(sel);
                Object antes = sel;
                recarregar(busca.getText().trim().toLowerCase());
                preencher(cad.lista().contains(antes) ? antes : null);
                texto(msg, m);
                return;
            }
            cat.alternarAtivo(sel);
            preencher(sel);
            texto(msg, "Cadastro reativado.");
        }
    }
}
