package view;

import controller.PainelController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * VIEW: tela "Orçamentos" (UC04). Tem duas "páginas":
 *  - LISTA: todos os orçamentos (em negociação, com reserva confirmada, expirados e cancelados);
 *  - FORMULÁRIO "Novo orçamento" (Tela 4 do protótipo): cliente e datas, itens da proposta
 *    (+ Produto, + Pacote, + Serviço), resumo, sinal de 10/20/50% (ou outro valor) e os botões
 *    Salvar rascunho, Registrar entrada e confirmar reserva, Arquivar orçamento.
 *
 * Regras (todas no PainelController): validade fixa de 30 dias; orçamento NÃO reserva estoque;
 * só o sinal registrado reserva; disponibilidade por período; cliente inadimplente exige autorização.
 */
public class TelaOrcamentos extends JPanel {

    private static final String[] FILTROS = {"Todos", "Em negociação", "Reserva confirmada", "Expirados", "Cancelados"};
    private static final String[] MODALIDADES = {"Retira", "Entrega", "Entrega + montagem"};
    private static final Color COR_CONFLITO = Tema.ERRO, FUNDO_CONFLITO = Tema.ERRO_BG;   // DESIGN.md 3.5 (erro)

    private final TelaPrincipal app;
    private final PainelController ctrl;

    // ----- lista -----
    private final List<Locacao> visiveis = new ArrayList<>();
    private final TelaPrincipal.Campo busca = new TelaPrincipal.Campo("Buscar por cliente ou número", 12);
    private final JComboBox<String> filtro = new JComboBox<>(FILTROS);
    private final ModeloLista modeloLista = new ModeloLista();
    private final JTable tabelaLista = new JTable(modeloLista);

    // ----- formulário -----
    private Locacao editando;                  // null = novo orçamento
    private final JLabel tituloForm = TelaPrincipal.rotulo("Novo orçamento", Font.BOLD, 30);
    private JComboBox<Cliente> cbCliente = new JComboBox<>();
    private final JComboBox<String> cbModalidade = new JComboBox<>(MODALIDADES);
    private final JLabel avisoCliente = TelaPrincipal.rotulo("Cliente inadimplente: nova locação exige autorização.", Font.BOLD, 13);
    private final TelaPrincipal.Campo tfRetirada = new TelaPrincipal.Campo("Retirada (dd/mm/aaaa)", 13);
    private final TelaPrincipal.Campo tfEvento = new TelaPrincipal.Campo("Evento (dd/mm/aaaa)", 13);
    private final TelaPrincipal.Campo tfDevolucao = new TelaPrincipal.Campo("Devolução (dd/mm/aaaa)", 13);
    private final List<Linha> linhas = new ArrayList<>();
    private final ModeloItens modeloItens = new ModeloItens();
    private final JTable tabelaItens = new JTable(modeloItens);
    private final JLabel lblSubtotal = TelaPrincipal.rotulo("R$ 0,00", Font.PLAIN, 13);
    private final JLabel lblDesconto = TelaPrincipal.rotulo("R$ 0,00", Font.PLAIN, 13);
    private final JLabel lblServicos = TelaPrincipal.rotulo("R$ 0,00", Font.PLAIN, 13);
    private final JLabel lblTotal = TelaPrincipal.rotulo("R$ 0,00", Font.BOLD, 26);
    private final JLabel lblValidade = TelaPrincipal.rotulo(" ", Font.PLAIN, 13);
    private final JLabel lblSinal = TelaPrincipal.rotulo(" ", Font.BOLD, 13);
    private final TelaPrincipal.Campo tfOutro = new TelaPrincipal.Campo("0,00", 13);
    private final JToggleButton[] bSinal = new JToggleButton[Regras.SINAIS_RAPIDOS.length];
    private final ButtonGroup grupoSinal = new ButtonGroup();
    private final JLabel msg = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final JLabel avisoEstoque = TelaPrincipal.rotulo(" ", Font.BOLD, 12);   // conflito de estoque, checado sozinho
    private final JButton btnConfirmar = Ui.botaoPreto("Registrar entrada e confirmar", 12);
    private final JButton btnArquivar = TelaPrincipal.botaoNeutro("Arquivar orçamento", 12);
    private final JButton btnRascunho = TelaPrincipal.botaoCinza("Salvar rascunho", 12);
    private double pctSinal = Regras.SINAL_PADRAO;     // 0 = valor digitado em "Outro valor"
    private boolean montando = false, retEditada = false, devEditada = false, ajustando = false;

    /** Linha do orçamento em montagem: produto/pacote (com desconto) ou serviço (com valor individual). */
    private static class Linha {
        Catalogo.Item item;
        Catalogo.Servico servico;
        int qtd = 1;
        double desconto;       // %
        double descValor = -1; // desconto em R$ na linha (>= 0 quando foi digitado em reais; -1 = vale o %)
        double valor;          // só serviço
        boolean auto;          // serviço lançado sozinho pela modalidade (Entrega / Entrega + montagem)
        double bruto() { return item != null ? item.preco * qtd : valor; }
        double subtotal() {
            if (descValor >= 0) return Math.max(0, bruto() - descValor);
            return bruto() * (1 - desconto / 100.0);
        }
        /** Desconto em % equivalente (o model guarda o desconto em %). */
        double pctEfetivo() { return bruto() <= 0 ? 0 : (1 - subtotal() / bruto()) * 100.0; }
    }

    public TelaOrcamentos(TelaPrincipal app, PainelController ctrl) {
        super(new CardLayout());
        this.app = app;
        this.ctrl = ctrl;
        setBackground(Tema.FUNDO);
        add(criarLista(), "lista");
        add(criarFormulario(), "form");
        ((CardLayout) getLayout()).show(this, "lista");
        atualizar();
    }

    private void mostrar(String pagina) { ((CardLayout) getLayout()).show(this, pagina); }

    // ====================================================================================
    //  LISTA
    // ====================================================================================
    private JComponent criarLista() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.FUNDO);
        p.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Orçamentos", Font.BOLD, 30), BorderLayout.WEST);
        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        dir.setOpaque(false);
        busca.setPreferredSize(new Dimension(240, Tema.ALTURA));
        busca.getDocument().addDocumentListener(Ui.ouvinte(this::atualizar));
        TelaPrincipal.estilizarCombo(filtro);
        filtro.setPreferredSize(new Dimension(170, Tema.ALTURA));
        filtro.addActionListener(e -> atualizar());
        JButton novo = TelaPrincipal.botaoPrimario("+ Novo orçamento", 13);
        novo.setPreferredSize(new Dimension(160, Tema.ALTURA));
        novo.addActionListener(e -> novo());
        dir.add(busca);
        dir.add(filtro);
        dir.add(novo);
        cab.add(dir, BorderLayout.EAST);
        p.add(cab, BorderLayout.NORTH);

        TelaClientes.estilizarTabela(tabelaLista, 12, 6);
        int[] larg = {60, 170, 90, 100, 100, 230};
        for (int i = 0; i < larg.length; i++) tabelaLista.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
        tabelaLista.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) abrirSelecionado();
            }
        });
        p.add(Ui.secao(Ui.rolagem(tabelaLista)), BorderLayout.CENTER);

        JButton abrir = TelaPrincipal.botaoCinza("Abrir / Editar", 12);
        JButton cancelar = TelaPrincipal.botaoNeutro("Cancelar orçamento", 12);
        abrir.setPreferredSize(new Dimension(140, Tema.ALTURA));
        cancelar.setPreferredSize(new Dimension(170, Tema.ALTURA));
        abrir.addActionListener(e -> abrirSelecionado());
        cancelar.addActionListener(e -> {
            Locacao l = selecionada();
            if (l != null) arquivarOrcamento(l);
        });
        JLabel dica = TelaPrincipal.rotulo("Duplo clique abre. Orçamento vale 30 dias e só reserva itens depois do sinal.", Font.PLAIN, 12);
        dica.setForeground(TelaPrincipal.CINZA_TEXTO);
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.setBorder(new EmptyBorder(10, 0, 0, 0));
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botoes.add(abrir);
        botoes.add(cancelar);
        rodape.add(dica, BorderLayout.WEST);
        rodape.add(botoes, BorderLayout.EAST);
        p.add(rodape, BorderLayout.SOUTH);
        return p;
    }

    private Locacao selecionada() {
        int i = tabelaLista.getSelectedRow();
        return i >= 0 && i < visiveis.size() ? visiveis.get(i) : null;
    }

    static String situacao(Locacao l) {
        if (l.arquivada) return l.motivoArquivo.isEmpty() ? "Arquivado" : l.motivoArquivo;
        if (l.temEntrada()) return "Reserva confirmada";
        return "Em negociação";
    }

    /** Recarrega a lista (mantém a seleção). */
    public void atualizar() {
        if (montando) return;
        Locacao antes = selecionada();
        String q = busca.getText().trim().toLowerCase();
        String f = (String) filtro.getSelectedItem();
        visiveis.clear();
        for (Locacao l : ctrl.locacoes) {
            if (l.etapa != Etapa.ORCAMENTO) continue;
            String s = situacao(l);
            if (f.equals("Em negociação") && !s.equals("Em negociação")) continue;
            if (f.equals("Reserva confirmada") && !s.equals("Reserva confirmada")) continue;
            if (f.equals("Expirados") && !s.equals("Expirado")) continue;
            if (f.equals("Cancelados") && !s.equals("Cancelado")) continue;
            if (!q.isEmpty() && !(l.codigo() + " " + l.cliente.nome).toLowerCase().contains(q)) continue;
            visiveis.add(l);
        }
        visiveis.sort((a, b) -> b.id - a.id);
        modeloLista.fireTableDataChanged();
        int i = visiveis.indexOf(antes);
        if (i >= 0) tabelaLista.setRowSelectionInterval(i, i);
    }

    private void abrirSelecionado() {
        Locacao l = selecionada();
        if (l == null) { app.aviso("Selecione um orçamento na lista."); return; }
        if (l.arquivada || l.etapa != Etapa.ORCAMENTO) app.abrirDetalhes(l);    // arquivado ou já retirado: só consulta
        else editar(l);                                                          // em negociação ou reserva confirmada (UC05 FA02)
    }

    private void arquivarOrcamento(Locacao l) {
        int r = JOptionPane.showOptionDialog(this, "Cancelar o orçamento " + l.codigo() + " · " + l.cliente.nome + "?\n"
                        + "Ele fica arquivado no histórico.", "Cancelar orçamento", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Cancelar orçamento", "Voltar"}, "Voltar");
        if (r != 0) return;
        try {
            ctrl.arquivar(l);
        } catch (RegraNegocioException e) {
            app.aviso(e.getMessage());
        }
    }

    class ModeloLista extends AbstractTableModel {
        private final String[] cols = {"Nº", "Cliente", "Evento", "Total", "Validade", "Situação"};
        public int getRowCount() { return visiveis.size(); }
        public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int i, int c) {
            Locacao l = visiveis.get(i);
            switch (c) {
                case 0:  return l.codigo();
                case 1:  return l.cliente.nome;
                case 2:  return Ui.data(l.evento);
                case 3:  return TelaPrincipal.moeda(l.totalItens());
                case 4:  return l.validade == null ? "-" : Ui.data(l.validade);
                default: {
                    String s = situacao(l);
                    if (s.equals("Em negociação")) return s + " · " + ctrl.aviso(l).toLowerCase();
                    return s;
                }
            }
        }
    }

    // ====================================================================================
    //  FORMULÁRIO (Tela 4 do protótipo)
    // ====================================================================================
    private JComponent criarFormulario() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Tema.FUNDO);
        p.setBorder(new EmptyBorder(24, 28, 24, 28));

        // cabeçalho: título + voltar + salvar rascunho
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.add(tituloForm, BorderLayout.WEST);
        JButton voltar = TelaPrincipal.botaoCinza("‹ Voltar à lista", 12);
        voltar.setPreferredSize(new Dimension(130, Tema.ALTURA));
        voltar.addActionListener(e -> mostrar("lista"));
        btnRascunho.setPreferredSize(new Dimension(140, Tema.ALTURA));
        btnRascunho.addActionListener(e -> salvarRascunho());
        JPanel bt = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bt.setOpaque(false);
        bt.add(voltar);
        bt.add(btnRascunho);
        cab.add(bt, BorderLayout.EAST);
        p.add(cab, BorderLayout.NORTH);

        JPanel corpo = new JPanel(new BorderLayout(20, 0));
        corpo.setOpaque(false);
        corpo.add(criarEsquerda(), BorderLayout.CENTER);
        corpo.add(criarResumo(), BorderLayout.EAST);
        p.add(corpo, BorderLayout.CENTER);

        msg.setBorder(new EmptyBorder(2, 2, 0, 0));
        msg.setForeground(Tema.ERRO);
        p.add(msg, BorderLayout.SOUTH);

        configurarCampos();
        return p;
    }

    private JComponent criarEsquerda() {
        JPanel esq = new JPanel(new BorderLayout(0, 20));
        esq.setOpaque(false);

        // --- cliente e datas ---
        TelaPrincipal.estilizarCombo(cbCliente);
        TelaPrincipal.estilizarCombo(cbModalidade);
        cbCliente.setPreferredSize(new Dimension(100, Tema.ALTURA));
        cbModalidade.setPreferredSize(new Dimension(190, Tema.ALTURA));
        avisoCliente.setOpaque(true);                       // aviso de erro (DESIGN.md 3.5)
        avisoCliente.setBackground(Tema.ERRO_BG);
        avisoCliente.setForeground(Tema.ERRO);
        avisoCliente.setBorder(new Tema.Arredondada(Tema.ERRO_BG, 1, Tema.RAIO_CONTROLE, 8, 12));
        avisoCliente.setVisible(false);
        JPanel blocoModalidade = Ui.bloco("Tipo de entrega", cbModalidade);
        blocoModalidade.setPreferredSize(new Dimension(210, blocoModalidade.getPreferredSize().height));
        JPanel linha1 = new JPanel(new BorderLayout(12, 0));
        linha1.setOpaque(false);
        linha1.add(Ui.bloco("Cliente", cbCliente), BorderLayout.CENTER);
        linha1.add(blocoModalidade, BorderLayout.EAST);
        JPanel datas = new JPanel(new GridLayout(1, 3, 12, 0));
        datas.setOpaque(false);
        for (TelaPrincipal.Campo c : new TelaPrincipal.Campo[]{tfRetirada, tfEvento, tfDevolucao}) c.setPreferredSize(new Dimension(100, Tema.ALTURA));
        datas.add(Ui.bloco("Retirada", tfRetirada));
        datas.add(Ui.bloco("Data do evento", tfEvento));
        datas.add(Ui.bloco("Devolução", tfDevolucao));
        JPanel dados = new JPanel();
        dados.setLayout(new BoxLayout(dados, BoxLayout.Y_AXIS));
        dados.setOpaque(false);
        for (JComponent c : new JComponent[]{linha1, avisoCliente, datas}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            dados.add(c);
            if (c != datas) dados.add(Box.createVerticalStrut(c == avisoCliente ? 0 : 12));
        }
        esq.add(Ui.caixa("CLIENTE E DATAS", dados), BorderLayout.NORTH);

        // --- itens da proposta ---
        JButton bProd = TelaPrincipal.botaoSuave("+ Produto", 12);     // suaves: ações repetidas da seção (DESIGN.md 6.1)
        JButton bPac = TelaPrincipal.botaoSuave("+ Pacote", 12);
        JButton bServ = TelaPrincipal.botaoSuave("+ Serviço", 12);
        JButton bRem = TelaPrincipal.botaoNeutro("Remover", 12);
        // A disponibilidade é verificada sozinha (coluna "Disponib." + aviso abaixo da tabela): não há botão manual.
        bProd.addActionListener(e -> escolherItem(Catalogo.Tipo.PRODUTO));
        bPac.addActionListener(e -> escolherItem(Catalogo.Tipo.PACOTE));
        bServ.addActionListener(e -> escolherServico());
        bRem.addActionListener(e -> remover());
        JPanel barra = new JPanel(new Ui.WrapLayout(FlowLayout.LEFT, 8, 6));
        barra.setOpaque(false);
        barra.setBorder(new EmptyBorder(0, -8, 0, 0));
        barra.add(bProd);
        barra.add(bPac);
        barra.add(bServ);
        barra.add(bRem);

        TelaClientes.estilizarTabela(tabelaItens, 12, 6);
        TableCellRenderer base = tabelaItens.getDefaultRenderer(Object.class);
        tabelaItens.setDefaultRenderer(Object.class, (t, v, sel, foco, lin, col) -> {
            Component c = base.getTableCellRendererComponent(t, v, sel, foco, lin, col);
            if (lin < linhas.size() && emConflito(linhas.get(lin))) {        // linha em conflito: vermelho
                c.setForeground(COR_CONFLITO);
                if (!sel) c.setBackground(FUNDO_CONFLITO);
                if (col == 5) c.setFont(c.getFont().deriveFont(Font.BOLD));
            }
            return c;
        });
        int[] larg = {180, 54, 96, 90, 100, 200};
        for (int i = 0; i < larg.length; i++) tabelaItens.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
        tabelaItens.setSurrendersFocusOnKeystroke(true);
        tabelaItens.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "remover");
        tabelaItens.getActionMap().put("remover", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { remover(); }
        });
        JScrollPane sp = Ui.rolagem(tabelaItens);
        JLabel dicaItens = TelaPrincipal.rotulo("Dois cliques na célula para editar: Qtde, Valor un. dos serviços e Desc. (10% ou R$ 50).", Font.PLAIN, 12);
        dicaItens.setForeground(Tema.TEXTO_SUAVE);
        JPanel topoItens = new JPanel(new BorderLayout(0, 6));
        topoItens.setOpaque(false);
        topoItens.add(barra, BorderLayout.NORTH);
        topoItens.add(dicaItens, BorderLayout.SOUTH);

        JPanel itens = new JPanel(new BorderLayout(0, 8));
        itens.setOpaque(false);
        itens.add(topoItens, BorderLayout.NORTH);
        itens.add(sp, BorderLayout.CENTER);
        avisoEstoque.setOpaque(true);                       // aviso na própria tela, abaixo da tabela (DESIGN.md 8.3)
        avisoEstoque.setBackground(Tema.ERRO_BG);
        avisoEstoque.setForeground(COR_CONFLITO);
        avisoEstoque.setBorder(new Tema.Arredondada(Tema.ERRO_BG, 1, Tema.RAIO_CONTROLE, 8, 12));
        avisoEstoque.setVisible(false);
        itens.add(avisoEstoque, BorderLayout.SOUTH);
        esq.add(Ui.caixa("ITENS DA PROPOSTA", itens), BorderLayout.CENTER);
        return esq;
    }

    private JComponent criarResumo() {
        JPanel r = new JPanel();
        r.setLayout(new BoxLayout(r, BoxLayout.Y_AXIS));
        r.setOpaque(false);
        r.add(linha("Subtotal", lblSubtotal));
        r.add(linha("Desconto manual", lblDesconto));
        r.add(linha("Serviços", lblServicos));
        r.add(Box.createVerticalStrut(8));
        lblTotal.setForeground(Tema.PRIMARIA_ESCURA);
        JComponent linhaTotal = linha("Total", lblTotal);
        linhaTotal.setBorder(new CompoundBorder(new javax.swing.border.MatteBorder(1, 0, 0, 0, Tema.BORDA_SECAO), new EmptyBorder(8, 0, 0, 0)));
        linhaTotal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        r.add(linhaTotal);
        r.add(Box.createVerticalStrut(4));
        lblValidade.setForeground(Tema.TEXTO_SUAVE);
        lblValidade.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(lblValidade);
        r.add(Box.createVerticalStrut(16));

        JLabel ent = TelaPrincipal.rotulo("ENTRADA (SINAL)", Font.BOLD, 12);
        ent.setFont(Tema.fonteSecao());
        ent.setForeground(TelaPrincipal.CINZA_TEXTO);
        ent.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(ent);
        r.add(Box.createVerticalStrut(8));
        JPanel pct = new JPanel(new GridLayout(1, 3, 8, 0));
        pct.setOpaque(false);
        pct.setAlignmentX(Component.LEFT_ALIGNMENT);
        pct.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTURA));
        for (int i = 0; i < Regras.SINAIS_RAPIDOS.length; i++) {
            final double v = Regras.SINAIS_RAPIDOS[i];
            JToggleButton b = Ui.botaoEscolha(Math.round(v * 100) + "%");
            b.setPreferredSize(new Dimension(62, Tema.ALTURA));
            b.addActionListener(e -> { pctSinal = v; tfOutro.setText(""); atualizarValores(); });
            grupoSinal.add(b);
            bSinal[i] = b;
            pct.add(b);
        }
        r.add(pct);
        r.add(Box.createVerticalStrut(10));
        JPanel blocoOutro = Ui.bloco("Outro valor (R$)", tfOutro);
        blocoOutro.setAlignmentX(Component.LEFT_ALIGNMENT);
        blocoOutro.setMaximumSize(new Dimension(Integer.MAX_VALUE, blocoOutro.getPreferredSize().height));
        tfOutro.getDocument().addDocumentListener(Ui.ouvinte(() -> {
            if (montando || ajustando) return;
            if (!tfOutro.getText().trim().isEmpty()) { pctSinal = 0; grupoSinal.clearSelection(); }
            atualizarValores();
        }));
        r.add(blocoOutro);
        r.add(Box.createVerticalStrut(10));
        lblSinal.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(lblSinal);
        JLabel nota = TelaPrincipal.rotulo("<html><div style='width:230px'>Os itens só são reservados após a entrada.</div></html>", Font.PLAIN, 12);
        nota.setForeground(TelaPrincipal.CINZA_TEXTO);
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(Box.createVerticalStrut(4));
        r.add(nota);
        r.add(Box.createVerticalStrut(12));

        btnConfirmar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnConfirmar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));     // botão principal da tela: 48 px
        btnConfirmar.setPreferredSize(new Dimension(250, 48));
        btnConfirmar.addActionListener(e -> confirmar());
        btnArquivar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnArquivar.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTURA));
        btnArquivar.setPreferredSize(new Dimension(250, Tema.ALTURA));
        btnArquivar.addActionListener(e -> arquivar());
        r.add(btnConfirmar);
        r.add(Box.createVerticalStrut(8));
        r.add(btnArquivar);

        JPanel envolve = new JPanel(new BorderLayout());
        envolve.setOpaque(false);
        envolve.setPreferredSize(new Dimension(300, 100));
        envolve.add(Ui.caixa("RESUMO", r), BorderLayout.NORTH);
        return envolve;
    }

    private static JComponent linha(String titulo, JLabel valor) {
        JPanel l = new JPanel(new BorderLayout());
        l.setOpaque(false);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        l.add(TelaPrincipal.rotulo(titulo, Font.PLAIN, 13), BorderLayout.WEST);
        l.add(valor, BorderLayout.EAST);
        return l;
    }

    private void configurarCampos() {
        for (TelaPrincipal.Campo c : new TelaPrincipal.Campo[]{tfRetirada, tfEvento, tfDevolucao}) Ui.mascaraData(c);
        tfEvento.getDocument().addDocumentListener(Ui.ouvinte(() -> {
            if (montando || ajustando) return;
            LocalDate ev = Ui.lerData(tfEvento);
            if (ev != null) {                         // retirada na véspera e devolução no dia seguinte, se a pessoa não mexeu
                ajustando = true;
                try {
                    if (!retEditada) tfRetirada.setText(Ui.data(ev.minusDays(1)));
                    if (!devEditada) tfDevolucao.setText(Ui.data(ev.plusDays(1)));
                } finally { ajustando = false; }
            }
            atualizarValores();
        }));
        tfRetirada.getDocument().addDocumentListener(Ui.ouvinte(() -> { if (!montando && !ajustando) retEditada = true; atualizarValores(); }));
        tfDevolucao.getDocument().addDocumentListener(Ui.ouvinte(() -> { if (!montando && !ajustando) devEditada = true; atualizarValores(); }));
        cbCliente.addActionListener(e -> atualizarValores());
        cbModalidade.addActionListener(e -> { if (!montando) { aplicarModalidade(); atualizarValores(); } });
    }

    /** Serviços que cada modalidade lança sozinha. "Retira" não cobra nada. */
    private static List<String> servicosDaModalidade(String modalidade) {
        if ("Entrega".equals(modalidade)) return List.of("Entrega");
        if ("Entrega + montagem".equals(modalidade)) return List.of("Entrega", "Montagem");
        return List.of();
    }

    /**
     * Ajusta os serviços da proposta à modalidade escolhida: lança os que faltam (valor de referência do catálogo,
     * editável com dois cliques) e tira só os que o próprio sistema lançou. Serviços incluídos à mão ficam.
     */
    private void aplicarModalidade() {
        List<String> quero = servicosDaModalidade((String) cbModalidade.getSelectedItem());
        linhas.removeIf(l -> l.auto && !quero.contains(l.servico.nome));
        List<String> faltando = new ArrayList<>();
        for (String nome : quero) {
            boolean jaTem = false;
            for (Linha l : linhas) if (l.servico != null && l.servico.nome.equals(nome)) jaTem = true;
            if (jaTem) continue;
            Catalogo.Servico s = null;
            for (Catalogo.Servico x : Catalogo.SERVICOS) if (x.nome.equals(nome) && x.ativo) s = x;
            if (s == null) { faltando.add(nome); continue; }
            Linha l = new Linha();
            l.servico = s; l.valor = s.valor; l.auto = true;
            linhas.add(l);
        }
        msg(faltando.isEmpty() ? " " : "Serviço \"" + String.join("\", \"", faltando)
                + "\" não está ativo no catálogo. Ative em Catálogo > Serviços ou inclua com \"+ Serviço\".");
    }

    // ----------------------------------------------------------------------------------
    //  Abrir o formulário
    // ----------------------------------------------------------------------------------
    /** Formulário em branco (botão "+ Novo orçamento"). */
    public void novo() {
        preparar(null);
        mostrar("form");
    }

    /** Formulário com um orçamento em negociação ou uma reserva confirmada ainda não retirada. */
    void editar(Locacao l) {
        preparar(l);
        mostrar("form");
    }

    private void preparar(Locacao l) {
        montando = true;
        editando = l;
        linhas.clear();
        msg.setText(" ");
        cbCliente.removeAllItems();
        for (Cliente c : ctrl.clientesAtivos()) cbCliente.addItem(c);              // inativos não recebem orçamento novo
        if (l != null && !l.cliente.ativo) cbCliente.addItem(l.cliente);          // mas o orçamento antigo ainda abre
        cbCliente.setEnabled(l == null);
        tfOutro.setText("");
        retEditada = devEditada = false;
        pctSinal = Regras.SINAL_PADRAO;
        for (int i = 0; i < bSinal.length; i++) bSinal[i].setSelected(Math.abs(Regras.SINAIS_RAPIDOS[i] - pctSinal) < 1e-9);
        if (l == null) {
            tituloForm.setText("Novo orçamento · Nº " + String.format("%03d", ctrl.proximoNumero()));
            if (cbCliente.getItemCount() > 0) cbCliente.setSelectedIndex(0);
            cbModalidade.setSelectedIndex(0);
            LocalDate ev = ctrl.hoje().plusDays(14);
            tfEvento.setText(Ui.data(ev));
            tfRetirada.setText(Ui.data(ev.minusDays(1)));
            tfDevolucao.setText(Ui.data(ev.plusDays(1)));
        } else {
            tituloForm.setText("Orçamento " + l.codigo() + " · " + l.cliente.nome);
            cbCliente.setSelectedItem(l.cliente);
            cbModalidade.setSelectedItem(l.modalidade);
            tfEvento.setText(Ui.data(l.evento));
            tfRetirada.setText(Ui.data(l.retirada));
            tfDevolucao.setText(Ui.data(l.devolucao));
            retEditada = devEditada = true;
            for (Catalogo.Linha li : l.itens) {
                Linha x = new Linha();
                x.item = li.item; x.qtd = li.qtd; x.desconto = li.desconto;
                linhas.add(x);
            }
            List<String> daModalidade = servicosDaModalidade(l.modalidade);
            for (ServicoContratado s : l.servicos) {
                Linha x = new Linha();
                x.servico = s.servico; x.valor = s.valor;
                x.auto = daModalidade.contains(s.servico.nome);
                linhas.add(x);
            }
        }
        btnArquivar.setText(l == null ? "Descartar orçamento" : "Arquivar orçamento");
        boolean confirmada = l != null && l.temEntrada();
        boolean admin = PainelController.podeRegistrarPagamento(app.usuario());
        if (confirmada) tituloForm.setText("Alterar reserva " + l.codigo() + " · " + l.cliente.nome);
        btnRascunho.setText(confirmada ? "Salvar alterações" : "Salvar rascunho");
        btnArquivar.setVisible(!confirmada);                     // reserva confirmada se cancela pelo painel (sinal retido)
        btnConfirmar.setVisible(!confirmada);
        btnConfirmar.setEnabled(admin);                          // UC07: só o Administrador registra o sinal
        btnConfirmar.setToolTipText(admin ? null : "Somente o Administrador registra o sinal. Salve o orçamento; "
                + "a confirmação fica pendente até ela registrar o pagamento.");
        for (JToggleButton b : bSinal) b.setEnabled(!confirmada);
        tfOutro.setEnabled(!confirmada);
        montando = false;
        atualizarValores();
    }

    // ----------------------------------------------------------------------------------
    //  Itens
    // ----------------------------------------------------------------------------------
    private record Opcao(Catalogo.Item item, String texto) {
        @Override public String toString() { return texto; }
    }

    /** "+ Produto" / "+ Pacote": escolhe o item e a quantidade. */
    private void escolherItem(Catalogo.Tipo tipo) {
        List<Opcao> ops = new ArrayList<>();
        for (Catalogo.Item i : Catalogo.ITENS) {
            if (i.tipo != tipo || !i.ativo) continue;
            ops.add(new Opcao(i, i.nome + "  ·  " + TelaPrincipal.moeda(i.preco) + "  ·  disp. " + disponivel(i)));
        }
        if (ops.isEmpty()) { msg("Não há " + (tipo == Catalogo.Tipo.PRODUTO ? "produtos" : "pacotes") + " ativos no catálogo."); return; }
        JComboBox<Opcao> cb = new JComboBox<>(ops.toArray(new Opcao[0]));
        TelaPrincipal.estilizarCombo(cb);
        TelaPrincipal.Campo qtd = new TelaPrincipal.Campo("Quantidade", 13);
        qtd.setText("1");
        qtd.setPreferredSize(new Dimension(300, Tema.ALTURA));
        JLabel det = TelaPrincipal.rotulo(" ", Font.PLAIN, 11);
        Runnable mostraDet = () -> {
            Catalogo.Item i = ((Opcao) cb.getSelectedItem()).item();
            det.setText("<html><div style='width:300px'>" + (i.tipo == Catalogo.Tipo.PACOTE ? "Inclui: " + i.composicao()
                    : "Avulso" + (i.cor.isEmpty() ? "" : " · " + i.cor))
                    + (disponivel(i) == 0 ? "<br><b style='color:#9B1C1C'>Sem unidades livres no período: entra na proposta como indisponível.</b>" : "")
                    + "</div></html>");
        };
        cb.addActionListener(e -> mostraDet.run());
        mostraDet.run();
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(TelaPrincipal.rotulo(tipo == Catalogo.Tipo.PRODUTO ? "Produto:" : "Pacote:", Font.PLAIN, 12));
        p.add(cb);
        p.add(det);
        p.add(TelaPrincipal.rotulo("Quantidade:", Font.PLAIN, 12));
        p.add(qtd);
        int r = JOptionPane.showOptionDialog(this, p, tipo == Catalogo.Tipo.PRODUTO ? "Adicionar produto" : "Adicionar pacote",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Adicionar", "Cancelar"}, "Adicionar");
        if (r != 0) return;
        int n;
        try { n = Integer.parseInt(qtd.getText().trim()); } catch (NumberFormatException e) { n = 0; }
        if (n <= 0) { msg(Mensagens.MSG011); return; }
        Catalogo.Item item = ((Opcao) cb.getSelectedItem()).item();
        Linha existente = null;
        for (Linha l : linhas) if (l.item == item) existente = l;
        if (existente != null) existente.qtd += n;
        else { Linha l = new Linha(); l.item = item; l.qtd = n; linhas.add(l); }
        msg(" ");
        atualizarValores();
    }

    /** "+ Serviço": escolhe o serviço e o valor individual. */
    private void escolherServico() {
        List<Catalogo.Servico> ativos = new ArrayList<>();
        for (Catalogo.Servico s : Catalogo.SERVICOS) if (s.ativo) ativos.add(s);
        if (ativos.isEmpty()) { msg("Não há serviços ativos. Cadastre em Catálogo > Serviços."); return; }
        JComboBox<Catalogo.Servico> cb = new JComboBox<>(ativos.toArray(new Catalogo.Servico[0]));
        TelaPrincipal.estilizarCombo(cb);
        TelaPrincipal.Campo valor = new TelaPrincipal.Campo("Valor", 13);
        valor.setPreferredSize(new Dimension(300, Tema.ALTURA));
        Runnable preenche = () -> valor.setText(Ui.numero(((Catalogo.Servico) cb.getSelectedItem()).valor));
        cb.addActionListener(e -> preenche.run());
        preenche.run();
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(TelaPrincipal.rotulo("Serviço:", Font.PLAIN, 12));
        p.add(cb);
        p.add(TelaPrincipal.rotulo("Valor nesta proposta (R$):", Font.PLAIN, 12));
        p.add(valor);
        int r = JOptionPane.showOptionDialog(this, p, "Adicionar serviço", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Adicionar", "Cancelar"}, "Adicionar");
        if (r != 0) return;
        Linha l = new Linha();
        l.servico = (Catalogo.Servico) cb.getSelectedItem();
        l.valor = Math.max(0, TelaPrincipal.lerValor(valor.getText()));
        linhas.add(l);
        msg(" ");
        atualizarValores();
    }

    private void remover() {
        pararEdicao();
        int i = tabelaItens.getSelectedRow();
        if (i < 0 || i >= linhas.size()) { msg("Selecione um item na lista da proposta."); return; }
        linhas.remove(i);
        atualizarValores();
    }

    private void pararEdicao() {
        if (tabelaItens.isEditing() && tabelaItens.getCellEditor() != null) tabelaItens.getCellEditor().stopCellEditing();
    }

    // ----------------------------------------------------------------------------------
    //  Disponibilidade (UC15) para o formulário
    // ----------------------------------------------------------------------------------
    private LocalDate[] periodo() {
        LocalDate ev = Ui.lerData(tfEvento), r = Ui.lerData(tfRetirada), d = Ui.lerData(tfDevolucao);
        if (ev == null) ev = ctrl.hoje();
        if (r == null) r = ev;
        if (d == null) d = ev;
        if (d.isBefore(r)) d = r;
        return new LocalDate[]{r, d};
    }

    private int consumo(Catalogo.Item produto) {
        int s = 0;
        for (Linha l : linhas) if (l.item != null) s += l.qtd * l.item.consumo(produto);
        return s;
    }

    /** Unidades livres do produto no período (reservas do painel e o que já está nesta proposta descontados). */
    private int livre(Catalogo.Item produto) {
        LocalDate[] p = periodo();
        return ctrl.livre(produto, p[0], p[1], editando) - consumo(produto);
    }

    /** Quantas unidades do item ainda cabem nesta proposta. */
    private int disponivel(Catalogo.Item item) {
        int menor = Integer.MAX_VALUE;
        for (Catalogo.Item p : item.produtos()) menor = Math.min(menor, livre(p) / Math.max(1, item.consumo(p)));
        return Math.max(0, menor == Integer.MAX_VALUE ? 0 : menor);
    }

    private boolean emConflito(Linha l) {
        if (l.item == null) return false;
        for (Catalogo.Item p : l.item.produtos()) if (livre(p) < 0) return true;
        return false;
    }

    private List<String> conflitos() {
        List<String> r = new ArrayList<>();
        for (Linha l : linhas)
            if (emConflito(l)) {
                for (Catalogo.Item p : l.item.produtos()) if (livre(p) < 0 && !r.contains(p.nome)) r.add(p.nome);
            }
        return r;
    }

    /** Quantas unidades do item faltam no período (0 = sem conflito). */
    private int faltam(Linha l) {
        if (l.item == null) return 0;
        int f = 0;
        for (Catalogo.Item p : l.item.produtos()) {
            int livre = livre(p), c = Math.max(1, l.item.consumo(p));
            if (livre < 0) f = Math.max(f, (-livre + c - 1) / c);
        }
        return Math.min(f, l.qtd);
    }

    // ----------------------------------------------------------------------------------
    //  Valores
    // ----------------------------------------------------------------------------------
    private double total() {
        double t = 0;
        for (Linha l : linhas) t += l.subtotal();
        return t;
    }

    /** Sinal escolhido (atalho % ou valor digitado). */
    private double sinal() {
        if (pctSinal > 0) return Math.round(total() * pctSinal * 100) / 100.0;
        return Math.max(0, TelaPrincipal.lerValor(tfOutro.getText()));
    }

    private void atualizarValores() {
        if (montando) return;
        double bruto = 0, desc = 0, serv = 0;
        for (Linha l : linhas) {
            if (l.item != null) bruto += l.bruto();
            else serv += l.bruto();                                // serviços pelo valor cheio...
            desc += l.bruto() - l.subtotal();                        // ...e o desconto (produto ou serviço) aparece aqui
        }
        lblSubtotal.setText(TelaPrincipal.moeda(bruto));
        lblDesconto.setText(TelaPrincipal.moeda(desc));
        lblServicos.setText(TelaPrincipal.moeda(serv));
        lblTotal.setText(TelaPrincipal.moeda(total()));
        LocalDate validade = editando != null && editando.validade != null ? editando.validade
                : ctrl.hoje().plusDays(Regras.DIAS_VALIDADE_ORCAMENTO);
        lblValidade.setText("Validade: " + Regras.DIAS_VALIDADE_ORCAMENTO + " dias (" + TelaPrincipal.FMT_CURTA.format(validade) + ")");
        double s = sinal();
        if (editando != null && editando.temEntrada()) {          // reserva confirmada: mostra o sinal já pago
            double pago = editando.pagoItens();
            lblSinal.setText("<html><div style='width:240px'>Sinal/pagamentos já registrados: " + TelaPrincipal.moeda(pago)
                    + "<br>Falta: " + TelaPrincipal.moeda(Math.max(0, total() - pago)) + "</div></html>");
        } else {
            boolean abaixo = total() > 0.005 && s + 0.005 < Math.ceil(total() * Regras.SINAL_MINIMO * 100 - 1e-9) / 100.0;
            lblSinal.setText("<html><div style='width:240px'>Sinal: " + TelaPrincipal.moeda(s)
                    + (total() > 0.005 ? "  (" + Math.round(s / total() * 100) + "% do total)" : "")
                    + (abaixo ? "<br><span style='color:#9B1C1C'>Abaixo do mínimo de 10%.</span>" : "") + "</div></html>");
        }
        Cliente c = (Cliente) cbCliente.getSelectedItem();
        avisoCliente.setVisible(c != null && ctrl.inadimplente(c));
        List<String> cf = conflitos();                        // UC15: verificado sozinho a cada mudança
        avisoEstoque.setText(cf.isEmpty() ? " " : "<html>Sem estoque no período para: " + String.join(", ", cf)
                + ". Ajuste a quantidade, as datas ou remova o item — a reserva só é confirmada sem conflito.</html>");
        avisoEstoque.setVisible(!cf.isEmpty());
        int sel = tabelaItens.getSelectedRow();
        modeloItens.fireTableDataChanged();
        if (sel >= 0 && sel < linhas.size()) tabelaItens.setRowSelectionInterval(sel, sel);
    }

    private void msg(String t) { msg.setText("<html>" + t.replace("\n", "<br>") + "</html>"); }

    // ----------------------------------------------------------------------------------
    //  Ações dos botões
    // ----------------------------------------------------------------------------------
    private List<Catalogo.Linha> itensDoForm() {
        List<Catalogo.Linha> r = new ArrayList<>();
        for (Linha l : linhas) if (l.item != null) {
            Catalogo.Linha x = new Catalogo.Linha(l.item, l.qtd);
            x.desconto = l.pctEfetivo();
            r.add(x);
        }
        return r;
    }

    private List<ServicoContratado> servicosDoForm() {
        List<ServicoContratado> r = new ArrayList<>();
        for (Linha l : linhas) if (l.servico != null) r.add(new ServicoContratado(l.servico, l.subtotal()));   // preço efetivo contratado (UC06), já com desconto
        return r;
    }

    private void salvarRascunho() {
        pararEdicao();
        try {
            Locacao l = editando;
            if (l == null) {
                l = ctrl.criarOrcamento((Cliente) cbCliente.getSelectedItem(), Ui.lerData(tfEvento), Ui.lerData(tfRetirada),
                        Ui.lerData(tfDevolucao), (String) cbModalidade.getSelectedItem(), itensDoForm(), servicosDoForm(),
                        0, null, app.usuario(), null);
            } else {
                ctrl.atualizarOrcamento(l, Ui.lerData(tfEvento), Ui.lerData(tfRetirada), Ui.lerData(tfDevolucao),
                        (String) cbModalidade.getSelectedItem(), itensDoForm(), servicosDoForm());
            }
            if (l.temEntrada())
                app.aviso(Mensagens.MSG004 + "\nReserva " + l.codigo() + " · " + l.cliente.nome + " alterada; itens reservados de "
                        + Ui.data(l.retirada) + " a " + Ui.data(l.devolucao) + ".\n"
                        + (l.contratoPendente ? Mensagens.MSG016 : "Nova versão do contrato gerada (veja em Locações)."));
            else
                app.aviso(Mensagens.MSG001 + "\nOrçamento " + l.codigo() + " · " + l.cliente.nome + "\nVálido até "
                        + Ui.data(l.validade) + ". Ainda NÃO reserva itens: só o sinal reserva."
                        + (PainelController.podeRegistrarPagamento(app.usuario()) ? ""
                        : "\nA confirmação fica pendente até o Administrador registrar o sinal."));
            mostrar("lista");
            atualizar();
        } catch (RegraNegocioException e) {
            msg(e.getMessage());
        }
    }

    /** "Registrar entrada e confirmar reserva": sinal + disponibilidade + contrato (tudo validado no controller). */
    private void confirmar() {
        pararEdicao();
        double entrada = sinal();
        if (entrada <= 0.005) { msg(Mensagens.MSG014); return; }
        JComboBox<Pagamento.Forma> forma = new JComboBox<>(Pagamento.Forma.values());
        TelaPrincipal.estilizarCombo(forma);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(TelaPrincipal.rotulo("Registrar o sinal de " + TelaPrincipal.moeda(entrada) + " e confirmar a reserva?", Font.BOLD, 13));
        p.add(TelaPrincipal.rotulo("Total da proposta: " + TelaPrincipal.moeda(total()), Font.PLAIN, 12));
        p.add(TelaPrincipal.rotulo("Forma de pagamento:", Font.PLAIN, 12));
        p.add(forma);
        for (String a : ctrl.avisosDeAtraso(itensDoForm(), Ui.lerData(tfRetirada), editando))   // avisa, não bloqueia
            p.add(TelaPrincipal.rotulo("<html><div style='width:360px'>Atenção: " + a + ". A disponibilidade conta que volte a tempo.</div></html>", Font.BOLD, 11));
        int r = JOptionPane.showOptionDialog(this, p, "Confirmar reserva", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Registrar entrada", "Cancelar"}, "Registrar entrada");
        if (r != 0) return;
        Pagamento.Forma f = (Pagamento.Forma) forma.getSelectedItem();
        try {
            Locacao l = editando;
            try {
                l = gravarConfirmando(entrada, f, null);
            } catch (ExigeAutorizacaoException e) {                       // inadimplente: autorização registrada (decisão da equipe)
                Cliente c = (Cliente) cbCliente.getSelectedItem();
                Locacao ref = l != null ? l : new Locacao(0, c, Etapa.ORCAMENTO, LocalDate.now(), null, null, null, null, new ArrayList<>());
                if (!app.pedirAutorizacao(ref, e.getMessage())) return;
                l = gravarConfirmando(entrada, f, app.usuario());
            }
            app.aviso("Reserva confirmada! " + l.codigo() + " · " + l.cliente.nome + "\nSinal de " + TelaPrincipal.moeda(entrada)
                    + " registrado; os itens ficam reservados de " + Ui.data(l.retirada) + " a " + Ui.data(l.devolucao) + ".\n"
                    + (l.contratoPendente ? Mensagens.MSG016 : "Contrato gerado em PDF (veja em Locações)."));
            mostrar("lista");
            atualizar();
        } catch (RegraNegocioException e) {
            msg(e.getMessage());
        }
    }

    private Locacao gravarConfirmando(double entrada, Pagamento.Forma f, Usuario autoriza) {
        if (editando == null) {
            return ctrl.criarOrcamento((Cliente) cbCliente.getSelectedItem(), Ui.lerData(tfEvento), Ui.lerData(tfRetirada),
                    Ui.lerData(tfDevolucao), (String) cbModalidade.getSelectedItem(), itensDoForm(), servicosDoForm(),
                    entrada, f, app.usuario(), autoriza);
        }
        ctrl.atualizarOrcamento(editando, Ui.lerData(tfEvento), Ui.lerData(tfRetirada), Ui.lerData(tfDevolucao),
                (String) cbModalidade.getSelectedItem(), itensDoForm(), servicosDoForm());
        ctrl.pagar(editando, entrada, f, app.usuario(), autoriza);
        return editando;
    }

    private void arquivar() {
        pararEdicao();
        if (editando != null) {
            arquivarOrcamento(editando);
            if (editando.arquivada) { mostrar("lista"); atualizar(); }
            return;
        }
        int r = JOptionPane.showOptionDialog(this, "Descartar este orçamento? Nada foi salvo ainda.", "Descartar orçamento",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Descartar", "Voltar"}, "Voltar");
        if (r == 0) mostrar("lista");
    }

    // ----------------------------------------------------------------------------------
    //  Tabela de itens
    // ----------------------------------------------------------------------------------
    class ModeloItens extends AbstractTableModel {
        private final String[] cols = {"Item", "Qtde", "Valor un.", "Desc.", "Subtotal", "Disponib."};
        public int getRowCount() { return linhas.size(); }
        public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public boolean isCellEditable(int r, int c) {
            Linha l = linhas.get(r);
            return l.item != null ? (c == 1 || c == 3) : (c == 2 || c == 3);   // serviço: valor e desconto
        }

        public Object getValueAt(int i, int c) {
            Linha l = linhas.get(i);
            boolean item = l.item != null;
            switch (c) {
                case 0:  return item ? l.item.nome : "Serviço: " + l.servico.nome + (l.auto ? "  (pela modalidade)" : "");
                case 1:  return item ? String.valueOf(l.qtd) : "1";
                case 2:  return TelaPrincipal.moeda(item ? l.item.preco : l.valor);
                case 3:  if (l.descValor >= 0) return TelaPrincipal.moeda(Math.min(l.descValor, l.bruto()));
                         return (l.desconto == Math.rint(l.desconto) ? String.valueOf((int) l.desconto)
                                 : String.format(TelaPrincipal.PT_BR, "%.2f", l.desconto)) + "%";
                case 4:  return TelaPrincipal.moeda(l.subtotal());
                default: return !item ? "-" : (emConflito(l) ? "Indisponível (faltam " + faltam(l) + ")" : "OK");
            }
        }

        @Override public void setValueAt(Object v, int i, int c) {
            Linha l = linhas.get(i);
            String bruto = String.valueOf(v).replace('\u00a0', ' ').trim();
            String t = bruto.replace("%", "").trim();
            if (l.item != null && c == 1) {
                try { int n = Integer.parseInt(t); if (n > 0) l.qtd = n; } catch (NumberFormatException e) { /* ignora */ }
            } else if (c == 3) {                                   // produto, pacote ou serviço
                // Desconto em % ("10" ou "10%") ou em reais ("R$ 50" ou "R$ 50,00")
                double d = TelaPrincipal.lerValor(t);
                if (bruto.toUpperCase().contains("R$")) {
                    if (d >= 0) {
                        if (d > l.bruto() + 0.005) msg("O desconto não pode passar do valor do item (" + TelaPrincipal.moeda(l.bruto()) + ").");
                        else { l.descValor = d; l.desconto = 0; msg(" "); }
                    }
                } else if (d >= 0 && d <= 100) { l.desconto = d; l.descValor = -1; msg(" "); }
                else msg("Desconto em % vai de 0 a 100. Para desconto em reais, digite com R$ (ex.: R$ 50).");
            } else if (l.item == null && c == 2) {
                l.valor = Math.max(0, TelaPrincipal.lerValor(t));
                msg(" ");
            }
            atualizarValores();
        }
    }
}
