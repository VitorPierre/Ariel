package view;

import controller.CatalogoController;
import controller.ClienteController;
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
 * VIEW: janela principal - Ariel · Conto de Fadas (Swing, visual rosa e branco do DESIGN.md).
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

    // ===== Paleta: vem de Tema (azul + creme). Os nomes antigos continuam para as outras telas. =====
    static final Color PRETO       = Tema.TEXTO;         // texto e contornos escuros
    static final Color BRANCO      = Tema.SUPERFICIE;
    static final Color BORDA_CLARA = Tema.MENU;          // barra de título da janela
    static final Color BORDA_FINA  = Tema.BORDA_MEDIA;
    static final Color CINZA_SIDE  = Tema.MENU;
    static final Color CINZA_COL   = Tema.COLUNA;
    static final Color CINZA_CAB   = Tema.CABECALHO;
    static final Color CINZA_BTN   = Tema.BOTAO;
    static final Color CINZA_SEL   = Tema.SELECAO;
    static final Color CINZA_HOVER = Tema.HOVER;
    static final Color CINZA_TEXTO = Tema.TEXTO_SUAVE;
    static final Color CINZA_LINHA = Tema.LINHA_TABELA;   // divisória entre linhas de tabela
    static final Color CINZA_BORDA = Tema.BORDA_FORTE;

    // ===== Datas e moeda (padrão brasileiro) =====
    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    static final DateTimeFormatter FMT_CURTA = DateTimeFormatter.ofPattern("dd/MM");
    static final DateTimeFormatter FMT_COMPLETA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FMT_EXTENSO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", PT_BR);
    static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(PT_BR);

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
    private final ClienteController clientesCtrl;     // CONTROLLER do cadastro de clientes
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
    private boolean menuRecolhido = false;      // sidebar recolhida/expandida (Ctrl+B)
    private JPanel sidebar;
    private JLabel marcaLogo, subLogo;
    private JButton btnRecolher, btnSair;
    private Tema.Painel boxHoje;

    public TelaPrincipal(PainelController ctrl, UsuarioController usuarios, Usuario usuario, Runnable aoSair) {
        super("Ariel · Conto de Fadas");
        setIconImage(Logo.imagem());
        this.ctrl = ctrl;
        this.usuarios = usuarios;
        this.usuario = usuario;
        this.aoSair = aoSair;
        this.catalogo = new CatalogoController(ctrl);
        this.clientesCtrl = new ClienteController(ctrl);
        this.relatorios = new RelatorioController(ctrl);
        setUndecorated(true);
        // fechar pela barra, Alt+F4 ou botão X sempre pede confirmação (os dados podem estar só em memória)
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { fecharSistema(); }
        });
        setGlassPane(vidro);
        vidro.setVisible(false);

        // moldura clara em volta da tela
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(BORDA_CLARA);
        raiz.setBorder(new LineBorder(BORDA_FINA, 1));
        setContentPane(raiz);

        raiz.add(criarBarraTitulo(), BorderLayout.NORTH);

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.setBackground(Tema.FUNDO);
        conteudo.setBorder(null);
        conteudo.add(criarSidebar(), BorderLayout.WEST);
        conteudo.add(criarPaginas(), BorderLayout.CENTER);
        raiz.add(conteudo, BorderLayout.CENTER);

        // sempre que o controller avisar que algo mudou, redesenhamos a tela
        ctrl.addOuvinte(this::atualizarTudo);
        atualizarTudo();

        setSize(1120, 720);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        // ESC: só sai da tela cheia. Não fecha o sistema: Esc é apertado sem querer ao cancelar algo num campo
        getRootPane().registerKeyboardAction(e -> { if (telaCheia) alternarTelaCheia(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // F11: entra/sai da tela cheia (funciona em qualquer tela do sistema)
        getRootPane().registerKeyboardAction(e -> alternarTelaCheia(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // Ctrl+B: recolhe ou expande o menu lateral
        getRootPane().registerKeyboardAction(e -> alternarMenu(),
                KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK),
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

    /** Fechar o sistema (botão X ou Alt+F4): confirma antes, porque no modo memória os dados se perdem. */
    private void fecharSistema() {
        int r = JOptionPane.showOptionDialog(this,
                "Fechar o Ariel?\nAlterações não salvas serão perdidas"
                        + " (no modo memória, todos os dados desta sessão).",
                "Fechar o sistema", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Fechar", "Cancelar"}, "Cancelar");
        if (r != 0) return;
        dispose();
        System.exit(0);
    }

    /** Sair: fecha esta janela e volta para a tela de login. */
    private void sair() {
        dispose();
        aoSair.run();
    }

    // ================= Navegação entre telas =================
    private JComponent criarPaginas() {
        paginas.setBackground(Tema.FUNDO);
        telaClientes = new TelaClientes(this, clientesCtrl);
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
        p.setBorder(new EmptyBorder(24, 28, 24, 28));
        p.add(rotulo(nome, Font.BOLD, 30), BorderLayout.NORTH);

        JPanel caixa = new Tema.Painel(new GridBagLayout(), CINZA_COL, Tema.BORDA, 16);
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
        barra.setPreferredSize(new Dimension(10, Tema.ALTURA));

        JLabel titulo = tituloJanela;
        titulo.setForeground(CINZA_TEXTO);
        barra.add(titulo, BorderLayout.WEST);

        JButton fechar = new JButton(new IconeFechar());
        fechar.setFocusPainted(false);
        fechar.setContentAreaFilled(false);
        fechar.setOpaque(true);
        fechar.setBackground(BORDA_CLARA);
        fechar.setForeground(PRETO);
        fechar.setBorder(new EmptyBorder(2, 14, 2, 14));
        fechar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fechar.setToolTipText("Fechar o sistema");
        fechar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { fechar.setBackground(Tema.ALERTA); fechar.setForeground(BRANCO); }
            public void mouseExited(MouseEvent e)  { fechar.setBackground(BORDA_CLARA); fechar.setForeground(PRETO); }
        });
        fechar.addActionListener(e -> fecharSistema());
        // botão de minimizar (ao lado dos outros)
        JButton minimizar = new JButton(new IconeMinimizar());
        minimizar.setFocusPainted(false);
        minimizar.setContentAreaFilled(false);
        minimizar.setOpaque(true);
        minimizar.setBackground(BORDA_CLARA);
        minimizar.setBorder(new EmptyBorder(2, 12, 2, 12));
        minimizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minimizar.setToolTipText("Minimizar");
        minimizar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { minimizar.setBackground(CINZA_SEL); }
            public void mouseExited(MouseEvent e)  { minimizar.setBackground(BORDA_CLARA); }
        });
        minimizar.addActionListener(e -> setExtendedState(Frame.ICONIFIED));
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
        botoesJanela.add(minimizar);
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

    /** Seta das caixas de seleção. */
    private static class IconeSeta implements Icon {
        public int getIconWidth() { return 12; }
        public int getIconHeight() { return 8; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suave(g);
            g.setColor(Tema.PRIMARIA);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x + 2, y + 2, x + 6, y + 6);
            g.drawLine(x + 6, y + 6, x + 10, y + 2);
            g.dispose();
        }
    }

    /** Ícone de minimizar: um traço horizontal. */
    private static class IconeMinimizar implements Icon {
        public int getIconWidth() { return 14; }
        public int getIconHeight() { return 14; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suave(g);
            g.setColor(PRETO);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x + 2, y + 11, x + 11, y + 11);
            g.dispose();
        }
    }

    /** Ícone "X" desenhado. */
    private static class IconeFechar implements Icon {
        public int getIconWidth() { return 14; }
        public int getIconHeight() { return 14; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suave(g);
            g.setColor(c.getForeground());
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(x + 2, y + 2, x + 11, y + 11);
            g.drawLine(x + 11, y + 2, x + 2, y + 11);
            g.dispose();
        }
    }

    /** Alça de arrastar do cartão: seis pontinhos. */
    private static class IconeAlca implements Icon {
        public int getIconWidth() { return 12; }
        public int getIconHeight() { return 16; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suave(g);
            g.setColor(Tema.BORDA_FORTE);
            for (int col = 0; col < 2; col++) for (int lin = 0; lin < 3; lin++) g.fillOval(x + 2 + col * 5, y + 2 + lin * 5, 3, 3);
            g.dispose();
        }
    }

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
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(CINZA_SIDE);
        sidebar.setPreferredSize(new Dimension(200, 10));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 2, Tema.BORDA_MEDIA),
                new EmptyBorder(12, 12, 12, 12)));

        // botão recolher/expandir no topo da barra lateral
        JPanel barraTopo = new JPanel(new BorderLayout());
        barraTopo.setOpaque(false);
        barraTopo.setAlignmentX(Component.LEFT_ALIGNMENT);
        barraTopo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btnRecolher = new Tema.Botao("");
        ((Tema.Botao) btnRecolher).compacto = true;
        ((Tema.Botao) btnRecolher).raio = 8;
        btnRecolher.setBackground(Tema.ROSA_CLARO);
        btnRecolher.setForeground(Tema.PRIMARIA_ESCURA);
        btnRecolher.setIcon(new IconeMenu(9));
        btnRecolher.setPreferredSize(new Dimension(32, 32));
        btnRecolher.setBorder(new EmptyBorder(0, 0, 0, 0));
        btnRecolher.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRecolher.setToolTipText("Recolher menu (Ctrl+B)");
        btnRecolher.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnRecolher.setBackground(Tema.BORDA_SECAO); }
            public void mouseExited(MouseEvent e)  { btnRecolher.setBackground(Tema.ROSA_CLARO); }
        });
        btnRecolher.addActionListener(e -> alternarMenu());
        barraTopo.add(btnRecolher, BorderLayout.EAST);
        sidebar.add(barraTopo);
        sidebar.add(Box.createVerticalStrut(6));

        JPanel topo = new JPanel(new BorderLayout());          // logo + nome, centralizados na barra lateral
        topo.setOpaque(false);
        topo.setAlignmentX(Component.LEFT_ALIGNMENT);
        topo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));
        marcaLogo = new JLabel(Logo.icone(96), SwingConstants.CENTER);
        topo.add(marcaLogo, BorderLayout.CENTER);
        subLogo = new JLabel("CONTO DE FADAS", SwingConstants.CENTER);
        subLogo.setFont(Tema.fonte(Font.PLAIN, 10));
        subLogo.setForeground(Tema.TEXTO_SUAVE);
        subLogo.setBorder(new EmptyBorder(6, 0, 0, 0));
        topo.add(subLogo, BorderLayout.SOUTH);
        sidebar.add(topo);
        sidebar.add(Box.createVerticalStrut(18));

        for (int i = 0; i < MENU.length; i++) {
            final int idx = i;
            Tema.Botao b = new Tema.Botao(MENU[i]);
            b.raio = 10;
            b.setFont(Tema.fonte(Font.PLAIN, 13));
            b.setIcon(new IconeMenu(idx));
            b.setIconTextGap(10);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setForeground(PRETO);
            b.setToolTipText(MENU[i]);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTURA));
            b.setPreferredSize(new Dimension(160, Tema.ALTURA));
            b.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { estiloMenu(idx, true); }
                public void mouseExited(MouseEvent e)  { estiloMenu(idx, false); }
            });
            b.addActionListener(e -> mostrarPagina(idx));
            botoesMenu[i] = b;
            sidebar.add(b);
            sidebar.add(Box.createVerticalStrut(4));
        }
        for (int k = 0; k < MENU.length; k++) estiloMenu(k, false);
        sidebar.add(Box.createVerticalGlue());

        // Caixa com a data de hoje (atualiza sozinha quando o dia vira)
        boxHoje = new Tema.Painel(null, Tema.ROSA_CLARO, null, Tema.RAIO_CARTAO);   // caixa "Hoje": rosa claro, sem borda
        boxHoje.setLayout(new BoxLayout(boxHoje, BoxLayout.Y_AXIS));
        boxHoje.setBorder(new EmptyBorder(9, 12, 9, 12));
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
        sidebar.add(boxHoje);

        // quem está logado + botão Sair (volta para a tela de login)
        sidebar.add(Box.createVerticalStrut(8));
        logado = rotulo(" ", Font.BOLD, 11);
        atualizarUsuario();
        logado.setAlignmentX(Component.LEFT_ALIGNMENT);
        logado.setBorder(new EmptyBorder(0, 2, 6, 0));
        sidebar.add(logado);
        btnSair = botaoCinza("Sair", 12);
        btnSair.setIcon(new IconeMenu(8));
        btnSair.setIconTextGap(8);
        btnSair.setToolTipText("Sair do sistema");
        btnSair.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSair.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTURA));
        btnSair.setPreferredSize(new Dimension(160, Tema.ALTURA));
        btnSair.addActionListener(e -> sair());
        sidebar.add(btnSair);
        return sidebar;
    }

    /** Recolhe ou expande a coluna do menu lateral para dar mais espaço às telas. */
    private void alternarMenu() {
        menuRecolhido = !menuRecolhido;
        int larg = menuRecolhido ? 70 : 200;
        sidebar.setPreferredSize(new Dimension(larg, 10));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 2, Tema.BORDA_MEDIA),
                new EmptyBorder(12, menuRecolhido ? 10 : 12, 12, menuRecolhido ? 10 : 12)));

        btnRecolher.setIcon(new IconeMenu(menuRecolhido ? 10 : 9));
        btnRecolher.setToolTipText(menuRecolhido ? "Expandir menu (Ctrl+B)" : "Recolher menu (Ctrl+B)");
        Container barraTopo = btnRecolher.getParent();
        barraTopo.remove(btnRecolher);
        barraTopo.add(btnRecolher, menuRecolhido ? BorderLayout.CENTER : BorderLayout.EAST);

        marcaLogo.setIcon(Logo.icone(menuRecolhido ? 42 : 96));
        subLogo.setVisible(!menuRecolhido);

        for (int i = 0; i < MENU.length; i++) {
            JButton b = botoesMenu[i];
            b.setText(menuRecolhido ? "" : MENU[i]);
            b.setHorizontalAlignment(menuRecolhido ? SwingConstants.CENTER : SwingConstants.LEFT);
            b.setPreferredSize(new Dimension(menuRecolhido ? 44 : 160, Tema.ALTURA));
            estiloMenu(i, false);
        }

        boxHoje.setVisible(!menuRecolhido);
        logado.setVisible(!menuRecolhido);
        btnSair.setText(menuRecolhido ? "" : "Sair");
        btnSair.setHorizontalAlignment(SwingConstants.CENTER);
        btnSair.setBorder(new EmptyBorder(0, menuRecolhido ? 0 : 14, 0, menuRecolhido ? 0 : 14));
        btnSair.setPreferredSize(new Dimension(menuRecolhido ? 44 : 160, Tema.ALTURA));
        btnSair.setToolTipText(menuRecolhido ? "Sair (" + usuario.nome() + ")" : "Sair do sistema");

        sidebar.revalidate();
        sidebar.repaint();
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void estiloMenu(int i, boolean hover) {
        JButton b = botoesMenu[i];
        b.setBorder(menuRecolhido ? new EmptyBorder(0, 0, 0, 0) : new EmptyBorder(0, 12, 0, 10));
        if (i == menuSelecionado) {
            b.setBackground(Tema.PRIMARIA);
            b.setForeground(Color.WHITE);
            b.setFont(Tema.fonte(Font.BOLD, 13));
        } else if (hover) {
            b.setBackground(Tema.SELECAO);
            b.setForeground(Tema.PRIMARIA_ESCURA);
            b.setFont(Tema.fonte(Font.PLAIN, 13));
        } else {
            b.setBackground(Tema.MENU);
            b.setForeground(PRETO);
            b.setFont(Tema.fonte(Font.PLAIN, 13));
        }
    }

    /** Ícones vetoriais minimalistas do menu lateral (herdam a cor do botão). */
    private static class IconeMenu implements Icon {
        private final int tipo;
        IconeMenu(int tipo) { this.tipo = tipo; }
        @Override public int getIconWidth()  { return 16; }
        @Override public int getIconHeight() { return 16; }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suave(g);
            g.setColor(c.getForeground());
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (tipo) {
                case 0: // Painel (grade 2x2)
                    g.drawRoundRect(x + 1, y + 1, 5, 5, 2, 2);
                    g.drawRoundRect(x + 9, y + 1, 5, 5, 2, 2);
                    g.drawRoundRect(x + 1, y + 9, 5, 5, 2, 2);
                    g.drawRoundRect(x + 9, y + 9, 5, 5, 2, 2);
                    break;
                case 1: // Clientes (pessoa)
                    g.drawOval(x + 5, y + 2, 6, 6);
                    g.drawArc(x + 2, y + 10, 12, 8, 0, 180);
                    break;
                case 2: // Catálogo (caixa)
                    g.drawRoundRect(x + 2, y + 4, 12, 10, 2, 2);
                    g.drawLine(x + 2, y + 7, x + 14, y + 7);
                    g.drawLine(x + 6, y + 10, x + 10, y + 10);
                    break;
                case 3: // Orçamentos (documento)
                    g.drawRoundRect(x + 3, y + 1, 10, 14, 2, 2);
                    g.drawLine(x + 5, y + 5, x + 11, y + 5);
                    g.drawLine(x + 5, y + 8, x + 11, y + 8);
                    g.drawLine(x + 5, y + 11, x + 9, y + 11);
                    break;
                case 4: // Locações (calendário)
                    g.drawRoundRect(x + 2, y + 3, 12, 11, 2, 2);
                    g.drawLine(x + 2, y + 7, x + 14, y + 7);
                    g.drawLine(x + 5, y + 1, x + 5, y + 4);
                    g.drawLine(x + 11, y + 1, x + 11, y + 4);
                    break;
                case 5: // Devoluções (seta retorno)
                    g.drawArc(x + 3, y + 4, 10, 9, -60, 240);
                    g.drawLine(x + 3, y + 3, x + 3, y + 7);
                    g.drawLine(x + 3, y + 7, x + 7, y + 7);
                    break;
                case 6: // Relatórios (gráfico de barras)
                    g.drawLine(x + 2, y + 14, x + 14, y + 14);
                    g.drawLine(x + 4, y + 14, x + 4, y + 9);
                    g.drawLine(x + 8, y + 14, x + 8, y + 4);
                    g.drawLine(x + 12, y + 14, x + 12, y + 7);
                    break;
                case 7: // Configurações (controles deslizantes)
                    g.drawLine(x + 2, y + 4, x + 14, y + 4);
                    g.drawLine(x + 2, y + 8, x + 14, y + 8);
                    g.drawLine(x + 2, y + 12, x + 14, y + 12);
                    g.fillOval(x + 4, y + 2, 4, 4);
                    g.fillOval(x + 9, y + 6, 4, 4);
                    g.fillOval(x + 5, y + 10, 4, 4);
                    break;
                case 8: // Sair (porta + seta)
                    g.drawLine(x + 7, y + 2, x + 3, y + 2);
                    g.drawLine(x + 3, y + 2, x + 3, y + 14);
                    g.drawLine(x + 3, y + 14, x + 7, y + 14);
                    g.drawLine(x + 7, y + 8, x + 14, y + 8);
                    g.drawLine(x + 11, y + 5, x + 14, y + 8);
                    g.drawLine(x + 11, y + 11, x + 14, y + 8);
                    break;
                case 9: // Recolher («)
                    g.drawLine(x + 7, y + 4, x + 3, y + 8);
                    g.drawLine(x + 3, y + 8, x + 7, y + 12);
                    g.drawLine(x + 12, y + 4, x + 8, y + 8);
                    g.drawLine(x + 8, y + 8, x + 12, y + 12);
                    break;
                default: // Expandir (»)
                    g.drawLine(x + 4, y + 4, x + 8, y + 8);
                    g.drawLine(x + 8, y + 8, x + 4, y + 12);
                    g.drawLine(x + 9, y + 4, x + 13, y + 8);
                    g.drawLine(x + 13, y + 8, x + 9, y + 12);
                    break;
            }
            g.dispose();
        }
    }

    // ================= Painel principal =================
    private JComponent criarAreaPrincipal() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(Tema.FUNDO);
        area.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel topo = new JPanel();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        topo.setOpaque(false);

        // título + busca + botão
        JPanel cab = new JPanel(new BorderLayout(12, 6));
        cab.setOpaque(false);
        tituloPagina.setFont(Tema.titulo(Font.BOLD, 30));
        cab.add(tituloPagina, BorderLayout.WEST);

        JPanel direita = new JPanel(new Ui.WrapLayout(FlowLayout.RIGHT, 10, 4));
        direita.setOpaque(false);
        Campo busca = new Campo("Buscar cliente, item ou reserva", 12);
        busca.setPreferredSize(new Dimension(220, Tema.ALTURA));
        busca.getDocument().addDocumentListener(new DocumentListener() {
            void mudou() { filtro = busca.getText().trim().toLowerCase(); for (Coluna c : colunas) c.atualizar(); }
            public void insertUpdate(DocumentEvent e) { mudou(); }
            public void removeUpdate(DocumentEvent e) { mudou(); }
            public void changedUpdate(DocumentEvent e) { mudou(); }
        });
        direita.add(busca);

        JButton novo = botaoPrimario("+ Novo orçamento", 13);
        novo.addActionListener(e -> novoOrcamento());
        direita.add(novo);
        cab.add(direita, BorderLayout.CENTER);
        topo.add(cab);
        topo.add(Box.createVerticalStrut(14));

        // cartões de resumo (GridLayout 1x4)
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(10, 74));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
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
        Tema.Painel p = new Tema.Painel(null, BRANCO, Tema.BORDA, 14);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8, 14, 6, 14));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.setToolTipText(CRITERIOS_STATS[i]);

        JLabel t = rotulo(TITULOS_STATS[i], Font.BOLD, 11);
        t.setForeground(Tema.TEXTO_SUAVE);
        JLabel n = rotulo("00", Font.BOLD, 32);
        n.setForeground(Tema.PRIMARIA);
        numerosStats[i] = n;
        p.add(t);
        p.add(n);

        p.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { p.setBackground(CINZA_HOVER); p.borda = Tema.PRIMARIA_CLARA; p.repaint(); }
            public void mouseExited(MouseEvent e)  { p.setBackground(BRANCO); p.borda = Tema.BORDA; p.repaint(); }
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
    /**
     * Mostra a regra que impediu a ação. Se o controller disse ONDE resolver (PendenciaException), oferece
     * o botão que leva direto ao lugar certo: cadastro do cliente, produto no Catálogo ou a própria reserva.
     */
    void avisarRegra(Locacao l, RegraNegocioException e) {
        String botao = null;
        PendenciaException p = e instanceof PendenciaException x ? x : null;
        if (p != null) {
            switch (p.onde) {
                case CLIENTE: botao = "Abrir cadastro do cliente"; break;
                case PRODUTO: botao = p.produto != null ? "Abrir produto" : null; break;
                default:      botao = "Abrir reserva"; break;
            }
        }
        Object[] opcoes = botao == null ? new Object[]{"Fechar"} : new Object[]{"Fechar", botao};
        int r = JOptionPane.showOptionDialog(this, e.getMessage(), l.codigo() + " · " + l.cliente.nome,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opcoes, "Fechar");
        if (r != 1 || p == null) return;
        switch (p.onde) {
            case CLIENTE: abrirCadastro(l.cliente); break;
            case PRODUTO: abrirProduto(p.produto); break;
            default:      DetalhesReserva.abrir(this, ctrl, l); break;
        }
    }

    /** Abre o Catálogo na aba Produtos com o produto selecionado (para ajustar estoque/indisponíveis). */
    void abrirProduto(Catalogo.Item produto) {
        mostrarPagina(2);
        telaCatalogo.abrirProduto(produto);
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

    /** UC05 FA02: abre a reserva confirmada (ainda não retirada) no formulário de Orçamentos para alteração. */
    void alterarReserva(Locacao l) {
        mostrarPagina(3);
        telaOrcamentos.editar(l);
    }

    /** Quem está usando o sistema (para registrar responsáveis). */
    Usuario usuario() { return usuario; }

    /** Retirada/entrega: pergunta QUEM retira (titular ou pessoa autorizada, UC05) e o controller registra. */
    void retirar(Locacao l) {
        try {
            ctrl.validarMovimento(l, Etapa.LOCADO);   // confere as regras antes de perguntar
        } catch (RegraNegocioException e) {
            avisarRegra(l, e);
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
            avisarRegra(l, e);
        }
    }

    /** Pagamento (entrada, saldo ou pendências): pede valor e forma; o controller valida. */
    void pagar(Locacao l) {
        String rotulo = ctrl.rotuloPagamento(l);
        if (rotulo == null) { aviso("Não há nada a pagar nesta reserva."); return; }
        if (!PainelController.podeRegistrarPagamento(usuario)) {      // UC07: só o Administrador
            aviso("Somente o Administrador registra pagamentos.\nPeça a ela para registrar o pagamento desta reserva.");
            return;
        }
        Campo valor = new Campo("Valor", 13);
        valor.setText(String.format(PT_BR, "%.2f", ctrl.valorSugerido(l)).replace('.', ','));
        valor.setPreferredSize(new Dimension(260, Tema.ALTURA));
        JComboBox<Pagamento.Forma> forma = new JComboBox<>(Pagamento.Forma.values());
        estilizarCombo(forma);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.add(rotulo(l.codigo() + " · " + l.cliente.nome, Font.BOLD, 13));
        p.add(rotulo("Total: " + moeda(l.total()) + "  ·  Pago: " + moeda(l.pago()) + "  ·  Falta: " + moeda(l.saldo()), Font.PLAIN, 12));
        p.add(rotulo("Valor (R$):", Font.PLAIN, 12));
        p.add(valor);
        p.add(rotulo("Forma de pagamento:", Font.PLAIN, 12));
        p.add(forma);
        if (l.etapa == Etapa.ORCAMENTO && !l.temEntrada())             // sinal: avisa (sem bloquear) item atrasado
            for (String a : ctrl.avisosDeAtraso(l.itens, l.retirada, l))
                p.add(rotulo("<html><div style='width:360px'>Atenção: " + a + ". A disponibilidade conta que volte a tempo.</div></html>", Font.BOLD, 11));
        int r = JOptionPane.showOptionDialog(this, p, rotulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Confirmar pagamento", "Cancelar"}, "Confirmar pagamento");
        if (r != 0) return;
        double v = lerValor(valor.getText());
        Pagamento.Forma f = (Pagamento.Forma) forma.getSelectedItem();
        try {
            try {
                ctrl.pagar(l, v, f, usuario, null);
            } catch (ExigeAutorizacaoException e) {                 // cliente inadimplente: autorização registrada
                if (!pedirAutorizacao(l, e.getMessage())) return;
                ctrl.pagar(l, v, f, usuario, usuario);
            }
            if (l.contratoPendente) aviso(Mensagens.MSG016);
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
        }
    }

    /** Decisão da equipe (o UC05 bloquearia): cliente inadimplente não é bloqueado; um responsável pode autorizar (fica registrado). */
    boolean pedirAutorizacao(Locacao l, String msg) {
        int r = JOptionPane.showOptionDialog(this,
                msg + "\n\nVocê (" + usuario.nome() + ", " + usuario.perfil() + ") autoriza esta nova locação?\n"
                        + "Ficará registrado como responsável. A dívida anterior continua.",
                l.codigo() + " · " + l.cliente.nome, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new Object[]{"Autorizar locação", "Recusar"}, "Recusar");
        return r == 0;
    }

    /** Pessoa autorizada (UC03/UC05): cadastra um terceiro autorizado a retirar os itens desta locação. */
    void autorizarTerceiro(Locacao l) {
        Campo nome = new Campo("Nome completo", 13), doc = new Campo("Documento (RG ou CPF)", 13),
                contato = new Campo("Telefone / WhatsApp", 13);
        for (Campo c : new Campo[]{nome, doc, contato}) c.setPreferredSize(new Dimension(280, Tema.ALTURA));
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
        valor.setPreferredSize(new Dimension(260, Tema.ALTURA));
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
            Documento d = recibo ? ctrl.gerarRecibo(l, usuario) : ctrl.gerarContrato(l, usuario);
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

    /**
     * Lê "1.234,56", "1.500", "400,5" ou "400.50". Texto inválido vira 0 (o controller recusa).
     * Sem vírgula, ponto seguido de grupos de 3 dígitos é separador de milhar ("1.500" = 1500, "2.000.000");
     * qualquer outro ponto é decimal ("400.50", "1.5").
     */
    static double lerValor(String texto) {
        String t = texto.replace("R$", "").replace(' ', ' ').trim().replace(" ", "");
        if (t.contains(",")) t = t.replace(".", "").replace(",", ".");
        else if (t.matches("-?\\d{1,3}(\\.\\d{3})+")) t = t.replace(".", "");
        try { return Double.parseDouble(t); } catch (NumberFormatException e) { return 0; }
    }

    /** Pede confirmação e remove o cartão do painel (o cadastro do cliente continua). */
    void excluirDoPainel(Locacao l) {
        String aviso = l.etapa == Etapa.ORCAMENTO
                ? "\nO orçamento será CANCELADO e os itens voltam a ficar livres."
                        + (l.temEntrada() ? "\nA entrada já paga NÃO é devolvida (ela cobre a data reservada)." : "")
                : "\nEla continua em Locações, no histórico do cliente e nos relatórios.";
        int r = JOptionPane.showOptionDialog(this,
                "Tirar " + l.codigo() + " · " + l.cliente.nome + " do painel?\n"
                        + "Nada é apagado: o registro fica no histórico." + aviso,
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
        l.setFont(estilo == Font.BOLD && tam >= 18 ? Tema.titulo(Font.BOLD, tam) : Tema.fonte(estilo, tam));
        l.setForeground(PRETO);
        return l;
    }

    /** Botão de ação principal: azul com texto branco, cantos arredondados; escurece ao passar o mouse. */
    static JButton botaoPrimario(String texto, int tam) {
        Tema.Botao b = new Tema.Botao(texto);
        b.setFont(Tema.fonte(Font.BOLD, Math.max(tam, 13)));
        b.setBackground(Tema.PRIMARIA);
        b.setForeground(BRANCO);
        b.setBorder(new EmptyBorder(0, 16, 0, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(Tema.PRIMARIA_ESCURA); }
            public void mouseExited(MouseEvent e)  { b.setBackground(Tema.PRIMARIA); }
        });
        return b;
    }

    /** Botão SECUNDÁRIO (DESIGN.md 6.1): branco, texto rosa escuro, borda de campo; rosa claro no hover. */
    static JButton botaoCinza(String texto, int tam) {
        return botaoClaro(texto, tam, Tema.SUPERFICIE, Tema.PRIMARIA_ESCURA, Tema.BORDA_CAMPO);
    }

    /** Botão SUAVE (6.1): rosa claro, sem borda — ações repetidas dentro de uma seção (+ Produto, + Pacote...). */
    static JButton botaoSuave(String texto, int tam) {
        return botaoClaro(texto, tam, Tema.ROSA_CLARO, Tema.PRIMARIA_ESCURA, null);
    }

    /** Botão NEUTRO (6.1): branco, texto médio — ação de menor peso ou destrutiva leve (Remover, Descartar). */
    static JButton botaoNeutro(String texto, int tam) {
        return botaoClaro(texto, tam, Tema.SUPERFICIE, Tema.TEXTO_MEDIO, Tema.BORDA_CAMPO);
    }

    private static JButton botaoClaro(String texto, int tam, Color fundo, Color letra, Color borda) {
        Tema.Botao b = new Tema.Botao(texto);
        b.setFont(Tema.fonte(Font.BOLD, Math.max(tam, 13)));
        b.setBackground(fundo);
        b.setForeground(letra);
        b.borda = borda;
        b.setBorder(new EmptyBorder(0, 16, 0, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color hover = fundo.equals(Tema.ROSA_CLARO) ? Tema.BORDA_SECAO : Tema.ROSA_CLARO;
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { b.setBackground(fundo); }
        });
        return b;
    }

    /** Caixa de seleção no padrão do DESIGN.md (borda de campo, seta ▾, item destacado em rosa claro). */
    static <T> void estilizarCombo(JComboBox<T> cb) {
        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton(new IconeSeta());      // seta desenhada (a fonte nova não tem o símbolo ▾)
                b.setBorder(new EmptyBorder(0, 8, 0, 8));
                b.setContentAreaFilled(false);
                b.setOpaque(true);
                b.setBackground(BRANCO);
                b.setForeground(PRETO);
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { cb.setBorder(new Tema.Arredondada(Tema.PRIMARIA_CLARA, 2, 8, 1, 4)); }
                    public void mouseExited(MouseEvent e)  { cb.setBorder(new Tema.Arredondada(Tema.BORDA_FORTE, 1, 8, 1, 4)); }
                });
                return b;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle r, boolean foco) {
                g.setColor(BRANCO);                                 // fundo liso, sem a cor de seleção/cinza padrão
                g.fillRect(r.x, r.y, r.width, r.height);
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle r, boolean foco) {
                Object v = comboBox.getSelectedItem();
                if (v == null) return;
                JLabel l = new JLabel(String.valueOf(v));
                l.setFont(comboBox.getFont());
                l.setForeground(PRETO);
                l.setOpaque(false);
                currentValuePane.paintComponent(g, l, comboBox, r.x + 6, r.y, r.width - 6, r.height);
            }
        });
        // o texto escolhido é pintado com a cor de fundo da lista do popup (cinza padrão): deixamos igual ao resto
        Object popup = cb.getAccessibleContext().getAccessibleChild(0);
        if (popup instanceof javax.swing.plaf.basic.ComboPopup)
            ((javax.swing.plaf.basic.ComboPopup) popup).getList().setBackground(BRANCO);
        cb.setFont(Tema.fonte(Font.PLAIN, 13));
        cb.setBackground(BRANCO);
        cb.setForeground(PRETO);
        cb.setBorder(new Tema.Arredondada(Tema.BORDA_FORTE, 1, 8, 1, 4));
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object valor, int i,
                                                          boolean sel, boolean foco) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, valor, i, false, false);
                boolean realce = sel && i >= 0;
                l.setOpaque(true);
                l.setBorder(new EmptyBorder(4, 10, 4, 10));
                l.setBackground(realce ? Tema.PRIMARIA : BRANCO);
                l.setForeground(realce ? BRANCO : PRETO);
                return l;
            }
        });
        cb.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { cb.setBorder(new Tema.Arredondada(Tema.PRIMARIA_CLARA, 2, 8, 1, 4)); }
            public void mouseExited(MouseEvent e)  { cb.setBorder(new Tema.Arredondada(Tema.BORDA_FORTE, 1, 8, 1, 4)); }
        });
    }


    // ================= Coluna =================
    /** Uma coluna do painel (Orçamento / Locado / Devolvido). Só desenha: quem está nela vem do controller. */
    class Coluna extends Tema.Painel {
        final Etapa etapa;
        final List<Card> lista = new ArrayList<>();
        final JPanel corpo = new JPanel();
        final JLabel contador = rotulo("00", Font.BOLD, 12);
        final Tema.Painel zonaSoltar = new Tema.Painel(new GridBagLayout(), new Color(0, 0, 0, 0), null, 12);
        boolean mostrarZona = false;
        boolean zonaAtiva = false;

        Coluna(int i) {
            super(new BorderLayout(), CINZA_COL, Tema.BORDA, 16);
            this.etapa = Etapa.values()[i];
            faixa = Tema.corEtapa(etapa);                     // faixa em degradê no topo: lilás, azul, turquesa

            JPanel cab = new JPanel(new BorderLayout());
            cab.setOpaque(false);
            cab.setBorder(new EmptyBorder(16, 14, 6, 14));
            JLabel nomeCol = rotulo(NOMES_COLUNAS[i], Font.BOLD, 12);
            cab.add(nomeCol, BorderLayout.WEST);
            contador.setHorizontalAlignment(SwingConstants.CENTER);
            contador.setOpaque(false);
            contador.setForeground(PRETO);
            JPanel pilula = new Tema.Painel(new BorderLayout(), BRANCO, Tema.BORDA, 11);
            pilula.setBorder(new EmptyBorder(2, 10, 2, 10));
            pilula.add(contador, BorderLayout.CENTER);
            cab.add(pilula, BorderLayout.EAST);
            add(cab, BorderLayout.NORTH);

            corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
            corpo.setOpaque(false);
            corpo.setBorder(new EmptyBorder(4, 10, 10, 10));
            JScrollPane rolagem = new JScrollPane(corpo,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            rolagem.setBorder(null);
            rolagem.setOpaque(false);
            rolagem.getViewport().setOpaque(false);
            rolagem.getVerticalScrollBar().setUnitIncrement(14);
            add(rolagem, BorderLayout.CENTER);

            String msg = i == 0 ? "Arraste para trás não é permitido"
                    : i == 1 ? "Solte aqui para registrar retirada / entrega"
                    : "Solte aqui para registrar devolução";
            JLabel m = rotulo(msg, Font.BOLD, 12);
            m.setForeground(Tema.PRIMARIA);
            zonaSoltar.add(m);
            zonaSoltar.setAlignmentX(Component.LEFT_ALIGNMENT);
            zonaSoltar.setPreferredSize(new Dimension(100, 92));
            zonaSoltar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
            zonaSoltar.setBorder(new DashedBorder(Tema.PRIMARIA, 2));
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
                corpo.add(Box.createVerticalStrut(10));
            }
            if (mostrarZona) {
                        zonaSoltar.setBackground(zonaAtiva ? Tema.SELECAO : new Color(0, 0, 0, 0));
                zonaSoltar.setBorder(new DashedBorder(Tema.PRIMARIA, zonaAtiva ? 3 : 2));
                corpo.add(zonaSoltar);
            }
            corpo.add(Box.createVerticalGlue());
            contador.setText(dois(ctrl.doPainel(etapa).size()));
            corpo.revalidate();
            corpo.repaint();
            repaint();
        }
    }

    // ================= Cartão =================
    /**
     * Cartão de uma locação no painel. É só a parte visual: os dados ficam no model (Locacao)
     * e as regras no controller. Aqui tratamos o mouse (hover, clique e ARRASTAR).
     */
    class Card extends Tema.Painel {
        final Locacao loc;
        boolean hover = false, arrastando = false, emArraste = false;
        Point pressionado;

        Card(Locacao loc) {
            super(null, BRANCO, Tema.BORDA, 12);
            sombra = true;
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
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            estilo();

            // linha 1: alça + título (esquerda) e botão Excluir (direita)
            JPanel l1 = new JPanel(new BorderLayout());
            l1.setOpaque(false);
            l1.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel titulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titulo.setOpaque(false);
            JLabel grip = new JLabel(new IconeAlca());
            grip.setBorder(new EmptyBorder(0, 0, 0, 4));
            titulo.add(grip);
            titulo.add(rotulo(titulo(), Font.BOLD, 13));
            l1.add(titulo, BorderLayout.WEST);
            l1.add(criarBotaoExcluir(), BorderLayout.EAST);

            JPanel l2 = linha(18);
            l2.add(rotulo(info(), Font.PLAIN, 11));

            JPanel l3 = linha(18);
            String aviso = ctrl.aviso(loc);
            boolean urg = ctrl.urgente(loc);
            // selo em pílula (DESIGN.md 3.5): urgente = exige ação hoje; neutro = resolvido; normal = em andamento
            boolean resolvido = loc.etapa == Etapa.DEVOLVIDO || (loc.etapa == Etapa.LOCADO && !loc.temSaldo())
                    || (loc.etapa == Etapa.ORCAMENTO && loc.temEntrada());
            l3.add(urg ? Tema.Selo.urgente(aviso) : resolvido ? Tema.Selo.neutro(aviso) : Tema.Selo.normal(aviso));

            add(l1);
            add(Box.createVerticalStrut(2));
            add(l2);
            add(Box.createVerticalStrut(5));
            add(l3);

            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
            revalidate();
        }

        private JButton criarBotaoExcluir() {
            Tema.Botao x = new Tema.Botao("Excluir");
            x.compacto = true;
            x.raio = Tema.RAIO_PEQUENO;
            x.raio = 8;
            x.setFont(Tema.fonte(Font.BOLD, 10));
            x.setBackground(BRANCO);
            x.setForeground(Tema.TEXTO_SUAVE);
            x.borda = Tema.BORDA;
            x.setBorder(new EmptyBorder(2, 9, 2, 9));
            x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            x.setToolTipText("Remover este cartão do painel (o cadastro continua em Clientes)");
            x.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { x.setBackground(Tema.ALERTA_BG); x.setForeground(Tema.ALERTA); x.borda = Tema.ALERTA; }
                public void mouseExited(MouseEvent e)  { x.setBackground(BRANCO); x.setForeground(Tema.TEXTO_SUAVE); x.borda = Tema.BORDA; }
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
                setBackground(arrastando ? Tema.SELECAO : CINZA_HOVER);
                borda = Tema.PRIMARIA_CLARA;
            } else {
                setBackground(BRANCO);
                borda = Tema.BORDA;
            }
            setBorder(new EmptyBorder(11, 13, 14, 13));
            repaint();
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
            RegraNegocioException erro = null;
            if (destino != null) {
                try {
                    acao = ctrl.validarMovimento(loc, destino.etapa);
                } catch (RegraNegocioException ex) {
                    erro = ex;
                }
            }
            atualizarTudo();      // limpa as áreas tracejadas

            // diálogos depois que o arraste termina por completo
            final PainelController.Acao a = acao;
            final RegraNegocioException msg = erro;
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
            Tema.suave(g);
            g.setColor(new Color(20, 50, 80, 45));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(6, 10, w - 2, h - 2, 24, 24));
            g.drawImage(imagem, 0, 0, null);
            g.setColor(Tema.PRIMARIA);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(0.8f, 0.8f, w - 4, h - 7, 24, 24));
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
            Tema.suave(g2);
            g2.setColor(cor);
            g2.setStroke(new BasicStroke(esp, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{5f, 3f}, 0f));
            int arco = c instanceof Tema.Painel ? 24 : 0;
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(x + esp / 2f, y + esp / 2f, w - esp - 1, h - esp - 1, arco, arco));
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
            setFont(Tema.fonte(Font.PLAIN, tamanhoFonte));
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
                public void mouseEntered(MouseEvent e) { if (!isFocusOwner()) setBorder(montar(Tema.PRIMARIA_CLARA, 2)); }
                public void mouseExited(MouseEvent e)  { if (!isFocusOwner()) borda(false); }
            });
        }

        String dica() { return dica; }

        private Border montar(Color c, int esp) {
            return new Tema.Arredondada(c, esp, 10, 5, 10 - esp);
        }

        private void borda(boolean foco) {
            setBorder(foco ? montar(Tema.PRIMARIA, 2) : montar(Tema.BORDA_FORTE, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Tema.TEXTO_SUAVE);
                g2.setFont(getFont());
                Insets in = getInsets();
                g2.drawString(dica, in.left, (getHeight() + g2.getFontMetrics().getAscent()
                        - g2.getFontMetrics().getDescent()) / 2);
                g2.dispose();
            }
        }
    }

}
