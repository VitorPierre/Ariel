package view;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/** Campo de senha no mesmo estilo do Campo (borda que reage ao mouse e ao foco; texto aparece como bolinhas). */
class CampoSenha extends JPasswordField {
    private final String dica;

    CampoSenha(String dica, int tamanhoFonte) {
        this.dica = dica;
        setFont(Tema.fonte(Font.PLAIN, tamanhoFonte));
        setBackground(TelaPrincipal.BRANCO);
        setForeground(TelaPrincipal.PRETO);
        setCaretColor(TelaPrincipal.PRETO);
        setSelectionColor(TelaPrincipal.CINZA_SEL);
        setSelectedTextColor(TelaPrincipal.PRETO);
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

    private Border montar(Color c, int esp) {
        return new Tema.Arredondada(c, esp, 10, 5, 10 - esp);
    }

    private void borda(boolean foco) {
        setBorder(foco ? montar(Tema.PRIMARIA, 2) : montar(Tema.BORDA_FORTE, 1));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getDocument().getLength() == 0 && !isFocusOwner()) {      // dica cinza quando vazio (sem copiar a senha)
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
