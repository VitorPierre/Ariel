package view;

import controller.PainelController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Tela "Clientes" (preto e branco), aberta pelo menu lateral da TelaPrincipal.
 *
 *  - LISTA: CPF, Nome, WhatsApp e Situação (Em dia / Inadimplente), com busca e filtro.
 *  - FICHA DO CLIENTE: cadastro com CPF validado (único), máscaras e alerta de inadimplência.
 *  - PESSOAS AUTORIZADAS: quem pode retirar e/ou devolver pelo cliente.
 *
 * Integração com o painel (TelaPrincipal):
 *  - a situação vem do painel (marcação manual ou locação atrasada);
 *  - "Histórico de locações" lista os cartões do cliente no painel;
 *  - excluir cliente só é permitido sem orçamento/locação em aberto.
 *
 * Interações: passar o mouse (linhas, botões, campos), clicar numa linha para abrir a ficha,
 * botão direito na lista (histórico, situação, excluir), tecla Delete, Salvar com validação.
 */
public class TelaClientes extends JPanel {

    private static final Color PRETO        = TelaPrincipal.PRETO;
    private static final Color BRANCO       = TelaPrincipal.BRANCO;
    private static final Color CINZA_CAB    = TelaPrincipal.CINZA_CAB;
    private static final Color CINZA_BTN    = TelaPrincipal.CINZA_BTN;
    private static final Color CINZA_SEL    = TelaPrincipal.CINZA_SEL;
    private static final Color CINZA_HOVER  = TelaPrincipal.CINZA_HOVER;
    private static final Color CINZA_TEXTO  = TelaPrincipal.CINZA_TEXTO;
    private static final Color CINZA_LINHA  = TelaPrincipal.CINZA_LINHA;
    private static final Color CINZA_BORDA  = TelaPrincipal.CINZA_BORDA;
    private static final Color CINZA_PAINEL = new Color(0x666666);

    private static final String[] FILTROS = {"Todos", "Em dia", "Inadimplente"};
    private static final String[] TIPOS = {"Retira/Devolve", "Retira", "Devolve"};
    private static final int ALTURA_LINHA = 24;
    private static final int LARGURA_LATERAL = 360;

    private final TelaPrincipal app;          // janela principal (só para navegar entre telas)
    private final PainelController ctrl;      // CONTROLLER: regras de negócio e dados
    private final List<Cliente> clientes;
    private final List<Cliente> visiveis = new ArrayList<>();

    // ----- lista -----
    private final ModeloClientes modeloClientes = new ModeloClientes();
    private final JTable tabela = new JTable(modeloClientes);
    private final JScrollPane rolagemLista = new JScrollPane(tabela);
    private final TelaPrincipal.Campo busca = new TelaPrincipal.Campo("Buscar por nome ou CPF", 12);
    private final JComboBox<String> filtro = new JComboBox<>(FILTROS);

    // ----- ficha -----
    private final JLabel tituloFicha = TelaPrincipal.rotulo("FICHA DO CLIENTE", Font.BOLD, 12);
    private final JPanel alertaBox = new JPanel(new BorderLayout());
    private final TelaPrincipal.Campo tfCpf = new TelaPrincipal.Campo("CPF * (único)", 13);
    private final TelaPrincipal.Campo tfNome = new TelaPrincipal.Campo("Nome", 13);
    private final TelaPrincipal.Campo tfRg = new TelaPrincipal.Campo("RG", 13);
    private final TelaPrincipal.Campo tfInstagram = new TelaPrincipal.Campo("Instagram", 13);
    private final TelaPrincipal.Campo tfWhats = new TelaPrincipal.Campo("Telefone / WhatsApp", 13);
    private final TelaPrincipal.Campo tfEndereco = new TelaPrincipal.Campo("Endereço completo *", 13);
    private final AreaObs taObs = new AreaObs("Observações");
    private final JScrollPane rolagemObs = new JScrollPane(taObs);
    private final JButton btnHistorico = TelaPrincipal.botaoCinza("Histórico de locações", 12);
    private final JButton btnSalvar = new JButton("Salvar");
    private final JButton btnSituacao = TelaPrincipal.botaoCinza("Marcar como inadimplente", 12);   // alterna Em dia / Inadimplente (manual)

    // ----- pessoas autorizadas -----
    private final ModeloAutorizadas modeloAut = new ModeloAutorizadas();
    private final JTable tabelaAut = new JTable(modeloAut);
    private final JScrollPane rolagemAut = new JScrollPane(tabelaAut);

    private Cliente selecionado;           // null = novo cadastro
    private boolean carregando = false;    // true enquanto a ficha é preenchida pelo código
    private boolean sujo = false;          // há alterações não salvas
    private boolean ignorarSelecao = false;
    private boolean salvarHover = false;
    private boolean obsHover = false;

    public TelaClientes(TelaPrincipal app, PainelController ctrl) {
        super(new BorderLayout());
        this.app = app;
        this.ctrl = ctrl;
        this.clientes = ctrl.clientes;
        setBackground(BRANCO);
        setBorder(new EmptyBorder(14, 16, 16, 16));

        add(criarCabecalho(), BorderLayout.NORTH);
        add(criarCorpo(), BorderLayout.CENTER);

        configurarMascaras();
        configurarAlteracoes();
        configurarSalvar();
        configurarObs();

        recarregarLista();
        carregar(null);
    }

    // ================= Cabeçalho: título, busca, filtro e "+ Novo cliente" =================
    private JComponent criarCabecalho() {
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Clientes", Font.BOLD, 20), BorderLayout.WEST);

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        direita.setOpaque(false);

        busca.setPreferredSize(new Dimension(230, 36));
        busca.getDocument().addDocumentListener(simples(this::recarregarLista));
        direita.add(busca);

        TelaPrincipal.estilizarCombo(filtro);
        filtro.setPreferredSize(new Dimension(130, 36));
        filtro.addActionListener(e -> recarregarLista());
        direita.add(filtro);

        JButton novo = TelaPrincipal.botaoCinza("+ Novo cliente", 13);
        novo.setPreferredSize(new Dimension(140, 36));
        novo.addActionListener(e -> novoCliente());
        direita.add(novo);

        cab.add(direita, BorderLayout.EAST);
        return cab;
    }

    // ================= Corpo: lista (esquerda) + ficha e autorizadas (direita) =================
    private JComponent criarCorpo() {
        JPanel corpo = new JPanel(new GridBagLayout());
        corpo.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1;

        g.gridx = 0;
        g.weightx = 1;
        g.insets = new Insets(0, 0, 0, 12);
        corpo.add(criarLista(), g);

        g.gridx = 1;
        g.weightx = 0;
        g.insets = new Insets(0, 0, 0, 0);
        corpo.add(criarLateral(), g);
        return corpo;
    }

    // ---------- LISTA ----------
    private JComponent criarLista() {
        JPanel lista = new JPanel(new BorderLayout());
        lista.setBackground(BRANCO);
        lista.setBorder(new CompoundBorder(new LineBorder(CINZA_PAINEL, 2), new EmptyBorder(8, 10, 10, 10)));

        JLabel t = TelaPrincipal.rotulo("LISTA", Font.BOLD, 12);
        t.setForeground(CINZA_TEXTO);
        t.setBorder(new EmptyBorder(0, 0, 6, 0));
        lista.add(t, BorderLayout.NORTH);

        estilizarTabela(tabela, 12, 6);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(120);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(150);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(115);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(3).setCellRenderer(new RenderSituacao());
        rolagemLista.setBorder(new LineBorder(CINZA_LINHA, 1));
        rolagemLista.getViewport().setBackground(BRANCO);
        lista.add(rolagemLista, BorderLayout.CENTER);

        // clicar numa linha abre a ficha do cliente
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || ignorarSelecao) return;
            int linha = tabela.getSelectedRow();
            if (linha < 0) return;
            Cliente novo = modeloClientes.get(linha);
            if (novo == selecionado) return;
            if (sujo && !confirmarDescarte()) { restaurarSelecao(); return; }
            carregar(novo);
        });

        // botão direito: histórico, situação e excluir
        tabela.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { if (e.isPopupTrigger()) menuLista(e); }
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) menuLista(e); }
        });

        // tecla Delete exclui o cliente selecionado
        tabela.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "excluirCliente");
        tabela.getActionMap().put("excluirCliente", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (selecionado != null) excluir(selecionado); }
        });

        // a caixa da lista tem a altura das linhas (como no desenho) e rola se passar do espaço
        JPanel envoltorio = new JPanel(new TopoLayout());
        envoltorio.setOpaque(false);
        envoltorio.add(lista);
        return envoltorio;
    }

    // ---------- Coluna da direita ----------
    private JComponent criarLateral() {
        PainelRolavel coluna = new PainelRolavel();
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));
        coluna.setBackground(BRANCO);
        coluna.add(criarFicha());
        coluna.add(criarAutorizadas());

        JScrollPane sp = new JScrollPane(coluna, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setBackground(BRANCO);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setPreferredSize(new Dimension(LARGURA_LATERAL, 100));
        sp.setMinimumSize(new Dimension(LARGURA_LATERAL, 100));
        return sp;
    }

    // ---------- FICHA DO CLIENTE ----------
    private JComponent criarFicha() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BRANCO);
        p.setBorder(new CompoundBorder(new LineBorder(CINZA_PAINEL, 2), new EmptyBorder(10, 10, 10, 10)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        tituloFicha.setForeground(CINZA_TEXTO);
        tituloFicha.setAlignmentX(Component.LEFT_ALIGNMENT);
        tituloFicha.setBorder(new EmptyBorder(0, 0, 6, 0));
        p.add(tituloFicha);

        // alerta (só aparece para cliente inadimplente)
        JLabel texto = new JLabel("<html><div style='width:215px'><b>ALERTA: cliente inadimplente "
                + "(nova locação exige autorização)</b></div></html>");
        texto.setFont(new Font("SansSerif", Font.PLAIN, 12));
        texto.setForeground(PRETO);
        JPanel caixa = new JPanel(new BorderLayout());
        caixa.setBackground(CINZA_SEL);
        caixa.setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 2), new EmptyBorder(6, 8, 6, 8)));
        caixa.add(texto, BorderLayout.CENTER);
        alertaBox.setOpaque(false);
        alertaBox.setBorder(new EmptyBorder(0, 0, 6, 0));
        alertaBox.add(caixa, BorderLayout.CENTER);
        alertaBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        alertaBox.setVisible(false);
        p.add(alertaBox);

        for (TelaPrincipal.Campo c : new TelaPrincipal.Campo[]{tfCpf, tfNome, tfRg, tfInstagram, tfWhats, tfEndereco}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.setPreferredSize(new Dimension(100, 34));
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            p.add(c);
            p.add(Box.createVerticalStrut(6));
        }

        rolagemObs.setAlignmentX(Component.LEFT_ALIGNMENT);
        rolagemObs.setPreferredSize(new Dimension(100, 58));
        rolagemObs.setMinimumSize(new Dimension(100, 58));
        rolagemObs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        rolagemObs.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(rolagemObs);
        p.add(Box.createVerticalStrut(8));

        btnSituacao.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSituacao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnSituacao.setPreferredSize(new Dimension(100, 34));
        btnSituacao.setToolTipText("A inadimplência é decidida por você; o sistema só alerta (descreva o motivo em Observações).");
        btnSituacao.addActionListener(e -> { if (selecionado != null) alternarInadimplencia(selecionado); });
        p.add(btnSituacao);
        p.add(Box.createVerticalStrut(6));

        JPanel botoes = new JPanel(new BorderLayout());
        botoes.setOpaque(false);
        botoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        botoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnHistorico.setPreferredSize(new Dimension(172, 34));
        btnHistorico.addActionListener(e -> historico());
        btnSalvar.setPreferredSize(new Dimension(92, 34));
        botoes.add(btnHistorico, BorderLayout.WEST);
        botoes.add(btnSalvar, BorderLayout.EAST);
        p.add(botoes);
        return p;
    }

    // ---------- PESSOAS AUTORIZADAS ----------
    private JComponent criarAutorizadas() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BRANCO);
        p.setBorder(new CompoundBorder(new LineBorder(CINZA_PAINEL, 2), new EmptyBorder(10, 10, 10, 10)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = TelaPrincipal.rotulo("PESSOAS AUTORIZADAS", Font.BOLD, 12);
        t.setForeground(CINZA_TEXTO);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(new EmptyBorder(0, 0, 6, 0));
        p.add(t);

        estilizarTabela(tabelaAut, 11, 4);
        tabelaAut.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabelaAut.getColumnModel().getColumn(1).setPreferredWidth(95);
        tabelaAut.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabelaAut.getColumnModel().getColumn(3).setPreferredWidth(85);
        rolagemAut.setBorder(new LineBorder(CINZA_LINHA, 1));
        rolagemAut.getViewport().setBackground(BRANCO);
        rolagemAut.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(rolagemAut);
        p.add(Box.createVerticalStrut(8));

        tabelaAut.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { if (e.isPopupTrigger()) menuAutorizadas(e); }
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) menuAutorizadas(e); }
        });
        tabelaAut.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removerPessoa");
        tabelaAut.getActionMap().put("removerPessoa", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int linha = tabelaAut.getSelectedRow();
                if (linha >= 0) removerAutorizada(linha);
            }
        });

        JButton autorizar = TelaPrincipal.botaoCinza("+ Autorizar pessoa", 13);
        autorizar.setAlignmentX(Component.LEFT_ALIGNMENT);
        autorizar.setPreferredSize(new Dimension(100, 34));
        autorizar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        autorizar.addActionListener(e -> autorizarPessoa());
        p.add(autorizar);
        return p;
    }

    // ================= Dados e estado =================

    /** Chamado pela TelaPrincipal ao abrir esta tela e sempre que algo muda (ex.: virada do dia). */
    void atualizar() {
        recarregarLista();
        atualizarAlerta();
    }

    /** Há alterações não salvas na ficha? */
    boolean temAlteracoes() {
        return sujo;
    }

    /** Abre a ficha de um cliente (usado pelos atalhos do painel). */
    void abrirCliente(Cliente c) {
        if (c != selecionado && sujo && !confirmarDescarte()) return;
        busca.setText("");
        filtro.setSelectedIndex(0);
        carregar(c);
        recarregarLista();
        int i = visiveis.indexOf(c);
        if (i >= 0) tabela.scrollRectToVisible(tabela.getCellRect(i, 0, true));
    }

    private void recarregarLista() {
        ignorarSelecao = true;
        try {
            String q = busca.getText().trim().toLowerCase();
            String qDigitos = digitos(q);
            String situacao = (String) filtro.getSelectedItem();

            visiveis.clear();
            for (Cliente c : clientes) {
                boolean inad = ctrl.inadimplente(c);
                if ("Em dia".equals(situacao) && inad) continue;
                if ("Inadimplente".equals(situacao) && !inad) continue;
                if (!q.isEmpty()) {
                    boolean porNome = c.nome.toLowerCase().contains(q);
                    boolean porCpf = !qDigitos.isEmpty() && digitos(c.cpf).contains(qDigitos);
                    if (!porNome && !porCpf) continue;
                }
                visiveis.add(c);
            }
            Collator ordem = Collator.getInstance(TelaPrincipal.PT_BR);
            visiveis.sort((a, b) -> ordem.compare(a.nome, b.nome));

            modeloClientes.fireTableDataChanged();
            tabela.setPreferredScrollableViewportSize(new Dimension(100, Math.max(visiveis.size(), 2) * ALTURA_LINHA));
            rolagemLista.revalidate();

            int i = visiveis.indexOf(selecionado);
            if (i >= 0) tabela.setRowSelectionInterval(i, i);
            else tabela.clearSelection();
            tabela.repaint();
        } finally {
            ignorarSelecao = false;
        }
    }

    private void restaurarSelecao() {
        ignorarSelecao = true;
        int i = visiveis.indexOf(selecionado);
        if (i >= 0) tabela.setRowSelectionInterval(i, i);
        else tabela.clearSelection();
        ignorarSelecao = false;
    }

    /** Preenche a ficha com o cliente (ou deixa em branco para novo cadastro). */
    private void carregar(Cliente c) {
        carregando = true;
        try {
            selecionado = c;
            tfCpf.setText(c == null ? "" : c.cpf);
            tfNome.setText(c == null ? "" : c.nome);
            tfRg.setText(c == null ? "" : c.rg);
            tfInstagram.setText(c == null ? "" : c.instagram);
            tfWhats.setText(c == null ? "" : c.whatsapp);
            tfEndereco.setText(c == null ? "" : c.endereco);
            taObs.setText(c == null ? "" : c.observacoes);
            taObs.setCaretPosition(0);
            sujo = false;
            tituloFicha.setText(c == null ? "FICHA DO CLIENTE · NOVO" : "FICHA DO CLIENTE");
            atualizarAutorizadas();
            atualizarAlerta();
        } finally {
            carregando = false;
        }
        estilizarSalvar();
        atualizarEstiloObs();
    }

    private void atualizarAlerta() {
        alertaBox.setVisible(selecionado != null && ctrl.inadimplente(selecionado));
        alertaBox.revalidate();
        btnSituacao.setEnabled(selecionado != null);
        btnSituacao.setText(selecionado != null && selecionado.inadimplenteManual ? "Marcar como em dia" : "Marcar como inadimplente");
    }

    private void atualizarAutorizadas() {
        modeloAut.fireTableDataChanged();
        int linhas = Math.max(1, Math.min(2, selecionado == null ? 0 : selecionado.autorizadas.size()));
        tabelaAut.setPreferredScrollableViewportSize(new Dimension(100, linhas * ALTURA_LINHA));
        rolagemAut.revalidate();
    }

    private boolean confirmarDescarte() {
        int r = JOptionPane.showOptionDialog(this,
                "Há alterações não salvas na ficha.\nDeseja descartá-las?",
                "Alterações não salvas", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Descartar", "Continuar editando"}, "Continuar editando");
        return r == 0;
    }

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.PLAIN_MESSAGE);
    }

    // ================= Ações =================
    private void novoCliente() {
        if (sujo && !confirmarDescarte()) return;
        ignorarSelecao = true;
        tabela.clearSelection();
        ignorarSelecao = false;
        carregar(null);
        tfCpf.requestFocusInWindow();
    }

    private void salvar() {
        if (!sujo) { aviso("Nenhuma alteração para salvar."); return; }

        String cpfDigitos = digitos(tfCpf.getText());
        if (cpfDigitos.isEmpty()) { aviso("Informe o CPF do cliente."); tfCpf.requestFocusInWindow(); return; }
        if (!cpfValido(cpfDigitos)) { aviso("CPF inválido. Confira os números digitados."); tfCpf.requestFocusInWindow(); return; }
        for (Cliente o : clientes) {
            if (o != selecionado && digitos(o.cpf).equals(cpfDigitos)) {
                aviso("Já existe um cliente com este CPF: " + o.nome + ".");
                tfCpf.requestFocusInWindow();
                return;
            }
        }
        String nome = tfNome.getText().trim();
        if (nome.isEmpty()) { aviso("Informe o nome do cliente."); tfNome.requestFocusInWindow(); return; }
        String whats = tfWhats.getText().trim();
        if (!whats.isEmpty() && digitos(whats).length() < 10) {
            aviso("Telefone / WhatsApp incompleto. Informe o DDD e o número.");
            tfWhats.requestFocusInWindow();
            return;
        }
        String endereco = tfEndereco.getText().trim();
        if (endereco.isEmpty()) { aviso("Informe o endereço completo do cliente."); tfEndereco.requestFocusInWindow(); return; }

        Cliente c = selecionado != null ? selecionado : new Cliente();
        c.cpf = tfCpf.getText().trim();
        c.nome = nome;
        c.rg = tfRg.getText().trim();
        c.instagram = tfInstagram.getText().trim();
        c.whatsapp = whats;
        c.endereco = endereco;
        c.observacoes = taObs.getText().trim();
        if (selecionado == null) clientes.add(c);

        carregar(c);             // recarrega a ficha já salva
        ctrl.notificar();   // atualiza o painel (nomes nos cartões) e esta lista
        int i = visiveis.indexOf(c);
        if (i >= 0) tabela.scrollRectToVisible(tabela.getCellRect(i, 0, true));

        btnSalvar.setText("Salvo ✓");
        javax.swing.Timer t = new javax.swing.Timer(1500, e -> btnSalvar.setText("Salvar"));
        t.setRepeats(false);
        t.start();
    }

    private void historico() {
        if (selecionado == null) { aviso("Salve o cadastro do cliente para ver o histórico de locações."); return; }
        // o controller devolve as locações do cliente; aqui só formatamos as linhas da tabela
        List<String[]> linhas = new ArrayList<>();
        for (Locacao k : ctrl.historicoDe(selecionado)) {
            boolean saiu = k.etapa != Etapa.ORCAMENTO;
            linhas.add(new String[]{
                    k.codigo(), k.arquivada ? "Arquivado" : k.etapa.nome, TelaPrincipal.FMT_COMPLETA.format(k.evento),
                    saiu ? TelaPrincipal.FMT_COMPLETA.format(k.retirada) : "-",
                    saiu ? TelaPrincipal.FMT_COMPLETA.format(k.devolucao) : "-",
                    TelaPrincipal.moeda(k.total()), ctrl.aviso(k)});
        }
        if (linhas.isEmpty()) { aviso(selecionado.nome + " ainda não possui locações."); return; }

        String[] colunas = {"Código", "Etapa", "Evento", "Retirada", "Devolução", "Total", "Situação"};
        JTable t = new JTable(new DefaultTableModel(linhas.toArray(new String[0][]), colunas) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        estilizarTabela(t, 12, 6);
        t.getColumnModel().getColumn(0).setPreferredWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(80);
        t.getColumnModel().getColumn(5).setPreferredWidth(90);
        t.getColumnModel().getColumn(6).setPreferredWidth(170);
        t.setPreferredScrollableViewportSize(new Dimension(720, Math.min(linhas.size(), 8) * ALTURA_LINHA));
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(CINZA_LINHA, 1));
        sp.getViewport().setBackground(BRANCO);

        JOptionPane.showOptionDialog(this, sp, "Histórico de locações — " + selecionado.nome,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Fechar"}, "Fechar");
    }

    private void autorizarPessoa() {
        if (selecionado == null) { aviso("Salve o cadastro do cliente antes de autorizar pessoas."); return; }

        TelaPrincipal.Campo nome = new TelaPrincipal.Campo("Nome", 13);
        TelaPrincipal.Campo documento = new TelaPrincipal.Campo("Documento (RG ou CPF)", 13);
        TelaPrincipal.Campo contato = new TelaPrincipal.Campo("Contato (WhatsApp)", 13);
        ((AbstractDocument) contato.getDocument()).setDocumentFilter(
                new FiltroMascara(contato, 11, TelaClientes::formatarTelefone));
        JComboBox<String> tipo = new JComboBox<>(TIPOS);
        TelaPrincipal.estilizarCombo(tipo);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 8));
        form.setPreferredSize(new Dimension(300, 4 * 34 + 3 * 8));
        form.add(nome);
        form.add(documento);
        form.add(contato);
        form.add(tipo);

        while (true) {
            int r = JOptionPane.showOptionDialog(this, form, "Autorizar pessoa — " + selecionado.nome,
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                    new Object[]{"Autorizar", "Cancelar"}, "Autorizar");
            if (r != 0) return;
            if (nome.getText().trim().isEmpty()) { aviso("Informe o nome da pessoa."); continue; }
            if (documento.getText().trim().isEmpty()) { aviso("Informe o documento da pessoa."); continue; }
            String tel = contato.getText().trim();
            if (!tel.isEmpty() && digitos(tel).length() < 10) { aviso("Contato incompleto. Informe o DDD e o número."); continue; }
            selecionado.autorizadas.add(new Cliente.Autorizada(nome.getText().trim(), documento.getText().trim(),
                    tel, (String) tipo.getSelectedItem()));
            atualizarAutorizadas();
            return;
        }
    }

    private void removerAutorizada(int linha) {
        if (selecionado == null || linha < 0 || linha >= selecionado.autorizadas.size()) return;
        Cliente.Autorizada a = selecionado.autorizadas.get(linha);
        int r = JOptionPane.showOptionDialog(this,
                "Remover " + a.nome + " das pessoas autorizadas de " + selecionado.nome + "?",
                "Remover pessoa autorizada", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Remover", "Cancelar"}, "Cancelar");
        if (r != 0) return;
        selecionado.autorizadas.remove(linha);
        atualizarAutorizadas();
    }

    private void alternarInadimplencia(Cliente c) {
        String pergunta = c.inadimplenteManual
                ? "Marcar " + c.nome + " como EM DIA? Novas locações deixam de exigir autorização."
                : "Marcar " + c.nome + " como INADIMPLENTE? Novas locações passarão a exigir autorização.\n"
                        + "(Descreva o motivo no campo Observações e clique em Salvar.)";
        if (JOptionPane.showConfirmDialog(this, pergunta, "Situação do cliente", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (c.inadimplenteManual) {
            c.inadimplenteManual = false;
            if (ctrl.inadimplente(c)) {
                aviso(c.nome + " continua inadimplente por ter locação atrasada.\n"
                        + "Registre a devolução no painel para regularizar.");
            }
        } else {
            c.inadimplenteManual = true;
        }
        atualizar();
    }

    private void excluir(Cliente c) {
        if (ctrl.temReservaEmAberto(c)) {
            aviso("Não é possível excluir " + c.nome + ": há orçamento ou locação em aberto.\n"
                    + "Conclua a devolução ou remova o cartão no painel primeiro.");
            return;
        }
        int r = JOptionPane.showOptionDialog(this,
                "Excluir o cliente " + c.nome + " (CPF " + c.cpf + ")?\n"
                        + "O cadastro e o histórico dele no painel serão removidos e não poderão ser recuperados.",
                "Excluir cliente", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Excluir", "Cancelar"}, "Cancelar");
        if (r != 0) return;
        if (c == selecionado) carregar(null);
        ctrl.removerCliente(c);      // remove do cadastro e do painel; recarrega esta lista
    }

    // ================= Menus do botão direito =================
    private void menuLista(MouseEvent e) {
        int linha = tabela.rowAtPoint(e.getPoint());
        if (linha < 0) return;
        Cliente c = modeloClientes.get(linha);
        if (c != selecionado) {
            tabela.setRowSelectionInterval(linha, linha);
            if (c != selecionado) return;       // o usuário não quis descartar a edição
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem hist = new JMenuItem("Histórico de locações");
        hist.addActionListener(x -> historico());
        JMenuItem sit = new JMenuItem(c.inadimplenteManual ? "Marcar como em dia" : "Marcar como inadimplente");
        sit.addActionListener(x -> alternarInadimplencia(c));
        JMenuItem exc = new JMenuItem("Excluir cliente");
        exc.addActionListener(x -> excluir(c));
        menu.add(hist);
        menu.add(sit);
        menu.addSeparator();
        menu.add(exc);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void menuAutorizadas(MouseEvent e) {
        int linha = tabelaAut.rowAtPoint(e.getPoint());
        if (linha < 0) return;
        tabelaAut.setRowSelectionInterval(linha, linha);
        JPopupMenu menu = new JPopupMenu();
        JMenuItem rem = new JMenuItem("Remover pessoa");
        rem.addActionListener(x -> removerAutorizada(linha));
        menu.add(rem);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    // ================= Configuração de campos =================
    private void configurarMascaras() {
        ((AbstractDocument) tfCpf.getDocument()).setDocumentFilter(
                new FiltroMascara(tfCpf, 11, TelaClientes::formatarCpf));
        ((AbstractDocument) tfWhats.getDocument()).setDocumentFilter(
                new FiltroMascara(tfWhats, 11, TelaClientes::formatarTelefone));
    }

    /** Qualquer digitação na ficha marca "alterações não salvas" e ativa o botão Salvar. */
    private void configurarAlteracoes() {
        DocumentListener dl = simples(() -> {
            if (carregando) return;
            sujo = true;
            estilizarSalvar();
            atualizarEstiloObs();
        });
        for (JTextComponent c : new JTextComponent[]{tfCpf, tfNome, tfRg, tfInstagram, tfWhats, tfEndereco, taObs}) {
            c.getDocument().addDocumentListener(dl);
        }
    }

    private void configurarSalvar() {
        btnSalvar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSalvar.setFocusPainted(false);
        btnSalvar.setContentAreaFilled(false);
        btnSalvar.setOpaque(true);
        btnSalvar.setForeground(BRANCO);
        btnSalvar.setBorder(new CompoundBorder(new LineBorder(CINZA_BORDA, 2), new EmptyBorder(0, 14, 0, 14)));
        btnSalvar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSalvar.addActionListener(e -> salvar());
        btnSalvar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { salvarHover = true; estilizarSalvar(); }
            public void mouseExited(MouseEvent e)  { salvarHover = false; estilizarSalvar(); }
        });
        estilizarSalvar();
    }

    /** Cinza = nada a salvar; preto = há alterações; cinza escuro ao passar o mouse com alterações. */
    private void estilizarSalvar() {
        Color fundo = !sujo ? new Color(0x8C8C8C) : (salvarHover ? new Color(0x444444) : PRETO);
        btnSalvar.setBackground(fundo);
        btnSalvar.setForeground(BRANCO);
        btnSalvar.setToolTipText(sujo ? "Salvar as alterações" : "Nenhuma alteração para salvar");
    }

    private void configurarObs() {
        taObs.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { atualizarEstiloObs(); taObs.repaint(); }
            public void focusLost(FocusEvent e)   { atualizarEstiloObs(); taObs.repaint(); }
        });
        MouseAdapter hover = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { obsHover = true; atualizarEstiloObs(); }
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), rolagemObs);
                if (rolagemObs.contains(p)) return;     // ainda dentro da caixa
                obsHover = false;
                atualizarEstiloObs();
            }
        };
        taObs.addMouseListener(hover);
        rolagemObs.addMouseListener(hover);
        atualizarEstiloObs();
    }

    /** Observações: cinza tracejado quando vazio, branco com borda sólida quando em uso. */
    private void atualizarEstiloObs() {
        boolean foco = taObs.isFocusOwner();
        boolean vazio = taObs.getText().isEmpty();
        if (!foco && vazio) {
            rolagemObs.getViewport().setBackground(obsHover ? new Color(0xD2D2D2) : new Color(0xE0E0E0));
            rolagemObs.setBorder(new TelaPrincipal.DashedBorder(obsHover ? PRETO : new Color(0x777777), 1));
        } else {
            rolagemObs.getViewport().setBackground(BRANCO);
            rolagemObs.setBorder(new LineBorder(foco || obsHover ? PRETO : CINZA_BORDA, foco ? 3 : 2));
        }
        rolagemObs.repaint();
    }

    // ================= Estilo das tabelas =================
    static void estilizarTabela(JTable t, int fonte, int recuo) {
        t.setRowHeight(ALTURA_LINHA);
        t.setShowGrid(true);
        t.setGridColor(CINZA_LINHA);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFont(new Font("SansSerif", Font.PLAIN, fonte));
        t.setForeground(PRETO);
        t.setBackground(BRANCO);
        t.setSelectionBackground(CINZA_SEL);
        t.setSelectionForeground(PRETO);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(false);
        t.setDefaultRenderer(Object.class, new CelulaRenderer(fonte, recuo));
        t.putClientProperty("hover", -1);

        JTableHeader cab = t.getTableHeader();
        cab.setReorderingAllowed(false);
        cab.setDefaultRenderer(new CabecalhoRenderer(fonte, recuo));
        cab.setPreferredSize(new Dimension(100, ALTURA_LINHA));

        // linha sob o mouse fica em destaque
        t.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int linha = t.rowAtPoint(e.getPoint());
                Object atual = t.getClientProperty("hover");
                if (!Integer.valueOf(linha).equals(atual)) {
                    t.putClientProperty("hover", linha);
                    t.setCursor(Cursor.getPredefinedCursor(linha >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    t.repaint();
                }
            }
        });
        t.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                t.putClientProperty("hover", -1);
                t.repaint();
            }
        });
    }

    static Color corLinha(JTable t, boolean selecionada, int linha) {
        if (selecionada) return CINZA_SEL;
        Object h = t.getClientProperty("hover");
        return (h instanceof Integer && (Integer) h == linha) ? CINZA_HOVER : BRANCO;
    }

    static class CelulaRenderer extends DefaultTableCellRenderer {
        private final int fonte, recuo;

        CelulaRenderer(int fonte, int recuo) { this.fonte = fonte; this.recuo = recuo; }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int linha, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, linha, col);
            l.setFont(new Font("SansSerif", Font.PLAIN, fonte));
            l.setForeground(PRETO);
            l.setBackground(corLinha(t, sel, linha));
            l.setBorder(new EmptyBorder(0, recuo, 0, recuo));
            return l;
        }
    }

    static class CabecalhoRenderer extends DefaultTableCellRenderer {
        private final int fonte, recuo;

        CabecalhoRenderer(int fonte, int recuo) { this.fonte = fonte; this.recuo = recuo; }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int linha, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, linha, col);
            l.setFont(new Font("SansSerif", Font.BOLD, fonte));
            l.setForeground(PRETO);
            l.setBackground(CINZA_CAB);
            l.setOpaque(true);
            l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 1, CINZA_LINHA), new EmptyBorder(0, recuo, 0, recuo)));
            return l;
        }
    }

    /** Situação em "etiqueta": cinza para Em dia, preta com letras brancas para Inadimplente. */
    static class RenderSituacao extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int linha, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            p.setBackground(corLinha(t, sel, linha));
            boolean inad = "Inadimplente".equals(v);
            JLabel chip = TelaPrincipal.rotulo(String.valueOf(v), Font.BOLD, 11);
            chip.setOpaque(true);
            chip.setBackground(inad ? PRETO : CINZA_BTN);
            chip.setForeground(inad ? BRANCO : PRETO);
            chip.setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 1), new EmptyBorder(0, 6, 0, 6)));
            p.add(chip);
            return p;
        }
    }

    // ================= Modelos das tabelas =================
    class ModeloClientes extends AbstractTableModel {
        private final String[] colunas = {"CPF", "Nome", "WhatsApp", "Situação"};

        public int getRowCount() { return visiveis.size(); }
        public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int c) { return colunas[c]; }

        public Object getValueAt(int linha, int col) {
            Cliente c = visiveis.get(linha);
            switch (col) {
                case 0:  return c.cpf;
                case 1:  return c.nome;
                case 2:  return c.whatsapp;
                default: return ctrl.inadimplente(c) ? "Inadimplente" : "Em dia";
            }
        }

        Cliente get(int linha) { return visiveis.get(linha); }
    }

    class ModeloAutorizadas extends AbstractTableModel {
        private final String[] colunas = {"Nome", "Documento", "Contato", "Tipo"};

        public int getRowCount() { return selecionado == null ? 0 : selecionado.autorizadas.size(); }
        public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int c) { return colunas[c]; }

        public Object getValueAt(int linha, int col) {
            Cliente.Autorizada a = selecionado.autorizadas.get(linha);
            switch (col) {
                case 0:  return a.nome;
                case 1:  return a.documento;
                case 2:  return a.contato;
                default: return a.tipo;
            }
        }
    }

    // ================= Máscaras e validação =================
    static String digitos(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) if (Character.isDigit(c)) sb.append(c);
        return sb.toString();
    }

    static String formatarCpf(String d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            sb.append(d.charAt(i));
            boolean temMais = i < d.length() - 1;
            if ((i == 2 || i == 5) && temMais) sb.append('.');
            else if (i == 8 && temMais) sb.append('-');
        }
        return sb.toString();
    }

    static String formatarTelefone(String d) {
        if (d.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("(").append(d, 0, Math.min(2, d.length()));
        if (d.length() <= 2) return sb.toString();
        sb.append(") ");
        String resto = d.substring(2);
        int corte = d.length() > 10 ? 5 : 4;          // celular (9 dígitos) ou fixo (8 dígitos)
        if (resto.length() <= corte) return sb.append(resto).toString();
        return sb.append(resto, 0, corte).append('-').append(resto.substring(corte)).toString();
    }

    /** Confere os dois dígitos verificadores do CPF. */
    static boolean cpfValido(String cpf) {
        String d = digitos(cpf);
        if (d.length() != 11) return false;
        boolean todosIguais = true;
        for (int i = 1; i < 11; i++) if (d.charAt(i) != d.charAt(0)) { todosIguais = false; break; }
        if (todosIguais) return false;
        return digitoVerificador(d.substring(0, 9)) == d.charAt(9) - '0'
                && digitoVerificador(d.substring(0, 10)) == d.charAt(10) - '0';
    }

    private static int digitoVerificador(String base) {
        int soma = 0;
        for (int i = 0; i < base.length(); i++) soma += (base.charAt(i) - '0') * (base.length() + 1 - i);
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Aceita só números e reaplica a máscara (CPF, telefone) a cada digitação, mantendo o cursor. */
    static class FiltroMascara extends DocumentFilter {
        private final JTextComponent campo;
        private final int maxDigitos;
        private final Function<String, String> formato;

        FiltroMascara(JTextComponent campo, int maxDigitos, Function<String, String> formato) {
            this.campo = campo;
            this.maxDigitos = maxDigitos;
            this.formato = formato;
        }

        @Override
        public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException {
            aplicar(fb, off, 0, s, a);
        }

        @Override
        public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException {
            aplicar(fb, off, len, s, a);
        }

        @Override
        public void remove(FilterBypass fb, int off, int len) throws BadLocationException {
            aplicar(fb, off, len, "", null);
        }

        private void aplicar(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException {
            Document d = fb.getDocument();
            String atual = d.getText(0, d.getLength());
            String antes = atual.substring(0, off) + (s == null ? "" : s);
            String novo = antes + atual.substring(off + len);

            String dig = digitos(novo);
            if (dig.length() > maxDigitos) dig = dig.substring(0, maxDigitos);
            int digitosAteCursor = Math.min(digitos(antes).length(), dig.length());
            String f = formato.apply(dig);
            fb.replace(0, d.getLength(), f, a);

            int pos = 0, cont = 0;
            while (pos < f.length() && cont < digitosAteCursor) {
                if (Character.isDigit(f.charAt(pos))) cont++;
                pos++;
            }
            campo.setCaretPosition(Math.min(pos, f.length()));
        }
    }

    private static DocumentListener simples(Runnable acao) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { acao.run(); }
            public void removeUpdate(DocumentEvent e)  { acao.run(); }
            public void changedUpdate(DocumentEvent e) { acao.run(); }
        };
    }

    // ================= Componentes auxiliares =================

    /** Campo de observações com a dica "Observações" centralizada quando vazio. */
    static class AreaObs extends JTextArea {
        private final String dica;

        AreaObs(String dica) {
            this.dica = dica;
            setFont(new Font("SansSerif", Font.PLAIN, 12));
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(false);
            setForeground(PRETO);
            setCaretColor(PRETO);
            setSelectionColor(CINZA_SEL);
            setSelectedTextColor(PRETO);
            setBorder(new EmptyBorder(6, 8, 6, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(0x666666));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(dica, (getWidth() - fm.stringWidth(dica)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        }
    }

    /** Coluna da direita: acompanha a largura da janela e rola na vertical se faltar altura. */
    static class PainelRolavel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int orientacao, int direcao) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int orientacao, int direcao) { return 64; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Coloca o componente no topo com a altura que ele precisa (até o limite do espaço disponível). */
    static class TopoLayout implements LayoutManager {
        public void addLayoutComponent(String nome, Component c) { }
        public void removeLayoutComponent(Component c) { }

        public Dimension preferredLayoutSize(Container pai) {
            return pai.getComponentCount() == 0 ? new Dimension() : pai.getComponent(0).getPreferredSize();
        }

        public Dimension minimumLayoutSize(Container pai) { return new Dimension(0, 0); }

        public void layoutContainer(Container pai) {
            if (pai.getComponentCount() == 0) return;
            Insets in = pai.getInsets();
            int largura = pai.getWidth() - in.left - in.right;
            int altura = pai.getHeight() - in.top - in.bottom;
            Component c = pai.getComponent(0);
            c.setBounds(in.left, in.top, largura, Math.min(c.getPreferredSize().height, altura));
        }
    }
}