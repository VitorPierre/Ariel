package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * VIEW: aparência padrão de TODO botão comum do sistema — inclusive os dos diálogos (JOptionPane) —
 * no tipo "Secundário" do DESIGN.md 6.1: branco, texto rosa escuro, borda de campo, raio 12, 44 px.
 * Os botões desenhados pelo Tema (Tema.Botao) e os botões "invisíveis" (setas, ícones) desligam o
 * preenchimento (contentAreaFilled = false) e não são afetados.
 */
public class BotaoUI extends BasicButtonUI {

    public static ComponentUI createUI(JComponent c) { return new BotaoUI(); }

    @Override public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        if (b.getBorder() == null || b.getBorder() instanceof javax.swing.plaf.UIResource)
            b.setBorder(new BordaPadrao());
    }

    /** Margem interna padrão (marcada como "do tema", para o Swing poder trocá-la se o botão pedir outra). */
    private static class BordaPadrao extends EmptyBorder implements javax.swing.plaf.UIResource {
        BordaPadrao() { super(0, 18, 0, 18); }
    }

    @Override public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        AbstractButton b = (AbstractButton) c;
        return b.isContentAreaFilled() ? new Dimension(Math.max(d.width, 96), Math.max(d.height, Tema.ALTURA)) : d;
    }

    @Override public void paint(Graphics g0, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        if (b.isContentAreaFilled()) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suave(g);
            float r = Tema.RAIO_CONTROLE * 2f;
            RoundRectangle2D forma = new RoundRectangle2D.Float(0, 0, c.getWidth() - 1, c.getHeight() - 1, r, r);
            ButtonModel m = b.getModel();
            g.setColor(!b.isEnabled() || m.isRollover() || m.isPressed() ? Tema.ROSA_CLARO : Tema.SUPERFICIE);
            g.fill(forma);
            if (b.isEnabled()) {
                g.setColor(Tema.BORDA_CAMPO);
                g.draw(forma);
            }
            if (b.isFocusOwner() && b.isEnabled()) {              // foco de teclado sempre visível (DESIGN.md 7)
                g.setColor(Tema.PRIMARIA);
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Float(1, 1, c.getWidth() - 3, c.getHeight() - 3, r, r));
            }
            g.dispose();
        }
        super.paint(g0, c);
    }

    @Override protected void paintFocus(Graphics g, AbstractButton b, Rectangle v, Rectangle t, Rectangle i) { /* anel desenhado acima */ }
    @Override protected void paintButtonPressed(Graphics g, AbstractButton b) { /* fundo desenhado acima */ }
}
