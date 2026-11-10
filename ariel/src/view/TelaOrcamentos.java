package view;

import controller.PainelController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
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
    private final JLabel tituloForm = TelaPrincipal.rotulo("Novo orçamento", Font.BOLD, 20);
    private JComboBox<Cliente> cbCliente = new JComboBox<>();
    private final JComboBox<String> cbModalidade = new JComboBox<>(MODALIDADES);
    private final JLabel avisoCliente = TelaPrincipal.rotulo(" ALERTA: cliente inadimplente (nova locação exige autorização)", Font.BOLD, 12);
    private final TelaPrincipal.Campo tfRetirada = new TelaPrincipal.Campo("Retirada (dd/mm/aaaa)", 13);
    private final TelaPrincipal.Campo tfEvento = new TelaPrincipal.Campo("Evento (dd/mm/aaaa)", 13);
    private final TelaPrincipal.Campo tfDevolucao = new TelaPrincipal.Campo("Devolução (dd/mm/aaaa)", 13);
    private final List<Linha> linhas = new ArrayList<>();
    private final ModeloItens modeloItens = new ModeloItens();
    private final JTable tabelaItens = new JTable(modeloItens);
    private final JLabel lblSubtotal = TelaPrincipal.rotulo("R$ 0,00", Font.PLAIN, 13);
    private final JLabel lblDesconto = TelaPrincipal.rotulo("R$ 0,00", Font.PLAIN, 13);
    private final JLabel lblServicos = TelaPrincipal.rotulo("R$ 0,00", Font.PLAIN, 13);
    private final JLabel lblTotal = TelaPrincipal.rotulo("R$ 0,00", Font.BOLD, 15);
    private final JLabel lblValidade = TelaPrincipal.rotulo(" ", Font.PLAIN, 13);
    private final JLabel lblSinal = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final TelaPrincipal.Campo tfOutro = new TelaPrincipal.Campo("Outro valor (R$)", 12);
    private final JToggleButton[] bSinal = new JToggleButton[Regras.SINAIS_RAPIDOS.length];
    private final ButtonGroup grupoSinal = new ButtonGroup();
    private final JLabel msg = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final JButton btnConfirmar = Ui.botaoPreto("Registrar entrada e confirmar reserva", 12);
    private final JButton btnArquivar = TelaPrincipal.botaoCinza("Arquivar orçamento", 12);
    private final JButton btnRascunho = TelaPrincipal.botaoCinza("Salvar rascunho", 12);
    private double pctSinal = Regras.SINAL_PADRAO;     // 0 = valor digitado em "Outro valor"
    private boolean montando = false, retEditada = false, devEditada = false, ajustando = false;

    /** Linha do orçamento em montagem: produto/pacote (com desconto) ou serviço (com valor individual). */
    private static class Linha {
        Catalogo.Item item;
        Catalogo.Servico servico;
        int qtd = 1;
        double desconto;       // %
        double valor;          // só serviço
        double subtotal() { return item != null ? item.preco * qtd * (1 - desconto / 100.0) : valor; }
    }

    public TelaOrcamentos(TelaPrincipal app, PainelController ctrl) {
        super(new CardLayout());
        this.app = app;
        this.ctrl = ctrl;
        setBackground(TelaPrincipal.BRANCO);
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
        p.setBackground(TelaPrincipal.BRANCO);
        p.setBorder(new EmptyBorder(14, 16, 16, 16));

        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Orçamentos", Font.BOLD, 20), BorderLayout.WEST);
        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        dir.setOpaque(false);
        busca.setPreferredSize(new Dimension(240, 36));
        busca.getDocument().addDocumentListener(Ui.ouvinte(this::atualizar));
        TelaPrincipal.estilizarCombo(filtro);
        filtro.setPreferredSize(new Dimension(170, 36));
        filtro.addActionListener(e -> atualizar());
        JButton novo = TelaPrincipal.botaoCinza("+ Novo orçamento", 13);
        novo.setPreferredSize(new Dimension(160, 36));
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
        JScrollPane sp = new JScrollPane(tabelaLista);
        sp.setBorder(new LineBorder(TelaPrincipal.CINZA_BORDA, 2));
        sp.getViewport().setBackground(TelaPrincipal.BRANCO);
        p.add(sp, BorderLayout.CENTER);

        JButton abrir = TelaPrincipal.botaoCinza("Abrir / editar", 12);
        JButton cancelar = TelaPrincipal.botaoCinza("Cancelar orçamento", 12);
        abrir.setPreferredSize(new Dimension(140, 34));
        cancelar.setPreferredSize(new Dimension(170, 34));
        abrir.addActionListener(e -> abrirSelecionado());
        cancelar.addActionListener(e -> {
            Locacao l = selecionada();
            if (l != null) arquivarOrcamento(l);
        });
        JLabel dica = TelaPrincipal.rotulo("Duplo clique abre. Orçamento vale 30 dias e só reserva itens depois do sinal.", Font.PLAIN, 11);
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
        if (l.arquivada || l.temEntrada()) app.abrirDetalhes(l);    // reserva confirmada/arquivado: só consulta
        else editar(l);
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
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(TelaPrincipal.BRANCO);
        p.setBorder(new EmptyBorder(14, 16, 12, 16));

        // cabeçalho: título + voltar + salvar rascunho
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.add(tituloForm, BorderLayout.WEST);
        JButton voltar = TelaPrincipal.botaoCinza("‹ Voltar à lista", 12);
        voltar.setPreferredSize(new Dimension(130, 36));
        voltar.addActionListener(e -> mostrar("lista"));
        btnRascunho.setPreferredSize(new Dimension(140, 36));
        btnRascunho.addActionListener(e -> salvarRascunho());
        JPanel bt = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bt.setOpaque(false);
        bt.add(voltar);
        bt.add(btnRascunho);
        cab.add(bt, BorderLayout.EAST);
        p.add(cab, BorderLayout.NORTH);

        JPanel corpo = new JPanel(new BorderLayout(14, 0));
        corpo.setOpaque(false);
        corpo.add(criarEsquerda(), BorderLayout.CENTER);
        corpo.add(criarResumo(), BorderLayout.EAST);
        p.add(corpo, BorderLayout.CENTER);

        msg.setBorder(new EmptyBorder(2, 2, 0, 0));
        p.add(msg, BorderLayout.SOUTH);

        configurarCampos();
        return p;
    }

    private JComponent criarEsquerda() {
        JPanel esq = new JPanel(new BorderLayout(0, 10));
        esq.setOpaque(false);

        // --- cliente e datas ---
        TelaPrincipal.estilizarCombo(cbCliente);
        TelaPrincipal.estilizarCombo(cbModalidade);
        cbCliente.setPreferredSize(new Dimension(100, 34));
        cbModalidade.setPreferredSize(new Dimension(190, 34));
        avisoCliente.setOpaque(true);
        avisoCliente.setBackground(TelaPrincipal.PRETO);
        avisoCliente.setForeground(TelaPrincipal.BRANCO);
        avisoCliente.setBorder(new EmptyBorder(4, 6, 4, 6));
        avisoCliente.setVisible(false);
        JPanel linha1 = new JPanel(new BorderLayout(8, 0));
        linha1.setOpaque(false);
        linha1.add(cbCliente, BorderLayout.CENTER);
        linha1.add(cbModalidade, BorderLayout.EAST);
        JPanel datas = new JPanel(new GridLayout(1, 3, 8, 0));
        datas.setOpaque(false);
        for (TelaPrincipal.Campo c : new TelaPrincipal.Campo[]{tfRetirada, tfEvento, tfDevolucao}) c.setPreferredSize(new Dimension(100, 34));
        datas.add(Ui.bloco("Retirada", tfRetirada));
        datas.add(Ui.bloco("Data do evento", tfEvento));
        datas.add(Ui.bloco("Devolução", tfDevolucao));
        JPanel dados = new JPanel();
        dados.setLayout(new BoxLayout(dados, BoxLayout.Y_AXIS));
        dados.setOpaque(false);
        for (JComponent c : new JComponent[]{linha1, avisoCliente, datas}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            dados.add(c);
            dados.add(Box.createVerticalStrut(6));
        }
        esq.add(Ui.caixa("CLIENTE E DATAS", dados), BorderLayout.NORTH);

        // --- itens da proposta ---
        JButton bProd = TelaPrincipal.botaoCinza("+ Produto", 12);
        JButton bPac = TelaPrincipal.botaoCinza("+ Pacote", 12);
        JButton bServ = TelaPrincipal.botaoCinza("+ Serviço", 12);
        JButton bRem = TelaPrincipal.botaoCinza("Remover", 12);
        JButton bVer = TelaPrincipal.botaoCinza("Verificar disponibilidade", 12);
        for (JButton b : new JButton[]{bProd, bPac, bServ, bRem}) b.setPreferredSize(new Dimension(b == bRem ? 90 : 100, 34));
        bVer.setPreferredSize(new Dimension(210, 34));
        bProd.addActionListener(e -> escolherItem(Catalogo.Tipo.PRODUTO));
        bPac.addActionListener(e -> escolherItem(Catalogo.Tipo.PACOTE));
        bServ.addActionListener(e -> escolherServico());
        bRem.addActionListener(e -> remover());
        bVer.addActionListener(e -> verificar());
        JPanel esqBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        esqBotoes.setOpaque(false);
        esqBotoes.add(bProd);
        esqBotoes.add(bPac);
        esqBotoes.add(bServ);
        JPanel dirBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        dirBotoes.setOpaque(false);
        dirBotoes.add(bRem);
        dirBotoes.add(bVer);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.add(esqBotoes, BorderLayout.WEST);
        barra.add(dirBotoes, BorderLayout.EAST);

        TelaClientes.estilizarTabela(tabelaItens, 12, 6);
        int[] larg = {200, 50, 85, 60, 90, 80};
        for (int i = 0; i < larg.length; i++) tabelaItens.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
        tabelaItens.setSurrendersFocusOnKeystroke(true);
        tabelaItens.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "remover");
        tabelaItens.getActionMap().put("remover", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { remover(); }
        });
        JScrollPane sp = new JScrollPane(tabelaItens);
        sp.setBorder(new LineBorder(TelaPrincipal.CINZA_LINHA, 1));
        sp.getViewport().setBackground(TelaPrincipal.BRANCO);

        JPanel itens = new JPanel(new BorderLayout(0, 8));
        itens.setOpaque(false);
        itens.add(barra, BorderLayout.NORTH);
        itens.add(sp, BorderLayout.CENTER);
        esq.add(Ui.caixa("ITENS DA PROPOSTA   (dê dois cliques em Qtde, Desc. % ou, nos serviços, Valor un. para editar)", itens), BorderLayout.CENTER);
        return esq;
    }

    private JComponent criarResumo() {
        JPanel r = new JPanel();
        r.setLayout(new BoxLayout(r, BoxLayout.Y_AXIS));
        r.setOpaque(false);
        r.add(linha("Subtotal", lblSubtotal));
        r.add(linha("Desconto manual", lblDesconto));
        r.add(linha("Serviços", lblServicos));
        r.add(Box.createVerticalStrut(4));
        r.add(linha("Total", lblTotal));
        r.add(Box.createVerticalStrut(4));
        lblValidade.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(lblValidade);
        r.add(Box.createVerticalStrut(10));

        JLabel ent = TelaPrincipal.rotulo("ENTRADA (SINAL)", Font.BOLD, 12);
        ent.setForeground(TelaPrincipal.CINZA_TEXTO);
        ent.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(ent);
        r.add(Box.createVerticalStrut(4));
        JPanel pct = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pct.setOpaque(false);
        pct.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < Regras.SINAIS_RAPIDOS.length; i++) {
            final double v = Regras.SINAIS_RAPIDOS[i];
            JToggleButton b = Ui.botaoEscolha(Math.round(v * 100) + "%");
            b.setPreferredSize(new Dimension(62, 32));
            b.addActionListener(e -> { pctSinal = v; tfOutro.setText(""); atualizarValores(); });
            grupoSinal.add(b);
            bSinal[i] = b;
            pct.add(b);
        }
        r.add(pct);
        r.add(Box.createVerticalStrut(6));
        tfOutro.setPreferredSize(new Dimension(200, 34));
        tfOutro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        tfOutro.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfOutro.getDocument().addDocumentListener(Ui.ouvinte(() -> {
            if (montando || ajustando) return;
            if (!tfOutro.getText().trim().isEmpty()) { pctSinal = 0; grupoSinal.clearSelection(); }
            atualizarValores();
        }));
        r.add(tfOutro);
        r.add(Box.createVerticalStrut(6));
        lblSinal.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(lblSinal);
        JLabel nota = TelaPrincipal.rotulo("<html><div style='width:210px'>Itens só são bloqueados após a entrada.</div></html>", Font.PLAIN, 11);
        nota.setForeground(TelaPrincipal.CINZA_TEXTO);
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(Box.createVerticalStrut(4));
        r.add(nota);
        r.add(Box.createVerticalStrut(12));

        btnConfirmar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnConfirmar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnConfirmar.setPreferredSize(new Dimension(250, 40));
        btnConfirmar.addActionListener(e -> confirmar());
        btnArquivar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnArquivar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnArquivar.setPreferredSize(new Dimension(250, 38));
        btnArquivar.addActionListener(e -> arquivar());
        r.add(btnConfirmar);
        r.add(Box.createVerticalStrut(8));
        r.add(btnArquivar);

        JPanel envolve = new JPanel(new BorderLayout());
        envolve.setOpaque(false);
        envolve.setPreferredSize(new Dimension(270, 100));
        envolve.add(Ui.caixa("RESUMO", r), BorderLayout.NORTH);
        return envolve;
    }

    private static JComponent linha(String titulo, JLabel valor) {
        JPanel l = new JPanel(new BorderLayout());
        l.setOpaque(false);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
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
    }

    // ----------------------------------------------------------------------------------
    //  Abrir o formulário
    // ----------------------------------------------------------------------------------
    /** Formulário em branco (botão "+ Novo orçamento"). */
    public void novo() {
        preparar(null);
        mostrar("form");
    }

    /** Formulário com um orçamento em negociação (editar dentro da validade). */
    private void editar(Locacao l) {
        preparar(l);
        mostrar("form");
    }

    private void preparar(Locacao l) {
        montando = true;
        editando = l;
        linhas.clear();
        msg.setText(" ");
        cbCliente.removeAllItems();
        for (Cliente c : ctrl.clientes) cbCliente.addItem(c);
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
            for (ServicoContratado s : l.servicos) {
                Linha x = new Linha();
                x.servico = s.servico; x.valor = s.valor;
                linhas.add(x);
            }
        }
        btnArquivar.setText(l == null ? "Descartar orçamento" : "Arquivar orçamento");
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
        qtd.setPreferredSize(new Dimension(300, 34));
        JLabel det = TelaPrincipal.rotulo(" ", Font.PLAIN, 11);
        Runnable mostraDet = () -> {
            Catalogo.Item i = ((Opcao) cb.getSelectedItem()).item();
            det.setText("<html><div style='width:300px'>" + (i.tipo == Catalogo.Tipo.PACOTE ? "Inclui: " + i.composicao()
                    : "Avulso" + (i.cor.isEmpty() ? "" : " · " + i.cor)) + "</div></html>");
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
        valor.setPreferredSize(new Dimension(300, 34));
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

    private void verificar() {
        pararEdicao();
        if (linhas.isEmpty()) { msg("Adicione produtos ou pacotes para verificar."); return; }
        List<String> c = conflitos();
        msg(c.isEmpty() ? Mensagens.MSG020 : Mensagens.MSG010 + " (" + String.join(", ", c) + ")");
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
            if (l.item != null) { bruto += l.item.preco * l.qtd; desc += l.item.preco * l.qtd - l.subtotal(); }
            else serv += l.valor;
        }
        lblSubtotal.setText(TelaPrincipal.moeda(bruto));
        lblDesconto.setText(TelaPrincipal.moeda(desc));
        lblServicos.setText(TelaPrincipal.moeda(serv));
        lblTotal.setText(TelaPrincipal.moeda(total()));
        LocalDate validade = editando != null && editando.validade != null ? editando.validade
                : ctrl.hoje().plusDays(Regras.DIAS_VALIDADE_ORCAMENTO);
        lblValidade.setText("Validade: " + Regras.DIAS_VALIDADE_ORCAMENTO + " dias (" + TelaPrincipal.FMT_CURTA.format(validade) + ")");
        double s = sinal();
        lblSinal.setText("<html><div style='width:215px'>Sinal: " + TelaPrincipal.moeda(s)
                + (total() > 0.005 ? "  (" + Math.round(s / total() * 100) + "% do total)" : "") + "</div></html>");
        Cliente c = (Cliente) cbCliente.getSelectedItem();
        avisoCliente.setVisible(c != null && ctrl.inadimplente(c));
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
            x.desconto = l.desconto;
            r.add(x);
        }
        return r;
    }

    private List<ServicoContratado> servicosDoForm() {
        List<ServicoContratado> r = new ArrayList<>();
        for (Linha l : linhas) if (l.servico != null) r.add(new ServicoContratado(l.servico, l.valor));
        return r;
    }

    private void salvarRascunho() {
        pararEdicao();
        try {
            Locacao l = editando;
            if (l == null) {
                l = ctrl.criarOrcamento((Cliente) cbCliente.getSelectedItem(), Ui.lerData(tfEvento), Ui.lerData(tfRetirada),
                        Ui.lerData(tfDevolucao), (String) cbModalidade.getSelectedItem(), itensDoForm(), servicosDoForm(),
                        0, null, null);
            } else {
                ctrl.atualizarOrcamento(l, Ui.lerData(tfEvento), Ui.lerData(tfRetirada), Ui.lerData(tfDevolucao),
                        (String) cbModalidade.getSelectedItem(), itensDoForm(), servicosDoForm());
            }
            app.aviso(Mensagens.MSG001 + "\nOrçamento " + l.codigo() + " · " + l.cliente.nome + "\nVálido até "
                    + Ui.data(l.validade) + ". Ainda NÃO reserva itens: só o sinal reserva.");
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
        int r = JOptionPane.showOptionDialog(this, p, "Confirmar reserva", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Registrar entrada", "Cancelar"}, "Registrar entrada");
        if (r != 0) return;
        Pagamento.Forma f = (Pagamento.Forma) forma.getSelectedItem();
        try {
            Locacao l = editando;
            try {
                l = gravarConfirmando(entrada, f, null);
            } catch (ExigeAutorizacaoException e) {                       // UC22
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
                    entrada, f, autoriza);
        }
        ctrl.atualizarOrcamento(editando, Ui.lerData(tfEvento), Ui.lerData(tfRetirada), Ui.lerData(tfDevolucao),
                (String) cbModalidade.getSelectedItem(), itensDoForm(), servicosDoForm());
        ctrl.pagar(editando, entrada, f, autoriza);
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
            return l.item != null ? (c == 1 || c == 3) : c == 2;
        }

        public Object getValueAt(int i, int c) {
            Linha l = linhas.get(i);
            boolean item = l.item != null;
            switch (c) {
                case 0:  return item ? l.item.nome : "Serviço: " + l.servico.nome;
                case 1:  return item ? String.valueOf(l.qtd) : "1";
                case 2:  return TelaPrincipal.moeda(item ? l.item.preco : l.valor);
                case 3:  return item ? (l.desconto == Math.rint(l.desconto) ? String.valueOf((int) l.desconto)
                        : String.format(TelaPrincipal.PT_BR, "%.1f", l.desconto)) + "%" : "-";
                case 4:  return TelaPrincipal.moeda(l.subtotal());
                default: return !item ? "-" : (emConflito(l) ? "Conflito" : "OK");
            }
        }

        @Override public void setValueAt(Object v, int i, int c) {
            Linha l = linhas.get(i);
            String t = String.valueOf(v).replace("%", "").trim();
            if (l.item != null && c == 1) {
                try { int n = Integer.parseInt(t); if (n > 0) l.qtd = n; } catch (NumberFormatException e) { /* ignora */ }
            } else if (l.item != null && c == 3) {
                double d = TelaPrincipal.lerValor(t);
                if (d >= 0 && d <= 100) l.desconto = d;
            } else if (l.item == null && c == 2) {
                l.valor = Math.max(0, TelaPrincipal.lerValor(t));
            }
            atualizarValores();
        }
    }
}
