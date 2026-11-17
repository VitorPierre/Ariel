package view;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/** VIEW: barra de rolagem fina e arredondada, nas cores do tema (substitui a barra padrão do Swing). */
public class BarraRolagem extends BasicScrollBarUI {

    public static ComponentUI createUI(JComponent c) { return new BarraRolagem(); }

    @Override public void installUI(JComponent c) {
        super.installUI(c);
        c.setOpaque(false);
    }

    @Override protected void configureScrollBarColors() {
        thumbColor = Tema.BORDA_CAMPO;
        trackColor = Tema.SUPERFICIE;
    }

    @Override public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL ? new Dimension(10, 48) : new Dimension(48, 10);
    }

    @Override protected JButton createDecreaseButton(int o) { return invisivel(); }
    @Override protected JButton createIncreaseButton(int o) { return invisivel(); }

    private static JButton invisivel() {
        JButton b = new JButton() { @Override public void paint(Graphics g) { /* sem setas */ } };
        b.setContentAreaFilled(false);
        b.setBorder(null);
        b.setFocusable(false);
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        Container pai = c.getParent();                    // trilho some no fundo de quem contém a rolagem
        if (pai == null || !pai.isOpaque()) return;
        g.setColor(pai.getBackground());
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g = (Graphics2D) g0.create();
        Tema.suave(g);
        g.setColor(isDragging || isThumbRollover() ? Tema.PRIMARIA_ESCURA : Tema.BORDA_CAMPO);
        g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
        g.dispose();
    }
}
