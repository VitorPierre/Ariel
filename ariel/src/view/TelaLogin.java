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
 * VIEW: tela de login (DESIGN.md 8.1): painel rosa claro com a logo à esquerda, formulário à direita.
 * Não confere a senha: entrega o que foi digitado ao LoginController, que valida as regras
 * (usuário/senha certos, bloqueio após muitas tentativas). Se der certo, chama aoEntrar.
 */
public class TelaLogin extends JFrame {

    private final LoginController ctrl;
    private final Consumer<Usuario> aoEntrar;
    private final TelaPrincipal.Campo tfUsuario = new TelaPrincipal.Campo("", 14);
    private final CampoSenha pfSenha = new CampoSenha("", 14);
    private final JLabel lblErro = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final JButton btnEntrar = TelaPrincipal.botaoPrimario("Entrar", 14);
    private Point arrasteJanela;

    public TelaLogin(LoginController ctrl, Consumer<Usuario> aoEntrar) {
        super("Ariel · Conto de Fadas");
        setIconImage(Logo.imagem());
        this.ctrl = ctrl;
        this.aoEntrar = aoEntrar;
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(TelaPrincipal.BORDA_CLARA);
        raiz.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(TelaPrincipal.BORDA_FINA, 1), new EmptyBorder(0, 0, 0, 0)));
        setContentPane(raiz);
        raiz.add(criarBarraTitulo(), BorderLayout.NORTH);

        // área branca com o quadro do login no centro
        JPanel fundo = new JPanel(new GridLayout(1, 2));        // duas colunas: marca | formulário
        fundo.setBackground(TelaPrincipal.BRANCO);
        fundo.add(criarPainelMarca());
        JPanel direita = new JPanel(new GridBagLayout());
        direita.setBackground(TelaPrincipal.BRANCO);
        direita.add(criarQuadro());
        fundo.add(direita);
        raiz.add(fundo, BorderLayout.CENTER);

        // Enter em qualquer campo aciona o botão; ESC fecha
        getRootPane().setDefaultButton(btnEntrar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setSize(1000, 640);
        setMinimumSize(new Dimension(860, 600));
        setLocationRelativeTo(null);
    }

    private JComponent criarBarraTitulo() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(TelaPrincipal.BORDA_CLARA);
        barra.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Tema.BORDA_SECAO), new EmptyBorder(4, 12, 4, 0)));
        barra.setPreferredSize(new Dimension(10, Tema.ALTURA));
        JLabel titulo = TelaPrincipal.rotulo("Ariel · Conto de Fadas  —  Login", Font.PLAIN, 12);
        titulo.setForeground(TelaPrincipal.CINZA_TEXTO);
        barra.add(titulo, BorderLayout.WEST);

        JButton fechar = new JButton("×");
        fechar.setFont(Tema.fonte(Font.BOLD, 14));
        fechar.setFocusPainted(false);
        fechar.setContentAreaFilled(false);
        fechar.setOpaque(true);
        fechar.setBackground(TelaPrincipal.BORDA_CLARA);
        fechar.setBorder(new EmptyBorder(2, 14, 2, 14));
        fechar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fechar.setToolTipText("Fechar (Esc)");
        fechar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { fechar.setBackground(Tema.ERRO); fechar.setForeground(TelaPrincipal.BRANCO); }
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

    /** Coluna da esquerda: logo grande, nome completo e uma linha sobre o sistema, em fundo rosa claro. */
    private JComponent criarPainelMarca() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Tema.ROSA_CLARO);
        p.setBorder(new EmptyBorder(0, 56, 0, 40));
        JLabel logo = new JLabel(Logo.icone(240));
        JLabel nome = TelaPrincipal.rotulo("CONTO DE FADAS · FESTAS E EVENTOS", Font.PLAIN, 13);
        nome.setFont(Tema.fonte(Font.PLAIN, 13).deriveFont(java.util.Map.of(java.awt.font.TextAttribute.TRACKING, 0.15f)));
        nome.setForeground(Tema.TEXTO_SUAVE);
        JLabel frase = new JLabel("<html><div style='width:320px'>Orçamentos, locações e devoluções da decoração em um só lugar.</div></html>");
        frase.setFont(Tema.fonte(Font.PLAIN, 15));
        frase.setForeground(Tema.TEXTO_MEDIO);
        p.add(Box.createVerticalGlue());
        for (JComponent c : new JComponent[]{logo, nome, frase}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(c);
            p.add(Box.createVerticalStrut(c == logo ? 24 : 16));
        }
        p.add(Box.createVerticalGlue());
        return p;
    }

    /** Coluna da direita: formulário com no máximo 380 px de largura, rótulos acima dos campos. */
    private JComponent criarQuadro() {
        JLabel titulo = TelaPrincipal.rotulo("Entrar no sistema", Font.BOLD, 30);
        JLabel sub = TelaPrincipal.rotulo("Use o usuário e a senha cadastrados.", Font.PLAIN, 14);
        sub.setForeground(Tema.TEXTO_SUAVE);

        JPanel blocoUsuario = Ui.bloco("Usuário", tfUsuario), blocoSenha = Ui.bloco("Senha", pfSenha);
        for (JComponent c : new JComponent[]{tfUsuario, pfSenha}) c.setPreferredSize(new Dimension(100, 48));

        lblErro.setForeground(Tema.ERRO);
        btnEntrar.setPreferredSize(new Dimension(100, 48));
        btnEntrar.addActionListener(e -> entrar());

        JLabel ajuda = new JLabel("<html><div style='width:330px'>Esqueceu a senha? Peça à proprietária para redefinir em Configurações.</div></html>");
        ajuda.setFont(Tema.fonte(Font.PLAIN, 13));
        ajuda.setForeground(Tema.TEXTO_MEDIO);
        Tema.Painel caixaAjuda = new Tema.Painel(new BorderLayout(), Tema.ROSA_CLARO, null, Tema.RAIO_CONTROLE);
        caixaAjuda.setBorder(new EmptyBorder(12, 14, 12, 14));
        caixaAjuda.add(ajuda, BorderLayout.CENTER);

        JPanel q = Ui.pilha(0, titulo, Ui.espaco(6), sub, Ui.espaco(24), blocoUsuario,
                Ui.espaco(16), blocoSenha, Ui.espaco(8), lblErro,
                Ui.espaco(8), btnEntrar, Ui.espaco(20), caixaAjuda);
        q.setPreferredSize(new Dimension(380, q.getPreferredSize().height));
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
