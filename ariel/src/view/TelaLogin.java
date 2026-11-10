package view;

import controller.LoginController;
import model.RegraNegocioException;
import model.Usuario;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * VIEW: tela de login (usuário, senha e botão Entrar), no mesmo estilo preto e branco.
 * Não confere a senha: entrega o que foi digitado ao LoginController, que valida as regras
 * (usuário/senha certos, bloqueio após muitas tentativas). Se der certo, chama aoEntrar.
 */
public class TelaLogin extends JFrame {

    private final LoginController ctrl;
    private final Consumer<Usuario> aoEntrar;
    private final TelaPrincipal.Campo tfUsuario = new TelaPrincipal.Campo("Nome de usuário", 14);
    private final CampoSenha pfSenha = new CampoSenha("Senha", 14);
    private final JLabel lblErro = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final JButton btnEntrar = TelaPrincipal.botaoCinza("Entrar", 14);
    private Point arrasteJanela;

    public TelaLogin(LoginController ctrl, Consumer<Usuario> aoEntrar) {
        super("Ariel · Conto de Fadas");
        this.ctrl = ctrl;
        this.aoEntrar = aoEntrar;
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(TelaPrincipal.BORDA_CLARA);
        raiz.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(TelaPrincipal.BORDA_FINA, 1), new EmptyBorder(0, 6, 6, 6)));
        setContentPane(raiz);
        raiz.add(criarBarraTitulo(), BorderLayout.NORTH);

        // área branca com o quadro do login no centro
        JPanel fundo = new JPanel(new GridBagLayout());
        fundo.setBackground(TelaPrincipal.BRANCO);
        fundo.setBorder(new LineBorder(TelaPrincipal.PRETO, 2));
        fundo.add(criarQuadro());
        raiz.add(fundo, BorderLayout.CENTER);

        // Enter em qualquer campo aciona o botão; ESC fecha
        getRootPane().setDefaultButton(btnEntrar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setSize(760, 560);
        setMinimumSize(new Dimension(560, 480));
        setLocationRelativeTo(null);
    }

    private JComponent criarBarraTitulo() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(TelaPrincipal.BORDA_CLARA);
        barra.setBorder(new EmptyBorder(4, 6, 4, 0));
        barra.setPreferredSize(new Dimension(10, 34));
        JLabel titulo = TelaPrincipal.rotulo("Ariel · Conto de Fadas  —  Login", Font.PLAIN, 12);
        titulo.setForeground(TelaPrincipal.CINZA_TEXTO);
        barra.add(titulo, BorderLayout.WEST);

        JButton fechar = new JButton("✕");
        fechar.setFont(new Font("SansSerif", Font.BOLD, 14));
        fechar.setFocusPainted(false);
        fechar.setContentAreaFilled(false);
        fechar.setOpaque(true);
        fechar.setBackground(TelaPrincipal.BORDA_CLARA);
        fechar.setBorder(new EmptyBorder(2, 14, 2, 14));
        fechar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fechar.setToolTipText("Fechar (Esc)");
        fechar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { fechar.setBackground(TelaPrincipal.PRETO); fechar.setForeground(TelaPrincipal.BRANCO); }
            public void mouseExited(MouseEvent e)  { fechar.setBackground(TelaPrincipal.BORDA_CLARA); fechar.setForeground(TelaPrincipal.PRETO); }
        });
        fechar.addActionListener(e -> dispose());
        barra.add(fechar, BorderLayout.EAST);

        MouseAdapter mover = new MouseAdapter() {     // arrastar a janela pela barra
            public void mousePressed(MouseEvent e) { arrasteJanela = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - arrasteJanela.x, p.y + e.getY() - arrasteJanela.y);
            }
        };
        barra.addMouseListener(mover);
        barra.addMouseMotionListener(mover);
        titulo.addMouseListener(mover);
        titulo.addMouseMotionListener(mover);
        return barra;
    }

    private JComponent criarQuadro() {
        JPanel q = new JPanel();
        q.setLayout(new BoxLayout(q, BoxLayout.Y_AXIS));
        q.setBackground(TelaPrincipal.BRANCO);
        q.setBorder(new CompoundBorder(new LineBorder(new Color(0x666666), 2), new EmptyBorder(24, 28, 24, 28)));
        q.setPreferredSize(new Dimension(400, 380));

        JPanel logo = new JPanel(new GridBagLayout());
        logo.setBackground(new Color(0xE4E4E4));
        logo.setBorder(new TelaPrincipal.DashedBorder(new Color(0x888888), 1));
        logo.add(TelaPrincipal.rotulo("Logo Ariel", Font.PLAIN, 12));
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        logo.setPreferredSize(new Dimension(100, 80));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        q.add(logo);
        q.add(Box.createVerticalStrut(16));

        JLabel titulo = TelaPrincipal.rotulo("Entrar no sistema", Font.BOLD, 20);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        q.add(titulo);
        q.add(Box.createVerticalStrut(14));

        for (JComponent c : new JComponent[]{tfUsuario, pfSenha}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            c.setPreferredSize(new Dimension(100, 40));
            q.add(c);
            q.add(Box.createVerticalStrut(10));
        }

        lblErro.setAlignmentX(Component.LEFT_ALIGNMENT);
        q.add(lblErro);
        q.add(Box.createVerticalStrut(8));

        btnEntrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnEntrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnEntrar.setPreferredSize(new Dimension(100, 42));
        btnEntrar.addActionListener(e -> entrar());
        q.add(btnEntrar);
        return q;
    }

    /** Clicou em Entrar (ou apertou Enter): entrega ao controller e reage ao resultado. */
    private void entrar() {
        try {
            Usuario u = ctrl.entrar(tfUsuario.getText(), pfSenha.getPassword());
            dispose();           // fecha o login
            aoEntrar.accept(u);  // abre o sistema
        } catch (RegraNegocioException e) {
            lblErro.setText(e.getMessage());
            pfSenha.setText("");
            pfSenha.requestFocusInWindow();
        }
    }
}
