package view;

import controller.CatalogoController;
import controller.PainelController;
import controller.RelatorioController;
import controller.UsuarioController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * VIEW: janela principal - Ariel · Conto de Fadas (Swing, preto e branco).
 *
 * Estrutura MVC do projeto:
 *   model       -> dados (Cliente, Locacao, Usuario...), sem nada de tela
 *   view        -> telas (esta classe e as outras Tela*): só desenham e reagem a cliques
 *   controller  -> regras de negócio (PainelController, UsuarioController, LoginController)
 *
 * Telas (menu lateral):
 *  - Painel        : quadro de orçamentos / locados / devolvidos (cartões arrastáveis)
 *  - Clientes      : TelaClientes
 *  - Catálogo      : TelaCatalogo (produtos, pacotes, temas e categorias, serviços)
 *  - Orçamentos    : TelaOrcamentos (lista e formulário "Novo orçamento")
 *  - Locações      : TelaLocacoes (lista, detalhe, quitar saldo, contrato PDF)
 *  - Relatórios    : TelaRelatorios (só administrador; exporta CSV e PDF)
 *  - Devoluções    : TelaDevolucoes (conferência, atraso, dano/perda)
 *  - Configurações : TelaConfiguracoes (meus dados; administrador cria usuários)
 */
public class TelaPrincipal extends JFrame {

    // ===== Paleta (preto, branco e tons de cinza) =====
    static final Color PRETO       = Color.BLACK;
    static final Color BRANCO      = Color.WHITE;
    static final Color BORDA_CLARA = new Color(0xE3E3E3);   // moldura clara da janela
    static final Color BORDA_FINA  = new Color(0xBDBDBD);
    static final Color CINZA_SIDE  = new Color(0xEEEEEE);
    static final Color CINZA_COL   = new Color(0xF5F5F5);
    static final Color CINZA_CAB   = new Color(0xE8E8E8);
    static final Color CINZA_BTN   = new Color(0xD9D9D9);
    static final Color CINZA_SEL   = new Color(0xCCCCCC);
    static final Color CINZA_HOVER = new Color(0xEBEBEB);
    static final Color CINZA_TEXTO = new Color(0x555555);
    static final Color CINZA_LINHA = new Color(0xC8C8C8);
    static final Color CINZA_BORDA = new Color(0x555555);

    // ===== Datas e moeda (padrão brasileiro) =====
    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    static final DateTimeFormatter FMT_CURTA = DateTimeFormatter.ofPattern("dd/MM");
    static final DateTimeFormatter FMT_COMPLETA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FMT_EXTENSO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", PT_BR);
    static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(PT_BR);

    // Barras de rolagem em cinza (o padrão do Java é azulado e quebraria o preto e branco)
    static {
        javax.swing.plaf.ColorUIResource cinza = new javax.swing.plaf.ColorUIResource(0xBDBDBD);
        UIManager.put("ScrollBarUI", "javax.swing.plaf.basic.BasicScrollBarUI");
        UIManager.put("ScrollBar.thumb", cinza);
        UIManager.put("ScrollBar.thumbShadow", new javax.swing.plaf.ColorUIResource(0x777777));
        UIManager.put("ScrollBar.thumbHighlight", new javax.swing.plaf.ColorUIResource(0xF2F2F2));
        UIManager.put("ScrollBar.thumbDarkShadow", new javax.swing.plaf.ColorUIResource(0x000000));
        UIManager.put("ScrollBar.track", new javax.swing.plaf.ColorUIResource(0xEEEEEE));
        UIManager.put("ScrollBar.trackHighlight", new javax.swing.plaf.ColorUIResource(0xDDDDDD));
        UIManager.put("ScrollBar.width", 14);
    }

    static final String[] MENU = {"Painel", "Clientes", "Catálogo", "Orçamentos",
            "Locações", "Devoluções", "Relatórios", "Configurações"};
    static final String[] NOMES_COLUNAS = {"ORÇAMENTO", "LOCADO", "DEVOLVIDO"};
    static final String[] TITULOS_STATS = {"RESERVAS NA SEMANA", "PAGAMENTOS PENDENTES",
            "ORÇAMENTOS A VENCER", "DEVOLUÇÕES PREVISTAS"};
    static final String[] CRITERIOS_STATS = {
            "Orçamentos com entrada paga e evento nos próximos 7 dias, mais locações ativas nesse período.",
            "Orçamentos aguardando entrada e locações com saldo a receber.",
            "Orçamentos sem entrada que vencem nos próximos 7 dias.",
            "Locações com devolução prevista até 7 dias (inclui as atrasadas)."};

    // ===== Ligações MVC =====
    private final PainelController ctrl;      // CONTROLLER do painel: dados + regras de negócio
    private final UsuarioController usuarios; // CONTROLLER dos usuários (usado em Configurações)
    private final Usuario usuario;            // quem entrou no sistema (veio do login)
    private final Runnable aoSair;            // o que fazer ao clicar em "Sair" (voltar para o login)

    // ===== Componentes da tela =====
    private final Coluna[] colunas = new Coluna[3];
    private final JLabel[] numerosStats = new JLabel[4];
    private final JButton[] botoesMenu = new JButton[MENU.length];
    private final JLabel tituloPagina = new JLabel("Painel principal");
    private final JLabel tituloJanela = rotulo("Ariel · Conto de Fadas  —  Painel principal", Font.PLAIN, 12);
    private final JLabel lblDiaSemana = rotulo(" ", Font.BOLD, 14);
    private final JLabel lblDataHoje = rotulo(" ", Font.PLAIN, 11);
    private final CardLayout cartas = new CardLayout();      // mostra uma "página" por vez (uma por item do menu)
    private final JPanel paginas = new JPanel(cartas);
    private final Ghost vidro = new Ghost();                 // camada transparente onde o cartão arrastado é desenhado
    private final Map<Locacao, Card> cartoes = new HashMap<>(); // um cartão (componente) para cada locação
    private TelaClientes telaClientes;
    private TelaLocacoes telaLocacoes;
    private TelaDevolucoes telaDevolucoes;
    private TelaCatalogo telaCatalogo;
    private TelaOrcamentos telaOrcamentos;
    private TelaRelatorios telaRelatorios;
    private final CatalogoController catalogo;       // CONTROLLER do catálogo
    private final RelatorioController relatorios;    // CONTROLLER dos relatórios
    private TelaConfiguracoes telaConfig;
    private JLabel logado;                // "nome / perfil" no rodapé do menu
    private javax.swing.Timer relogio;
    private int menuSelecionado = 0;
    private String filtro = "";
    private Point arrasteJanela;
    private boolean telaCheia = false;          // modo tela cheia (F11)
    private Rectangle limitesAntes;             // tamanho/posição da janela antes de entrar em tela cheia
    private JButton btnTelaCheia;

    public TelaPrincipal(PainelController ctrl, UsuarioController usuarios, Usuario usuario, Runnable aoSair) {
        super("Ariel · Conto de Fadas");
        this.ctrl = ctrl;
        this.usuarios = usuarios;
        this.usuario = usuario;
        this.aoSair = aoSair;
        this.catalogo = new CatalogoController(ctrl);
        this.relatorios = new RelatorioController(ctrl);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setGlassPane(vidro);
        vidro.setVisible(false);

        // moldura clara em volta da tela
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(BORDA_CLARA);
        raiz.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDA_FINA, 1),
                new EmptyBorder(0, 6, 6, 6)));
        setContentPane(raiz);

        raiz.add(criarBarraTitulo(), BorderLayout.NORTH);

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.setBackground(BRANCO);
        conteudo.setBorder(new LineBorder(PRETO, 2));
        conteudo.add(criarSidebar(), BorderLayout.WEST);
        conteudo.add(criarPaginas(), BorderLayout.CENTER);
        raiz.add(conteudo, BorderLayout.CENTER);

        // sempre que o controller avisar que algo mudou, redesenhamos a tela
        ctrl.addOuvinte(this::atualizarTudo);
        atualizarTudo();

        setSize(1120, 720);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        // ESC: em tela cheia só sai dela; na janela normal fecha a tela
        getRootPane().registerKeyboardAction(e -> { if (telaCheia) alternarTelaCheia(); else dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // F11: entra/sai da tela cheia (funciona em qualquer tela do sistema)
        getRootPane().registerKeyboardAction(e -> alternarTelaCheia(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Confere a data a cada 30 s: quando o dia vira, tudo é recalculado sozinho
        relogio = new javax.swing.Timer(30_000, e -> verificarVirada());
        relogio.start();
    }

    @Override
    public void dispose() {
        if (relogio != null) relogio.stop();
        super.dispose();
    }

    private void verificarVirada() {
        if (vidro.isVisible()) return;          // não mexe no meio de um arraste
        if (ctrl.verificarVirada()) atualizarTudo();
    }

    /** Atualiza o nome mostrado no menu (chamado quando o usuário altera os próprios dados). */
    private void atualizarUsuario() {
        logado.setText("<html>" + usuario.nome() + "<br><span style='font-weight:normal'>"
                + usuario.perfil() + "</span></html>");
    }

    /** Sair: fecha esta janela e volta para a tela de login. */
    private void sair() {
        dispose();
        aoSair.run();
    }

    // ================= Navegação entre telas =================
    private JComponent criarPaginas() {
        paginas.setBackground(BRANCO);
        telaClientes = new TelaClientes(this, ctrl);
        telaLocacoes = new TelaLocacoes(this, ctrl);
        telaDevolucoes = new TelaDevolucoes(this, ctrl, usuario);
        telaCatalogo = new TelaCatalogo(this, ctrl, catalogo);
        telaOrcamentos = new TelaOrcamentos(this, ctrl);
        telaRelatorios = new TelaRelatorios(this, relatorios, usuario);
        telaConfig = new TelaConfiguracoes(usuarios, usuario, this::atualizarUsuario);
        for (int i = 0; i < MENU.length; i++) {
            JComponent pagina;
            switch (i) {
                case 0:  pagina = criarAreaPrincipal(); break;
                case 1:  pagina = telaClientes; break;
                case 2:  pagina = telaCatalogo; break;
                case 3:  pagina = telaOrcamentos; break;
                case 4:  pagina = telaLocacoes; break;
                case 5:  pagina = telaDevolucoes; break;
                case 6:  pagina = telaRelatorios; break;
                case 7:  pagina = telaConfig; break;
                default: pagina = criarPlaceholder(MENU[i]);       // telas ainda não feitas
            }
            paginas.add(pagina, String.valueOf(i));
        }
        return paginas;
    }

    private JComponent criarPlaceholder(String nome) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BRANCO);
        p.setBorder(new EmptyBorder(14, 16, 16, 16));
        p.add(rotulo(nome, Font.BOLD, 20), BorderLayout.NORTH);

        JPanel caixa = new JPanel(new GridBagLayout());
        caixa.setBackground(CINZA_COL);
        caixa.setBorder(new DashedBorder(CINZA_BORDA, 2));
        JLabel msg = rotulo("Tela de " + nome.toLowerCase(PT_BR) + " ainda em construção", Font.PLAIN, 13);
        msg.setForeground(CINZA_TEXTO);
        caixa.add(msg);

        JPanel miolo = new JPanel(new BorderLayout());
        miolo.setOpaque(false);
        miolo.setBorder(new EmptyBorder(14, 0, 0, 0));
        miolo.add(caixa, BorderLayout.CENTER);
        p.add(miolo, BorderLayout.CENTER);
        return p;
    }

    /** Troca a tela exibida e atualiza a que acabou de abrir. */
    void mostrarPagina(int idx) {
        menuSelecionado = idx;
        cartas.show(paginas, String.valueOf(idx));
        tituloJanela.setText("Ariel · Conto de Fadas  —  " + (idx == 0 ? "Painel principal" : MENU[idx]));
        for (int k = 0; k < MENU.length; k++) estiloMenu(k, false);
        if (idx == 1) telaClientes.atualizar();
        if (idx == 2) telaCatalogo.atualizar();
        if (idx == 3) telaOrcamentos.atualizar();
        if (idx == 6) telaRelatorios.atualizar();
        if (idx == 4) telaLocacoes.atualizar();
        if (idx == 5) telaDevolucoes.atualizar();
        if (idx == 7) telaConfig.atualizar();
    }

    /** Vai para a tela Clientes já com o cadastro aberto. */
    void abrirCadastro(Cliente c) {
        mostrarPagina(1);
        telaClientes.abrirCliente(c);
    }

    /** Vai para a tela Devoluções já com esta locação selecionada. */
    void abrirDevolucao(Locacao l) {
        mostrarPagina(5);
        telaDevolucoes.selecionar(l);
    }

    // ================= Barra de título com botão fechar =================
    private JComponent criarBarraTitulo() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(BORDA_CLARA);
        barra.setBorder(new EmptyBorder(4, 6, 4, 0));
        barra.setPreferredSize(new Dimension(10, 34));

        JLabel titulo = tituloJanela;
        titulo.setForeground(CINZA_TEXTO);
        barra.add(titulo, BorderLayout.WEST);

        JButton fechar = new JButton("✕");
        fechar.setFont(new Font("SansSerif", Font.BOLD, 14));
        fechar.setFocusPainted(false);
        fechar.setContentAreaFilled(false);
        fechar.setOpaque(true);
        fechar.setBackground(BORDA_CLARA);
        fechar.setForeground(PRETO);
        fechar.setBorder(new EmptyBorder(2, 14, 2, 14));
        fechar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fechar.setToolTipText("Fechar (Esc)");
        fechar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { fechar.setBackground(PRETO); fechar.setForeground(BRANCO); }
            public void mouseExited(MouseEvent e)  { fechar.setBackground(BORDA_CLARA); fechar.setForeground(PRETO); }
        });
        fechar.addActionListener(e -> dispose());
        // botão de tela cheia (ao lado do fechar)
        btnTelaCheia = new JButton(new IconeTelaCheia());
        btnTelaCheia.setFocusPainted(false);
        btnTelaCheia.setContentAreaFilled(false);
        btnTelaCheia.setOpaque(true);
        btnTelaCheia.setBackground(BORDA_CLARA);
        btnTelaCheia.setBorder(new EmptyBorder(2, 12, 2, 12));
        btnTelaCheia.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTelaCheia.setToolTipText("Tela cheia (F11)");
        btnTelaCheia.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnTelaCheia.setBackground(CINZA_SEL); }
            public void mouseExited(MouseEvent e)  { btnTelaCheia.setBackground(BORDA_CLARA); }
        });
        btnTelaCheia.addActionListener(e -> alternarTelaCheia());
        JPanel botoesJanela = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botoesJanela.setOpaque(false);
        botoesJanela.add(btnTelaCheia);
        botoesJanela.add(fechar);
        barra.add(botoesJanela, BorderLayout.EAST);

        // arrastar a janela pela barra
        MouseAdapter mover = new MouseAdapter() {
            public void mousePressed(MouseEvent e) { arrasteJanela = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                if (telaCheia || arrasteJanela == null) return;       // em tela cheia a janela não se move
                Point p = getLocation();
                setLocation(p.x + e.getX() - arrasteJanela.x, p.y + e.getY() - arrasteJanela.y);
            }
        };
        MouseAdapter duplo = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) alternarTelaCheia(); }
        };
        barra.addMouseListener(duplo);
        titulo.addMouseListener(duplo);
        barra.addMouseListener(mover);
        barra.addMouseMotionListener(mover);
        titulo.addMouseListener(mover);
        titulo.addMouseMotionListener(mover);
        return barra;
    }

    // ================= Tela cheia =================

    /**
     * Alterna entre janela normal e tela cheia (F11, botão da barra ou duplo clique na barra).
     * Usa o monitor em que a janela está e guarda o tamanho anterior para voltar exatamente a ele.
     */
    void alternarTelaCheia() {
        if (!telaCheia) {
            limitesAntes = getBounds();
            GraphicsConfiguration gc = getGraphicsConfiguration();
            Rectangle tela = gc != null ? gc.getBounds() : GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            setMinimumSize(new Dimension(0, 0));
            setBounds(tela);
            telaCheia = true;
        } else {
            setMinimumSize(new Dimension(980, 620));
            setBounds(limitesAntes != null ? limitesAntes : new Rectangle(100, 60, 1120, 720));
            telaCheia = false;
        }
        btnTelaCheia.setToolTipText(telaCheia ? "Sair da tela cheia (F11 ou Esc)" : "Tela cheia (F11)");
        btnTelaCheia.repaint();
        revalidate();
        repaint();
    }

    boolean emTelaCheia() { return telaCheia; }

    /** Ícone desenhado (sem depender de fonte): quadrado com cantos = entrar; dois quadrados = sair. */
    private class IconeTelaCheia implements Icon {
        public int getIconWidth() { return 16; }
        public int getIconHeight() { return 16; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(PRETO);
            g.setStroke(new BasicStroke(2f));
            if (!telaCheia) {
                g.drawRect(x + 2, y + 2, 12, 11);
            } else {
                g.drawRect(x + 2, y + 5, 9, 8);
                g.drawLine(x + 5, y + 5, x + 5, y + 2);
                g.drawLine(x + 5, y + 2, x + 14, y + 2);
                g.drawLine(x + 14, y + 2, x + 14, y + 10);
                g.drawLine(x + 14, y + 10, x + 11, y + 10);
            }
            g.dispose();
        }
    }

    // ================= Sidebar =================
    private JComponent criarSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(CINZA_SIDE);
        side.setPreferredSize(new Dimension(190, 10));
        side.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 2, new Color(0x8A8A8A)),
                new EmptyBorder(16, 12, 12, 12)));

        JLabel marca = new JLabel("<html>Ariel · Conto de<br>Fadas</html>");
        marca.setFont(new Font("SansSerif", Font.BOLD, 15));
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        marca.setBorder(new EmptyBorder(0, 4, 0, 0));
        side.add(marca);
        side.add(Box.createVerticalStrut(22));

        for (int i = 0; i < MENU.length; i++) {
            final int idx = i;
            JButton b = new JButton(MENU[i]);
            b.setFont(new Font("SansSerif", Font.PLAIN, 13));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setOpaque(true);
            b.setForeground(PRETO);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            b.setPreferredSize(new Dimension(150, 32));
            b.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { estiloMenu(idx, true); }
                public void mouseExited(MouseEvent e)  { estiloMenu(idx, false); }
            });
            b.addActionListener(e -> mostrarPagina(idx));
            botoesMenu[i] = b;
            side.add(b);
            side.add(Box.createVerticalStrut(9));
        }
        for (int k = 0; k < MENU.length; k++) estiloMenu(k, false);
        side.add(Box.createVerticalGlue());

        // Caixa com a data de hoje (atualiza sozinha quando o dia vira)
        JPanel boxHoje = new JPanel();
        boxHoje.setLayout(new BoxLayout(boxHoje, BoxLayout.Y_AXIS));
        boxHoje.setBackground(BRANCO);
        boxHoje.setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 2), new EmptyBorder(8, 10, 8, 10)));
        boxHoje.setAlignmentX(Component.LEFT_ALIGNMENT);
        boxHoje.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        boxHoje.setToolTipText("Data de hoje — atualiza sozinha quando o dia muda");
        JLabel tHoje = rotulo("HOJE", Font.BOLD, 11);
        tHoje.setForeground(CINZA_TEXTO);
        tHoje.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblDiaSemana.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblDataHoje.setAlignmentX(Component.LEFT_ALIGNMENT);
        boxHoje.add(tHoje);
        boxHoje.add(lblDiaSemana);
        boxHoje.add(lblDataHoje);
        side.add(boxHoje);

        // quem está logado + botão Sair (volta para a tela de login)
        side.add(Box.createVerticalStrut(8));
        logado = rotulo(" ", Font.BOLD, 11);
        atualizarUsuario();
        logado.setAlignmentX(Component.LEFT_ALIGNMENT);
        logado.setBorder(new EmptyBorder(0, 2, 6, 0));
        side.add(logado);
        JButton btnSair = botaoCinza("Sair", 12);
        btnSair.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSair.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        btnSair.addActionListener(e -> sair());
        side.add(btnSair);
        return side;
    }

    private void estiloMenu(int i, boolean hover) {
        JButton b = botoesMenu[i];
        if (i == menuSelecionado) {
            b.setBackground(CINZA_SEL);
            b.setBorder(new CompoundBorder(new LineBorder(PRETO, 2), new EmptyBorder(0, 8, 0, 8)));
            b.setFont(new Font("SansSerif", Font.BOLD, 13));
        } else if (hover) {
            b.setBackground(CINZA_HOVER);
            b.setBorder(new CompoundBorder(new LineBorder(PRETO, 1), new EmptyBorder(1, 9, 1, 9)));
            b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        } else {
            b.setBackground(CINZA_COL);
            b.setBorder(new CompoundBorder(new DashedBorder(new Color(0x9A9A9A), 1), new EmptyBorder(0, 9, 0, 9)));
            b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        }
    }

    // ================= Painel principal =================
    private JComponent criarAreaPrincipal() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(BRANCO);
        area.setBorder(new EmptyBorder(14, 16, 16, 16));

        JPanel topo = new JPanel();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        topo.setOpaque(false);

        // título + busca + botão
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        tituloPagina.setFont(new Font("SansSerif", Font.BOLD, 20));
        cab.add(tituloPagina, BorderLayout.WEST);

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        direita.setOpaque(false);
        Campo busca = new Campo("Buscar cliente, item ou reserva", 12);
        busca.setPreferredSize(new Dimension(230, 36));
        busca.getDocument().addDocumentListener(new DocumentListener() {
            void mudou() { filtro = busca.getText().trim().toLowerCase(); for (Coluna c : colunas) c.atualizar(); }
            public void insertUpdate(DocumentEvent e) { mudou(); }
            public void removeUpdate(DocumentEvent e) { mudou(); }
            public void changedUpdate(DocumentEvent e) { mudou(); }
        });
        direita.add(busca);

        JButton novo = botaoCinza("+ Novo orçamento", 13);
        novo.setPreferredSize(new Dimension(160, 36));
        novo.addActionListener(e -> novoOrcamento());
        direita.add(novo);
        cab.add(direita, BorderLayout.EAST);
        topo.add(cab);
        topo.add(Box.createVerticalStrut(14));

        // cartões de resumo (GridLayout 1x4)
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(10, 58));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        for (int i = 0; i < 4; i++) stats.add(criarStat(i));
        topo.add(stats);
        topo.add(Box.createVerticalStrut(12));

        // dica
        JPanel dica = new JPanel(new BorderLayout());
        dica.setOpaque(false);
        JLabel esq = rotulo("Arraste o cartão para outra coluna: Orçamento → Locado → Devolvido", Font.PLAIN, 12);
        JLabel dir = rotulo("Clique no cartão para abrir", Font.PLAIN, 12);
        esq.setForeground(CINZA_TEXTO);
        dir.setForeground(CINZA_TEXTO);
        dica.add(esq, BorderLayout.WEST);
        dica.add(dir, BorderLayout.EAST);
        dica.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        topo.add(dica);
        topo.add(Box.createVerticalStrut(8));

        area.add(topo, BorderLayout.NORTH);

        // colunas (GridLayout 1x3)
        JPanel grade = new JPanel(new GridLayout(1, 3, 16, 0));
        grade.setOpaque(false);
        for (int i = 0; i < 3; i++) {
            colunas[i] = new Coluna(i);
            grade.add(colunas[i]);
        }
        area.add(grade, BorderLayout.CENTER);
        return area;
    }

    private JComponent criarStat(int i) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BRANCO);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 2), new EmptyBorder(4, 11, 4, 11)));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.setToolTipText(CRITERIOS_STATS[i]);

        JLabel t = rotulo(TITULOS_STATS[i], Font.BOLD, 12);
        JLabel n = rotulo("00", Font.BOLD, 26);
        numerosStats[i] = n;
        p.add(t);
        p.add(n);

        p.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                p.setBackground(CINZA_HOVER);
                p.setBorder(new CompoundBorder(new LineBorder(PRETO, 3), new EmptyBorder(3, 10, 3, 10)));
            }
            public void mouseExited(MouseEvent e) {
                p.setBackground(BRANCO);
                p.setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 2), new EmptyBorder(4, 11, 4, 11)));
            }
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(TelaPrincipal.this,
                        TITULOS_STATS[i] + ": " + numerosStats[i].getText()
                                + "\n\nCritério: " + CRITERIOS_STATS[i]
                                + "\nReferência: hoje, " + FMT_COMPLETA.format(ctrl.hoje()),
                        "Resumo", JOptionPane.PLAIN_MESSAGE);
            }
        });
        return p;
    }

    // ================= Ações compartilhadas (usadas pelo painel e pelas outras telas) =================

    void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.PLAIN_MESSAGE);
    }

    /** Mostra o motivo de uma regra violada; se for sobre o cliente, oferece abrir o cadastro. */
    void avisarRegra(Locacao l, String msg) {
        boolean porSaldo = msg.startsWith("Saldo pendente");          // o remédio está em "Quitar saldo", nos detalhes da reserva
        String botao = porSaldo ? "Abrir reserva" : "Abrir cadastro";
        int r = JOptionPane.showOptionDialog(this, msg, l.codigo() + " · " + l.cliente.nome,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Fechar", botao}, "Fechar");
        if (r == 1) { if (porSaldo) DetalhesReserva.abrir(this, ctrl, l); else abrirCadastro(l.cliente); }
    }

    /** "+ Novo orçamento": abre a tela Orçamentos já com o formulário em branco. */
    private void novoOrcamento() {
        if (ctrl.clientes.isEmpty()) {
            aviso("Cadastre um cliente na tela Clientes antes de criar um orçamento.");
            mostrarPagina(1);
            return;
        }
        mostrarPagina(3);
        telaOrcamentos.novo();
    }

    /** Quem está usando o sistema (para registrar responsáveis). */
    Usuario usuario() { return usuario; }

    /** Retirada/entrega: pergunta QUEM retira (titular ou pessoa autorizada, RN26) e o controller registra. */
    void retirar(Locacao l) {
        try {
            ctrl.validarMovimento(l, Etapa.LOCADO);   // confere as regras antes de perguntar
        } catch (RegraNegocioException e) {
            avisarRegra(l, e.getMessage());
            return;
        }
        JComboBox<String> quem = new JComboBox<>(ctrl.pessoasParaRetirar(l).toArray(new String[0]));
        estilizarCombo(quem);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(rotulo(l.codigo() + " · " + l.cliente.nome, Font.BOLD, 13));
        p.add(rotulo("Quem está retirando os itens? (hoje: " + FMT_COMPLETA.format(ctrl.hoje()) + ")", Font.PLAIN, 12));
        p.add(quem);
        int r = JOptionPane.showOptionDialog(this, p, "Registrar retirada / entrega",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Registrar retirada", "Cancelar"}, "Registrar retirada");
        if (r != 0) return;
        try {
            ctrl.registrarRetirada(l, (String) quem.getSelectedItem());
        } catch (RegraNegocioException e) {
            avisarRegra(l, e.getMessage());
        }
    }

    /** Pagamento (entrada, saldo ou pendências): pede valor e forma; o controller valida. */
    void pagar(Locacao l) {
        String rotulo = ctrl.rotuloPagamento(l);
        if (rotulo == null) { aviso("Não há nada a pagar nesta reserva."); return; }
        Campo valor = new Campo("Valor", 13);
        valor.setText(String.format(PT_BR, "%.2f", ctrl.valorSugerido(l)).replace('.', ','));
        valor.setPreferredSize(new Dimension(260, 34));
        JComboBox<Pagamento.Forma> forma = new JComboBox<>(Pagamento.Forma.values());
        estilizarCombo(forma);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(rotulo(l.codigo() + " · " + l.cliente.nome, Font.BOLD, 13));
        p.add(rotulo("Total: " + moeda(l.total()) + "  ·  Pago: " + moeda(l.pago()) + "  ·  Falta: " + moeda(l.saldo()), Font.PLAIN, 12));
        p.add(rotulo("Valor (R$):", Font.PLAIN, 12));
        p.add(valor);
        p.add(rotulo("Forma de pagamento:", Font.PLAIN, 12));
        p.add(forma);
        int r = JOptionPane.showOptionDialog(this, p, rotulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Confirmar pagamento", "Cancelar"}, "Confirmar pagamento");
        if (r != 0) return;
        double v = lerValor(valor.getText());
        Pagamento.Forma f = (Pagamento.Forma) forma.getSelectedItem();
        try {
            try {
                ctrl.pagar(l, v, f, null);
            } catch (ExigeAutorizacaoException e) {                 // cliente inadimplente: UC22
                if (!pedirAutorizacao(l, e.getMessage())) return;
                ctrl.pagar(l, v, f, usuario);
            }
            if (l.contratoPendente) aviso(Mensagens.MSG016);
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
        }
    }

    /** UC22: cliente inadimplente não é bloqueado; um responsável pode autorizar (fica registrado). */
    boolean pedirAutorizacao(Locacao l, String msg) {
        int r = JOptionPane.showOptionDialog(this,
                msg + "\n\nVocê (" + usuario.nome() + ", " + usuario.perfil() + ") autoriza esta nova locação?\n"
                        + "Ficará registrado como responsável. A dívida anterior continua.",
                l.codigo() + " · " + l.cliente.nome, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Autorizar locação", "Recusar"}, "Recusar");
        return r == 0;
    }

    /** UC20: cadastra um terceiro autorizado a retirar os itens desta locação. */
    void autorizarTerceiro(Locacao l) {
        Campo nome = new Campo("Nome completo", 13), doc = new Campo("Documento (RG ou CPF)", 13),
                contato = new Campo("Telefone / WhatsApp", 13);
        for (Campo c : new Campo[]{nome, doc, contato}) c.setPreferredSize(new Dimension(280, 34));
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(rotulo(l.codigo() + " · " + l.cliente.nome, Font.BOLD, 13));
        p.add(rotulo("Pessoa autorizada pelo titular a RETIRAR os itens:", Font.PLAIN, 12));
        p.add(nome);
        p.add(doc);
        p.add(contato);
        int r = JOptionPane.showOptionDialog(this, p, "Autorizar terceiro para retirada",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Autorizar", "Cancelar"}, "Autorizar");
        if (r != 0) return;
        try {
            ctrl.autorizarTerceiro(l, nome.getText(), doc.getText(), contato.getText());
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
        }
    }

    /** UC06 FA04: contrata um serviço (entrega, montagem...) com valor individual. */
    void adicionarServico(Locacao l) {
        List<Catalogo.Servico> ativos = new ArrayList<>();
        for (Catalogo.Servico s : Catalogo.SERVICOS) if (s.ativo) ativos.add(s);
        if (ativos.isEmpty()) { aviso("Não há serviços ativos. Cadastre na tela Catálogo > Serviços."); return; }
        JComboBox<Catalogo.Servico> cb = new JComboBox<>(ativos.toArray(new Catalogo.Servico[0]));
        estilizarCombo(cb);
        Campo valor = new Campo("Valor", 13);
        valor.setPreferredSize(new Dimension(260, 34));
        Runnable preencher = () -> valor.setText(String.format(PT_BR, "%.2f",
                ((Catalogo.Servico) cb.getSelectedItem()).valor).replace('.', ','));
        cb.addActionListener(e -> preencher.run());
        preencher.run();
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(rotulo(l.codigo() + " · " + l.cliente.nome, Font.BOLD, 13));
        p.add(rotulo("Serviço:", Font.PLAIN, 12));
        p.add(cb);
        p.add(rotulo("Valor nesta locação (R$):", Font.PLAIN, 12));
        p.add(valor);
        int r = JOptionPane.showOptionDialog(this, p, "Adicionar serviço",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Adicionar", "Cancelar"}, "Adicionar");
        if (r != 0) return;
        try {
            ctrl.adicionarServico(l, (Catalogo.Servico) cb.getSelectedItem(), lerValor(valor.getText()));
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
        }
    }

    /** UC13: gera (ou consulta) o contrato/recibo em PDF e oferece abrir o arquivo. */
    void documento(Locacao l, boolean recibo) {
        try {
            Documento d = recibo ? ctrl.gerarRecibo(l) : ctrl.gerarContrato(l);
            int r = JOptionPane.showOptionDialog(this,
                    Mensagens.MSG018 + "\n" + d.tipo() + " v" + d.versao() + "\n" + d.arquivo(),
                    d.tipo() + " em PDF", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                    new Object[]{"Abrir PDF", "Fechar"}, "Abrir PDF");
            if (r == 0) {
                try {
                    Desktop.getDesktop().open(new java.io.File(d.arquivo()));
                } catch (Exception e) {
                    aviso("Não foi possível abrir o PDF automaticamente.\nEle está em: " + d.arquivo());
                }
            }
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
        }
    }

    /** Lê "1.234,56", "400,5" ou "400.50". Texto inválido vira 0 (o controller recusa). */
    static double lerValor(String texto) {
        String t = texto.replace("R$", "").trim().replace(" ", "");
        if (t.contains(",")) t = t.replace(".", "").replace(",", ".");
        try { return Double.parseDouble(t); } catch (NumberFormatException e) { return 0; }
    }

    /** Pede confirmação e remove o cartão do painel (o cadastro do cliente continua). */
    void excluirDoPainel(Locacao l) {
        String aviso = l.temEntrada() && l.etapa == Etapa.ORCAMENTO
                ? "\nA entrada já paga NÃO é devolvida (ela cobre a data reservada)." : "";
        int r = JOptionPane.showOptionDialog(this,
                "Excluir " + l.codigo() + " · " + l.cliente.nome + " do painel?\n"
                        + "O cadastro do cliente continua na tela Clientes." + aviso,
                "Excluir do painel", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Excluir", "Cancelar"}, "Cancelar");
        if (r != 0) return;
        try {
            ctrl.excluir(l);
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
        }
    }

    /** Detalhes da reserva (com os botões de ação, inclusive quitar o saldo). */
    void abrirDetalhes(Locacao l) {
        DetalhesReserva.abrir(this, ctrl, l);
    }

    /** Recalcula avisos dos cartões, contadores, números do resumo e a data de hoje. */
    private void atualizarTudo() {
        cartoes.keySet().removeIf(l -> !ctrl.locacoes.contains(l));      // cartões de locações removidas
        for (Card c : cartoes.values()) c.construir();
        for (Coluna col : colunas) col.atualizar();

        int[] n = ctrl.resumo();
        for (int i = 0; i < 4; i++) numerosStats[i].setText(dois(n[i]));

        LocalDate hoje = ctrl.hoje();
        String dia = hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
        lblDiaSemana.setText(Character.toUpperCase(dia.charAt(0)) + dia.substring(1));
        lblDataHoje.setText(FMT_EXTENSO.format(hoje));

        if (telaClientes != null) telaClientes.atualizar();
        if (telaLocacoes != null) telaLocacoes.atualizar();
        if (telaDevolucoes != null) telaDevolucoes.atualizar();
        if (telaCatalogo != null) telaCatalogo.atualizar();
        if (telaOrcamentos != null) telaOrcamentos.atualizar();
    }

    /** Card (componente) de uma locação: cria só uma vez e reaproveita. */
    private Card cartaoDe(Locacao l) {
        return cartoes.computeIfAbsent(l, Card::new);
    }


    static String dois(int n) { return String.format("%02d", n); }

    static String moeda(double v) { return MOEDA.format(v); }

    static String dias(long n) { return n + (Math.abs(n) == 1 ? " dia" : " dias"); }

    static JLabel rotulo(String t, int estilo, int tam) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", estilo, tam));
        l.setForeground(PRETO);
        return l;
    }

    /** Botão cinza com borda escura; vira preto ao passar o mouse. */
    static JButton botaoCinza(String texto, int tam) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, tam));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(CINZA_BTN);
        b.setForeground(PRETO);
        b.setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 2), new EmptyBorder(0, 12, 0, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(PRETO); b.setForeground(BRANCO); }
            public void mouseExited(MouseEvent e)  { b.setBackground(CINZA_BTN); b.setForeground(PRETO); }
        });
        return b;
    }

    /** Caixa de seleção em preto e branco (borda escura, seta ▾, item destacado em preto). */
    static <T> void estilizarCombo(JComboBox<T> cb) {
        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton("▾");
                b.setFont(new Font("SansSerif", Font.BOLD, 14));
                b.setBorder(new EmptyBorder(0, 8, 0, 8));
                b.setContentAreaFilled(false);
                b.setOpaque(true);
                b.setBackground(BRANCO);
                b.setForeground(PRETO);
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { cb.setBorder(new LineBorder(PRETO, 2)); }
                    public void mouseExited(MouseEvent e)  { cb.setBorder(new LineBorder(CINZA_BORDA, 2)); }
                });
                return b;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle r, boolean foco) {
                super.paintCurrentValueBackground(g, r, false);   // sem a cor de seleção padrão
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle r, boolean foco) {
                super.paintCurrentValue(g, r, false);
            }
        });
        cb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cb.setBackground(BRANCO);
        cb.setForeground(PRETO);
        cb.setBorder(new LineBorder(CINZA_BORDA, 2));
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object valor, int i,
                                                          boolean sel, boolean foco) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, valor, i, false, false);
                boolean realce = sel && i >= 0;
                l.setOpaque(true);
                l.setBorder(new EmptyBorder(4, 10, 4, 10));
                l.setBackground(realce ? PRETO : BRANCO);
                l.setForeground(realce ? BRANCO : PRETO);
                return l;
            }
        });
        cb.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { cb.setBorder(new LineBorder(PRETO, 2)); }
            public void mouseExited(MouseEvent e)  { cb.setBorder(new LineBorder(CINZA_BORDA, 2)); }
        });
    }


    // ================= Coluna =================
    /** Uma coluna do painel (Orçamento / Locado / Devolvido). Só desenha: quem está nela vem do controller. */
    class Coluna extends JPanel {
        final Etapa etapa;
        final List<Card> lista = new ArrayList<>();
        final JPanel corpo = new JPanel();
        final JLabel contador = rotulo("00", Font.BOLD, 12);
        final JPanel zonaSoltar = new JPanel(new GridBagLayout());
        boolean mostrarZona = false;
        boolean zonaAtiva = false;

        Coluna(int i) {
            super(new BorderLayout());
            this.etapa = Etapa.values()[i];
            setBackground(CINZA_COL);
            setBorder(new LineBorder(new Color(0x666666), 2));

            JPanel cab = new JPanel(new BorderLayout());
            cab.setBackground(CINZA_CAB);
            cab.setBorder(new CompoundBorder(new MatteBorder(0, 0, 2, 0, new Color(0x666666)),
                    new EmptyBorder(9, 12, 9, 12)));
            cab.add(rotulo(NOMES_COLUNAS[i], Font.BOLD, 14), BorderLayout.WEST);
            contador.setOpaque(true);
            contador.setBackground(BRANCO);
            contador.setHorizontalAlignment(SwingConstants.CENTER);
            contador.setBorder(new CompoundBorder(new LineBorder(PRETO, 1), new EmptyBorder(2, 6, 2, 6)));
            cab.add(contador, BorderLayout.EAST);
            add(cab, BorderLayout.NORTH);

            corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
            corpo.setBackground(CINZA_COL);
            corpo.setBorder(new EmptyBorder(9, 8, 8, 8));
            JScrollPane rolagem = new JScrollPane(corpo,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            rolagem.setBorder(null);
            rolagem.getVerticalScrollBar().setUnitIncrement(14);
            add(rolagem, BorderLayout.CENTER);

            String msg = i == 0 ? "Arraste para trás não é permitido"
                    : i == 1 ? "Solte aqui para registrar retirada / entrega"
                    : "Solte aqui para registrar devolução";
            JLabel m = rotulo(msg, Font.PLAIN, 12);
            m.setForeground(CINZA_TEXTO);
            zonaSoltar.add(m);
            zonaSoltar.setAlignmentX(Component.LEFT_ALIGNMENT);
            zonaSoltar.setPreferredSize(new Dimension(100, 92));
            zonaSoltar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
            zonaSoltar.setBorder(new DashedBorder(new Color(0x555555), 2));
        }

        /** Remonta a coluna a partir do que o controller diz que está nesta etapa. */
        void atualizar() {
            corpo.removeAll();
            lista.clear();
            for (Locacao l : ctrl.doPainel(etapa)) {
                Card c = cartaoDe(l);
                if (!filtro.isEmpty() && !c.textoBusca().contains(filtro)) continue;
                lista.add(c);
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
                corpo.add(c);
                corpo.add(Box.createVerticalStrut(9));
            }
            if (mostrarZona) {
                zonaSoltar.setBackground(zonaAtiva ? new Color(0xC4C4C4) : new Color(0xE9E9E9));
                zonaSoltar.setBorder(zonaAtiva ? new DashedBorder(PRETO, 3)
                        : new DashedBorder(new Color(0x555555), 2));
                corpo.add(zonaSoltar);
            }
            corpo.add(Box.createVerticalGlue());
            contador.setText(dois(ctrl.doPainel(etapa).size()));
            corpo.revalidate();
            corpo.repaint();
        }
    }

    // ================= Cartão =================
    /**
     * Cartão de uma locação no painel. É só a parte visual: os dados ficam no model (Locacao)
     * e as regras no controller. Aqui tratamos o mouse (hover, clique e ARRASTAR).
     */
    class Card extends JPanel {
        final Locacao loc;
        boolean hover = false, arrastando = false, emArraste = false;
        Point pressionado;

        Card(Locacao loc) {
            this.loc = loc;
            construir();
            configurarMouse();
        }

        String nomeCurto() {
            String n = loc.cliente.nome;
            return n.length() > 18 ? n.substring(0, 17) + "…" : n;
        }

        String titulo() { return loc.codigo() + " · " + nomeCurto(); }

        String info() {
            switch (loc.etapa) {
                case ORCAMENTO: return "Evento: " + FMT_CURTA.format(loc.evento) + " · Total: " + moeda(loc.total());
                case LOCADO:    return "Retirada: " + FMT_CURTA.format(loc.retirada) + " · Devolução: " + FMT_CURTA.format(loc.devolucao);
                default:        return "Devolvido em: " + FMT_CURTA.format(loc.devolvidoEm);
            }
        }

        String textoBusca() {
            StringBuilder it = new StringBuilder();
            for (Catalogo.Linha l : loc.itens) it.append(' ').append(l.item.nome);
            return (loc.codigo() + " " + loc.cliente.nome + " " + info() + " " + ctrl.aviso(loc) + it).toLowerCase();
        }

        void construir() {
            removeAll();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            estilo();

            // linha 1: alça + título (esquerda) e botão Excluir (direita)
            JPanel l1 = new JPanel(new BorderLayout());
            l1.setOpaque(false);
            l1.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel titulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titulo.setOpaque(false);
            JLabel grip = rotulo("⋮⋮ ", Font.PLAIN, 13);
            grip.setForeground(new Color(0x777777));
            titulo.add(grip);
            titulo.add(rotulo(titulo(), Font.BOLD, 13));
            l1.add(titulo, BorderLayout.WEST);
            l1.add(criarBotaoExcluir(), BorderLayout.EAST);

            JPanel l2 = linha(18);
            l2.add(rotulo(info(), Font.PLAIN, 11));

            JPanel l3 = linha(18);
            JLabel b = rotulo(ctrl.aviso(loc), Font.BOLD, 11);
            b.setOpaque(true);
            boolean urg = ctrl.urgente(loc);
            b.setBackground(urg ? PRETO : CINZA_BTN);
            b.setForeground(urg ? BRANCO : PRETO);
            b.setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 1), new EmptyBorder(1, 6, 1, 6)));
            l3.add(b);

            add(l1);
            add(Box.createVerticalStrut(2));
            add(l2);
            add(Box.createVerticalStrut(5));
            add(l3);

            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
            revalidate();
        }

        private JButton criarBotaoExcluir() {
            JButton x = new JButton("Excluir");
            x.setFont(new Font("SansSerif", Font.BOLD, 10));
            x.setFocusPainted(false);
            x.setContentAreaFilled(false);
            x.setOpaque(true);
            x.setBackground(BRANCO);
            x.setForeground(PRETO);
            x.setBorder(new CompoundBorder(new LineBorder(new Color(0x777777), 1), new EmptyBorder(1, 6, 1, 6)));
            x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            x.setToolTipText("Remover este cartão do painel (o cadastro continua em Clientes)");
            x.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { x.setBackground(PRETO); x.setForeground(BRANCO); }
                public void mouseExited(MouseEvent e)  { x.setBackground(BRANCO); x.setForeground(PRETO); }
            });
            x.addActionListener(e -> excluirDoPainel(loc));
            return x;
        }

        private JPanel linha(int recuo) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            p.setOpaque(false);
            p.setBorder(new EmptyBorder(0, recuo, 0, 0));
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            return p;
        }

        void estilo() {
            if (hover || emArraste) {
                setBackground(arrastando ? new Color(0xDEDEDE) : CINZA_HOVER);
                setBorder(new CompoundBorder(new LineBorder(PRETO, 2), new EmptyBorder(7, 9, 7, 9)));
            } else {
                setBackground(BRANCO);
                setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 1), new EmptyBorder(8, 10, 8, 10)));
            }
        }

        /** Coluna (diferente da atual) que está sob o ponto, em coordenadas da camada de arraste. */
        Coluna colunaSob(Point pg) {
            for (Coluna c : colunas) {
                if (c.etapa != loc.etapa && c.contains(SwingUtilities.convertPoint(vidro, pg, c))) return c;
            }
            return null;
        }

        /**
         * ARRASTAR E SOLTAR (como funciona): usamos só os eventos de mouse do Swing, sem
         * a API de Drag-and-Drop do Java. mousePressed guarda onde o clique começou;
         * mouseDragged, depois de mover mais de 5 pixels, tira uma "foto" do cartão e a desenha
         * numa camada transparente (glass pane) que acompanha o mouse; mouseReleased descobre
         * qual coluna está sob o mouse e pergunta ao controller se o movimento é permitido.
         */
        void configurarMouse() {
            MouseAdapter m = new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; estilo(); }
                public void mouseExited(MouseEvent e) {
                    // se o mouse só foi para cima do botão Excluir, o cartão continua em destaque
                    if (!emArraste && !contains(e.getPoint())) { hover = false; estilo(); }
                }
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) { menuContexto(e); return; }
                    pressionado = e.getPoint();
                    emArraste = false;
                }
                public void mouseDragged(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e) || pressionado == null) return;
                    if (!emArraste && pressionado.distance(e.getPoint()) > 5) iniciarArraste();
                    if (emArraste) moverArraste(e);
                }
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) { menuContexto(e); pressionado = null; return; }
                    if (emArraste) finalizarArraste(e);
                    else if (pressionado != null && SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) abrir();
                    pressionado = null;
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        void menuContexto(MouseEvent e) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem abrir = new JMenuItem("Abrir");
            abrir.addActionListener(x -> abrir());
            JMenuItem cadastro = new JMenuItem("Ver cadastro do cliente");
            cadastro.addActionListener(x -> abrirCadastro(loc.cliente));
            JMenuItem excluir = new JMenuItem("Excluir do painel");
            excluir.addActionListener(x -> excluirDoPainel(loc));
            menu.add(abrir);
            menu.add(cadastro);
            menu.addSeparator();
            menu.add(excluir);
            menu.show(e.getComponent(), e.getX(), e.getY());
        }

        void abrir() { abrirDetalhes(loc); }

        /** Começou a arrastar: tira a "foto" do cartão e mostra as áreas de soltar nas outras colunas. */
        void iniciarArraste() {
            emArraste = true;
            BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            paint(g);
            g.dispose();
            vidro.imagem = img;
            vidro.setVisible(true);
            arrastando = true;
            estilo();
            for (Coluna c : colunas) {
                if (c.etapa == loc.etapa) continue;
                c.mostrarZona = true;
                c.zonaAtiva = false;
                c.atualizar();
            }
        }

        /** Enquanto o mouse se move: a foto segue o mouse e a coluna sob ele fica destacada. */
        void moverArraste(MouseEvent e) {
            Point pg = SwingUtilities.convertPoint(this, e.getPoint(), vidro);
            vidro.posicao = new Point(pg.x - pressionado.x, pg.y - pressionado.y);
            vidro.repaint();
            Coluna alvo = colunaSob(pg);
            for (Coluna c : colunas) {
                if (c.etapa == loc.etapa) continue;
                boolean ativa = c == alvo;
                if (ativa != c.zonaAtiva) {
                    c.zonaAtiva = ativa;
                    c.atualizar();
                }
            }
        }

        /** Soltou o mouse: o CONTROLLER decide se pode (regras); a tela só segue a resposta dele. */
        void finalizarArraste(MouseEvent e) {
            Point pg = SwingUtilities.convertPoint(this, e.getPoint(), vidro);
            Coluna destino = colunaSob(pg);

            vidro.setVisible(false);
            vidro.imagem = null;
            emArraste = false;
            arrastando = false;
            hover = false;
            for (Coluna c : colunas) { c.mostrarZona = false; c.zonaAtiva = false; }
            estilo();

            PainelController.Acao acao = null;
            String erro = null;
            if (destino != null) {
                try {
                    acao = ctrl.validarMovimento(loc, destino.etapa);
                } catch (RegraNegocioException ex) {
                    erro = ex.getMessage();
                }
            }
            atualizarTudo();      // limpa as áreas tracejadas

            // diálogos depois que o arraste termina por completo
            final PainelController.Acao a = acao;
            final String msg = erro;
            if (msg != null) SwingUtilities.invokeLater(() -> avisarRegra(loc, msg));
            else if (a == PainelController.Acao.RETIRAR) SwingUtilities.invokeLater(() -> retirar(loc));
            else if (a == PainelController.Acao.DEVOLVER) SwingUtilities.invokeLater(() -> abrirDevolucao(loc));
        }
    }

    // ================= Camada do cartão arrastado =================
    static class Ghost extends JPanel {
        BufferedImage imagem;
        Point posicao = new Point();

        Ghost() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g0) {
            if (imagem == null) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = imagem.getWidth(), h = imagem.getHeight();
            g.translate(posicao.x + w / 2.0, posicao.y + h / 2.0);
            g.rotate(Math.toRadians(-2));
            g.translate(-w / 2.0, -h / 2.0);
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(8, 8, w, h);
            g.drawImage(imagem, 0, 0, null);
            g.setColor(PRETO);
            g.setStroke(new BasicStroke(2f));
            g.drawRect(1, 1, w - 2, h - 2);
            g.dispose();
        }
    }

    // ================= Componentes auxiliares =================
    static class DashedBorder extends javax.swing.border.AbstractBorder {
        private final Color cor;
        private final int esp;

        DashedBorder(Color cor, int esp) { this.cor = cor; this.esp = esp; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(cor);
            g2.setStroke(new BasicStroke(esp, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{5f, 3f}, 0f));
            g2.drawRect(x + esp / 2, y + esp / 2, w - esp, h - esp);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(esp, esp, esp, esp); }
    }

    /** Campo de texto com dica (texto cinza quando vazio) e borda que reage ao mouse e ao foco. */
    static class Campo extends JTextField {
        private final String dica;

        Campo(String dica, int tamanhoFonte) {
            this.dica = dica;
            setFont(new Font("SansSerif", Font.PLAIN, tamanhoFonte));
            setBackground(BRANCO);
            setForeground(PRETO);
            setCaretColor(PRETO);
            setSelectionColor(CINZA_SEL);
            setSelectedTextColor(PRETO);
            borda(false);
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { borda(true); repaint(); }
                public void focusLost(FocusEvent e)   { borda(false); repaint(); }
            });
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { if (!isFocusOwner()) setBorder(montar(PRETO, 2)); }
                public void mouseExited(MouseEvent e)  { if (!isFocusOwner()) borda(false); }
            });
        }

        String dica() { return dica; }

        private Border montar(Color c, int esp) {
            return new CompoundBorder(new LineBorder(c, esp), new EmptyBorder(4, 11 - esp, 4, 11 - esp));
        }

        private void borda(boolean foco) {
            setBorder(foco ? montar(PRETO, 3) : montar(CINZA_BORDA, 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(0x777777));
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(dica, in.left, (getHeight() + g2.getFontMetrics().getAscent()
                        - g2.getFontMetrics().getDescent()) / 2);
                g2.dispose();
            }
        }
    }

}
