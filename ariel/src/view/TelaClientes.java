package view;

import controller.ClienteController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Tela "Clientes" (visual do DESIGN.md), aberta pelo menu lateral da TelaPrincipal.
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

    private static final String[] FILTROS = ClienteController.FILTROS;
    private static final String[] TIPOS = ClienteController.TIPOS_AUTORIZACAO;
    static final int FONTE_TABELA = 13, RECUO_TABELA = 10;   // DESIGN.md 6.6
    private static final int ALTURA_LINHA = 36;          // linha de tabela (texto 14 px + 12 px de respiro)
    private static final int LARGURA_LATERAL = 440;

    private final TelaPrincipal app;          // janela principal (só para navegar entre telas)
    private final ClienteController ctrl;     // CONTROLLER: validação, cadastro, inadimplência, inativação
    private final List<Cliente> visiveis = new ArrayList<>();

    // ----- lista -----
    private final ModeloClientes modeloClientes = new ModeloClientes();
    private final JTable tabela = new JTable(modeloClientes) {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() != null && getPreferredSize().width < getParent().getWidth();
        }
    };
    private final JScrollPane rolagemLista = new JScrollPane(tabela);
    private final TelaPrincipal.Campo busca = new TelaPrincipal.Campo("Buscar por nome ou CPF", 12);
    private final JComboBox<String> filtro = new JComboBox<>(FILTROS);

    // ----- ficha -----
    private final JLabel tituloFicha = TelaPrincipal.rotulo("FICHA DO CLIENTE", Font.BOLD, 12);
    private final JPanel alertaBox = new JPanel(new BorderLayout());
    private final TelaPrincipal.Campo tfCpf = new TelaPrincipal.Campo("000.000.000-00", 13);
    private final TelaPrincipal.Campo tfNome = new TelaPrincipal.Campo("Nome e sobrenome", 13);
    private final TelaPrincipal.Campo tfRg = new TelaPrincipal.Campo("Opcional", 13);
    private final TelaPrincipal.Campo tfInstagram = new TelaPrincipal.Campo("@usuario", 13);
    private final TelaPrincipal.Campo tfWhats = new TelaPrincipal.Campo("(00) 00000-0000", 13);
    private final TelaPrincipal.Campo tfEndereco = new TelaPrincipal.Campo("Rua, número, bairro, cidade", 13);
    private final AreaObs taObs = new AreaObs("");
    private final JScrollPane rolagemObs = new JScrollPane(taObs);
    private final JButton btnHistorico = TelaPrincipal.botaoCinza("Histórico", 12);
    private final Tema.Botao btnSalvar = new Tema.Botao("Salvar");
    private final JButton btnSituacao = TelaPrincipal.botaoCinza("Marcar como inadimplente", 12);   // alterna Em dia / Inadimplente (manual)
    private final JButton btnInativar = TelaPrincipal.botaoNeutro("Inativar cliente", 12);          // alterna Ativo / Inativo

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

    public TelaClientes(TelaPrincipal app, ClienteController ctrl) {
        super(new BorderLayout());
        this.app = app;
        this.ctrl = ctrl;
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(24, 28, 24, 28));

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
        JPanel cab = new JPanel(new BorderLayout(12, 6));
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Clientes", Font.BOLD, 30), BorderLayout.WEST);

        JPanel direita = new JPanel(new Ui.WrapLayout(FlowLayout.RIGHT, 10, 4));
        direita.setOpaque(false);

        busca.setPreferredSize(new Dimension(210, Tema.ALTURA));
        busca.getDocument().addDocumentListener(simples(this::recarregarLista));
        direita.add(busca);

        TelaPrincipal.estilizarCombo(filtro);
        filtro.setPreferredSize(new Dimension(135, Tema.ALTURA));
        filtro.addActionListener(e -> recarregarLista());
        direita.add(filtro);

        JButton novo = TelaPrincipal.botaoPrimario("+ Novo cliente", 13);
        novo.addActionListener(e -> novoCliente());
        direita.add(novo);

        cab.add(direita, BorderLayout.CENTER);
        return cab;
    }

    // ================= Corpo: lista (esquerda) + ficha e autorizadas (direita) =================
    private JComponent criarCorpo() {
        JComponent lista = criarLista();
        JComponent lateral = criarLateral();
        lista.setPreferredSize(new Dimension(560, 400));
        lista.setMinimumSize(new Dimension(340, 200));
        lateral.setMinimumSize(new Dimension(340, 200));
        return Ui.divisor(lista, lateral, 0.56);
    }

    // ---------- LISTA ----------
    private JComponent criarLista() {
        JPanel lista = new JPanel(new BorderLayout());
        lista.setBackground(BRANCO);
        lista.setBorder(new Tema.Arredondada(Tema.BORDA_SECAO, 1, Tema.RAIO_SECAO, 8, 10));

        estilizarTabela(tabela, 12, 6);
        tabela.setFillsViewportHeight(true);
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        int[] minLarg = {120, 140, 125, 118};
        int[] prefLarg = {130, 190, 135, 122};
        for (int i = 0; i < minLarg.length; i++) {
            TableColumn col = tabela.getColumnModel().getColumn(i);
            col.setMinWidth(minLarg[i]);
            col.setPreferredWidth(prefLarg[i]);
        }
        tabela.getColumnModel().getColumn(3).setCellRenderer(new RenderSituacao());
        rolagemLista.setBorder(null);
        rolagemLista.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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

        // tecla Delete inativa o cliente selecionado (nada é excluído)
        tabela.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "inativarCliente");
        tabela.getActionMap().put("inativarCliente", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (selecionado != null && selecionado.ativo) alternarAtivo(selecionado); }   // excluir/desativar
        });

        return lista;
    }

    // ---------- Coluna da direita ----------
    private JComponent criarLateral() {
        PainelRolavel coluna = new PainelRolavel();
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));
        coluna.setBackground(Tema.FUNDO);
        coluna.add(criarFicha());
        coluna.add(Box.createVerticalStrut(16));
        coluna.add(criarAutorizadas());

        JScrollPane sp = new JScrollPane(coluna, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setBackground(Tema.FUNDO);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setPreferredSize(new Dimension(LARGURA_LATERAL, 100));
        sp.setMinimumSize(new Dimension(340, 100));
        return sp;
    }

    // ---------- FICHA DO CLIENTE ----------
    private JComponent criarFicha() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BRANCO);
        p.setBorder(new Tema.Arredondada(Tema.BORDA_SECAO, 1, Tema.RAIO_SECAO, 14, 16));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        tituloFicha.setFont(Tema.fonteSecao());
        tituloFicha.setForeground(CINZA_TEXTO);
        tituloFicha.setAlignmentX(Component.LEFT_ALIGNMENT);
        tituloFicha.setBorder(new EmptyBorder(0, 0, 10, 0));
        p.add(tituloFicha);

        // alerta (só aparece para cliente inadimplente)
        JLabel texto = new JLabel("<html><b>Cliente inadimplente:</b> nova locação exige autorização.</html>");
        texto.setFont(Tema.fonte(Font.PLAIN, 13));
        texto.setForeground(Tema.ERRO);
        JPanel caixa = new JPanel(new BorderLayout());
        caixa.setBackground(Tema.ERRO_BG);
        caixa.setBorder(new Tema.Arredondada(Tema.ERRO_BG, 1, Tema.RAIO_CONTROLE, 8, 12));
        caixa.add(texto, BorderLayout.CENTER);
        alertaBox.setOpaque(false);
        alertaBox.setBorder(new EmptyBorder(0, 0, 10, 0));
        alertaBox.add(caixa, BorderLayout.CENTER);
        alertaBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        alertaBox.setVisible(false);
        p.add(alertaBox);

        // campos com o rótulo ACIMA (DESIGN.md 6.2), em duas colunas como no protótipo
        p.add(linhaCampos(Ui.bloco("CPF *", tfCpf), Ui.bloco("RG", tfRg)));
        p.add(Box.createVerticalStrut(12));
        p.add(linhaCampos(Ui.bloco("Nome completo *", tfNome)));
        p.add(Box.createVerticalStrut(12));
        p.add(linhaCampos(Ui.bloco("Telefone / WhatsApp", tfWhats), Ui.bloco("Instagram", tfInstagram)));
        p.add(Box.createVerticalStrut(12));
        p.add(linhaCampos(Ui.bloco("Endereço completo *", tfEndereco)));
        p.add(Box.createVerticalStrut(12));

        rolagemObs.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel obs = Ui.bloco("Observações", rolagemObs);
        rolagemObs.setPreferredSize(new Dimension(100, 60));
        obs.setAlignmentX(Component.LEFT_ALIGNMENT);
        obs.setMaximumSize(new Dimension(Integer.MAX_VALUE, obs.getPreferredSize().height));
        p.add(obs);
        p.add(Box.createVerticalStrut(16));

        btnSituacao.setToolTipText("A inadimplência é decidida por você; o sistema só alerta (descreva o motivo em Observações).");
        btnSituacao.addActionListener(e -> { if (selecionado != null) alternarInadimplencia(selecionado); });
        btnInativar.setToolTipText("Inativa o cliente (preserva o histórico; pode ser reativado no filtro Inativos).");
        btnInativar.addActionListener(e -> { if (selecionado != null) inativarOuReativar(selecionado); });
        btnHistorico.addActionListener(e -> historico());
        btnSalvar.setPreferredSize(new Dimension(110, Tema.ALTURA));
        JPanel botoes = new JPanel(new Ui.WrapLayout(FlowLayout.RIGHT, 8, 8));
        botoes.setOpaque(false);
        botoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        botoes.add(btnSituacao);
        botoes.add(btnInativar);
        botoes.add(btnHistorico);
        botoes.add(btnSalvar);
        p.add(botoes);
        return p;
    }

    /** Uma linha do formulário: um bloco ocupa tudo; dois dividem a largura (12 px entre eles). */
    private static JPanel linhaCampos(JPanel... blocos) {
        JPanel l = new JPanel(new GridLayout(1, blocos.length, 12, 0));
        l.setOpaque(false);
        for (JPanel b : blocos) l.add(b);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height));
        return l;
    }

    // ---------- PESSOAS AUTORIZADAS ----------
    private JComponent criarAutorizadas() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BRANCO);
        p.setBorder(new Tema.Arredondada(Tema.BORDA_SECAO, 1, Tema.RAIO_SECAO, 14, 16));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = TelaPrincipal.rotulo("PESSOAS AUTORIZADAS", Font.BOLD, 12);
        t.setFont(Tema.fonteSecao());
        t.setForeground(CINZA_TEXTO);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(new EmptyBorder(0, 0, 6, 0));
        p.add(t);

        estilizarTabela(tabelaAut, 11, 4);
        tabelaAut.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabelaAut.getColumnModel().getColumn(1).setPreferredWidth(95);
        tabelaAut.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabelaAut.getColumnModel().getColumn(3).setPreferredWidth(85);
        rolagemAut.setBorder(null);
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

        JButton autorizar = TelaPrincipal.botaoSuave("+ Autorizar pessoa", 13);
        autorizar.setAlignmentX(Component.LEFT_ALIGNMENT);
        autorizar.setPreferredSize(new Dimension(100, Tema.ALTURA));
        autorizar.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTURA));
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
            visiveis.clear();
            visiveis.addAll(ctrl.buscar(busca.getText(), (String) filtro.getSelectedItem()));   // filtro e ordem: controller

            modeloClientes.fireTableDataChanged();
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
            tituloFicha.setText(c == null ? "FICHA DO CLIENTE · NOVO" : c.ativo ? "FICHA DO CLIENTE" : "FICHA DO CLIENTE · INATIVO");
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
        btnInativar.setEnabled(selecionado != null);
        btnInativar.setText(selecionado != null && !selecionado.ativo ? "Reativar cliente" : "Inativar cliente");
        Container pai = btnSituacao.getParent();
        if (pai != null) { pai.revalidate(); pai.repaint(); }
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

        Cliente c;
        try {
            c = ctrl.salvar(selecionado, tfCpf.getText(), tfNome.getText(), tfRg.getText(), tfInstagram.getText(),
                    tfWhats.getText(), tfEndereco.getText(), taObs.getText());
        } catch (ValidacaoException e) {
            aviso(e.getMessage());
            campo(e.campo).requestFocusInWindow();
            return;
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
            return;
        }

        carregar(c);             // recarrega a ficha já salva
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
                    TelaPrincipal.moeda(k.total()), ctrl.situacaoLocacao(k)});
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
        sp.setBorder(new Tema.Arredondada(Tema.BORDA_SECAO, 1, Tema.RAIO_CONTROLE, 4, 4));
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
            try {
                ctrl.autorizar(selecionado, nome.getText(), documento.getText(), contato.getText(), (String) tipo.getSelectedItem());
            } catch (RegraNegocioException e) {
                aviso(e.getMessage());
                continue;
            }
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
        ctrl.removerAutorizada(selecionado, a);
        atualizarAutorizadas();
    }

    private void alternarInadimplencia(Cliente c) {
        String pergunta = c.inadimplenteManual
                ? "Marcar " + c.nome + " como EM DIA? Novas locações deixam de exigir autorização."
                : "Marcar " + c.nome + " como INADIMPLENTE? Novas locações passarão a exigir autorização.\n"
                        + "(Descreva o motivo no campo Observações e clique em Salvar.)";
        if (JOptionPane.showConfirmDialog(this, pergunta, "Situação do cliente", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ctrl.alternarInadimplencia(c);
        atualizar();
    }

    private void inativarOuReativar(Cliente c) {
        if (!c.ativo) {
            ctrl.reativar(c);
            carregar(c);
            atualizar();
            aviso(c.nome + " foi reativado e volta a aparecer para novos orçamentos.");
            return;
        }
        int r = JOptionPane.showOptionDialog(this,
                "Inativar o cliente " + c.nome + " (CPF " + c.cpf + ")?\n"
                        + "O cadastro deixará de aparecer para novos orçamentos, mas o histórico é mantido e você pode reativá-lo no filtro \"Inativos\".",
                "Inativar cliente", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Inativar", "Cancelar"}, "Cancelar");
        if (r != 0) return;
        ctrl.desativar(c);
        carregar(null);
        atualizar();
        aviso(c.nome + " foi inativado.");
    }

    /**
     * UC03 FA03 — Excluir ou desativar: o controller verifica vínculos. Sem vínculos exclui (MSG005);
     * com vínculos desativa, preservando o histórico (MSG006). Desativado pode ser reativado (filtro "Inativos").
     */
    private void alternarAtivo(Cliente c) {
        if (!c.ativo) {
            ctrl.reativar(c);
            aviso(c.nome + " foi reativado e volta a aparecer para novos orçamentos.");
            return;
        }
        String detalhe = ctrl.temVinculos(c)
                ? "Ele possui orçamentos/locações: não será apagado, e sim DESATIVADO (o histórico é mantido)."
                : "Ele não possui orçamentos nem locações: o cadastro será excluído.";
        int r = JOptionPane.showOptionDialog(this,
                "Excluir o cliente " + c.nome + " (CPF " + c.cpf + ")?\n" + detalhe,
                "Excluir cliente", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Excluir", "Cancelar"}, "Cancelar");
        if (r != 0) return;
        boolean excluido = ctrl.excluirOuDesativar(c);
        if (c == selecionado) carregar(null);
        atualizar();
        aviso(excluido ? Mensagens.MSG005 : Mensagens.MSG006);
    }

    /** Campo da ficha correspondente ao nome usado pelo controller na ValidacaoException. */
    private JComponent campo(String nome) {
        switch (nome == null ? "" : nome) {
            case ClienteController.CAMPO_CPF:      return tfCpf;
            case ClienteController.CAMPO_NOME:     return tfNome;
            case ClienteController.CAMPO_WHATS:    return tfWhats;
            case ClienteController.CAMPO_ENDERECO: return tfEndereco;
            default:                               return tfCpf;
        }
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
        JMenuItem exc = new JMenuItem(c.ativo ? "Excluir cliente" : "Reativar cliente");
        exc.addActionListener(x -> alternarAtivo(c));
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
        btnSalvar.setFont(Tema.fonte(Font.BOLD, 12));
        btnSalvar.setFocusPainted(false);
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setBorder(new EmptyBorder(0, 14, 0, 14));
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
        Color fundo = !sujo ? Tema.PRIMARIA_CLARA : (salvarHover ? Tema.PRIMARIA_ESCURA : Tema.PRIMARIA);
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

    /** Observações: mesmo contorno dos campos (borda de campo; 2 px rosa no foco). */
    private void atualizarEstiloObs() {
        boolean foco = taObs.isFocusOwner();
        rolagemObs.getViewport().setBackground(BRANCO);
        rolagemObs.setBorder(new Tema.Arredondada(foco ? Tema.PRIMARIA : Tema.BORDA_CAMPO, foco ? 2 : 1, Tema.RAIO_CONTROLE, 2, 2));
        rolagemObs.repaint();
    }

    // ================= Estilo das tabelas =================
    static void estilizarTabela(JTable t, int fonte, int recuo) {
        t.setRowHeight(ALTURA_LINHA);
        t.setShowHorizontalLines(true);                  // só divisória entre linhas: sem grade vertical
        t.setShowVerticalLines(false);
        t.setGridColor(Tema.LINHA_TABELA);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFont(Tema.fonte(Font.PLAIN, FONTE_TABELA));
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
        cab.setBackground(BRANCO);
        cab.setBorder(null);

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
            l.setFont(Tema.fonte(Font.PLAIN, FONTE_TABELA));
            l.setForeground(PRETO);
            l.setBackground(corLinha(t, sel, linha));
            l.setBorder(new EmptyBorder(0, RECUO_TABELA, 0, RECUO_TABELA));
            return l;
        }
    }

    static class CabecalhoRenderer extends DefaultTableCellRenderer {
        private final int fonte, recuo;

        CabecalhoRenderer(int fonte, int recuo) { this.fonte = fonte; this.recuo = recuo; }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int linha, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, linha, col);
            l.setFont(Tema.fonte(Font.BOLD, 12));            // cabeçalho: 12 px, texto suave, linha embaixo
            l.setForeground(CINZA_TEXTO);
            l.setBackground(BRANCO);
            l.setOpaque(true);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Tema.BORDA_SECAO), new EmptyBorder(0, RECUO_TABELA, 0, RECUO_TABELA)));
            return l;
        }
    }

    /** Situação em selo (DESIGN.md 3.5): neutro para "Em dia"/"Inativo", urgente para "Inadimplente". */
    static class RenderSituacao extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int linha, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            p.setBackground(corLinha(t, sel, linha));
            JLabel chip = "Inadimplente".equals(v) ? Tema.Selo.urgente("Inadimplente") : Tema.Selo.neutro(String.valueOf(v));
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
                default: return ctrl.situacao(c);
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
            setFont(Tema.fonte(Font.PLAIN, 13));
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
                g2.setColor(Tema.TEXTO_SUAVE);
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