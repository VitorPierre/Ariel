package view;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Pequenas peças de tela reaproveitadas pelas telas novas (caixas com título, campos rotulados, datas). */
final class Ui {
    private Ui() { }

    /** Caixa branca com borda e título pequeno em cima (como nos protótipos). */
    static JPanel caixa(String titulo, JComponent conteudo) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(TelaPrincipal.BRANCO);
        p.setBorder(new Tema.Arredondada(Tema.BORDA_SECAO, 1, Tema.RAIO_SECAO, 14, 16));   // seção: raio 18 px
        JLabel t = TelaPrincipal.rotulo(titulo, Font.BOLD, 12);
        t.setFont(Tema.fonteSecao());
        t.setForeground(TelaPrincipal.CINZA_TEXTO);
        p.add(t, BorderLayout.NORTH);
        p.add(conteudo, BorderLayout.CENTER);
        return p;
    }

    /** Seção sem título: cartão branco arredondado em volta do conteúdo (tabelas, listas). */
    static JPanel secao(JComponent conteudo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(TelaPrincipal.BRANCO);
        p.setBorder(new Tema.Arredondada(Tema.BORDA_SECAO, 1, Tema.RAIO_SECAO, 8, 10));
        p.add(conteudo, BorderLayout.CENTER);
        return p;
    }

    /** Rolagem "invisível" para tabela dentro de seção (a seção já tem a borda). */
    static JScrollPane rolagem(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.getViewport().setBackground(TelaPrincipal.BRANCO);
        sp.setBackground(TelaPrincipal.BRANCO);
        return sp;
    }

    /** Empilha componentes na vertical, cada um na sua altura (botões e campos com 44 px, rótulos menores). */
    static JPanel pilha(int espaco, JComponent... itens) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        for (int i = 0; i < itens.length; i++) {
            JComponent c = itens[i];
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            if (i > 0) p.add(Box.createVerticalStrut(espaco));
            p.add(c);
        }
        return p;
    }

    /** Espaço vertical fixo, para usar dentro de pilha(). */
    static JComponent espaco(int altura) {
        JPanel e = new JPanel();
        e.setOpaque(false);
        e.setPreferredSize(new Dimension(1, altura));
        return e;
    }

    /** Rótulo pequeno em cima de um campo. */
    static JPanel bloco(String titulo, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(0, 5));          // rótulo ACIMA do campo (DESIGN.md 6.2)
        p.setOpaque(false);
        JLabel l = TelaPrincipal.rotulo(titulo, Font.PLAIN, 12);
        l.setForeground(TelaPrincipal.CINZA_TEXTO);
        campo.setPreferredSize(new Dimension(100, Tema.ALTURA));
        p.add(l, BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    static DocumentListener ouvinte(Runnable acao) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { acao.run(); }
            public void removeUpdate(DocumentEvent e)  { acao.run(); }
            public void changedUpdate(DocumentEvent e) { acao.run(); }
        };
    }

    /** 01012026 -> 01/01/2026 (usado pela máscara de data). */
    static String formatarData(String d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            sb.append(d.charAt(i));
            if ((i == 1 || i == 3) && i < d.length() - 1) sb.append('/');
        }
        return sb.toString();
    }

    /** Coloca a máscara dd/mm/aaaa no campo. */
    static void mascaraData(JTextField c) {
        ((javax.swing.text.AbstractDocument) c.getDocument()).setDocumentFilter(
                new TelaClientes.FiltroMascara(c, 8, Ui::formatarData));
    }

    /** Lê dd/mm/aaaa (null se estiver incompleta ou inválida). */
    static LocalDate lerData(JTextField c) {
        try {
            return LocalDate.parse(c.getText().trim(), TelaPrincipal.FMT_COMPLETA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static String data(LocalDate d) { return d == null ? "" : TelaPrincipal.FMT_COMPLETA.format(d); }

    /** Número com vírgula, duas casas (para preencher campos de valor). */
    static String numero(double v) { return String.format(TelaPrincipal.PT_BR, "%.2f", v).replace('.', ','); }

    /** Botão principal (rosa): a ação que conclui a tela. Desabilitado, o Tema.Botao pinta em rosa claro. */
    static JButton botaoPreto(String texto, int tam) {
        Tema.Botao b = new Tema.Botao(texto);
        b.setFont(Tema.fonte(Font.BOLD, Math.max(tam, 13)));
        b.setBackground(Tema.PRIMARIA);
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(0, 14, 0, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { if (b.isEnabled()) b.setBackground(Tema.PRIMARIA_ESCURA); }
            public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(Tema.PRIMARIA); }
        });
        return b;
    }

    /** Botão de escolha (como 10% / 20% / 50%): preto quando marcado. */
    static JToggleButton botaoEscolha(String texto) {
        JToggleButton b = new JToggleButton(texto) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Tema.suave(g);
                java.awt.geom.RoundRectangle2D f = new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, Tema.RAIO_CONTROLE * 2f, Tema.RAIO_CONTROLE * 2f);
                g.setColor(getBackground());
                g.fill(f);
                g.setColor(isSelected() ? Tema.PRIMARIA : Tema.BORDA_FORTE);
                g.draw(f);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        b.setFont(Tema.fonte(Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorder(new EmptyBorder(0, 16, 0, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Runnable estilo = () -> {
            b.setBackground(b.isSelected() ? Tema.PRIMARIA : Tema.BOTAO);
            b.setForeground(b.isSelected() ? Color.WHITE : Tema.TEXTO);
        };
        b.addChangeListener(e -> estilo.run());
        estilo.run();
        Dimension pref = b.getPreferredSize();
        b.setPreferredSize(new Dimension(Math.max(pref.width, 96), Tema.ALTURA));
        return b;
    }

    /** Divisor responsivo entre lista e detalhe, estilizado no tema rosa e branco. */
    static JSplitPane divisor(JComponent esquerda, JComponent direita, double pesoEsquerda) {
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, esquerda, direita);
        sp.setResizeWeight(pesoEsquerda);
        sp.setContinuousLayout(true);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.setDividerSize(12);
        sp.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g0) {
                        Graphics2D g = (Graphics2D) g0.create();
                        Tema.suave(g);
                        g.setColor(Tema.FUNDO);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        int h = Math.min(36, Math.max(16, getHeight() / 8));
                        int w = 4;
                        int x = (getWidth() - w) / 2;
                        int y = (getHeight() - h) / 2;
                        g.setColor(Tema.BORDA_CAMPO);
                        g.fillRoundRect(x, y, w, h, w, w);
                        g.dispose();
                    }
                };
            }
        });
        return sp;
    }

    /** FlowLayout que quebra automaticamente para a linha de baixo quando o container estreita. */
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension min = layoutSize(target, false);
            min.width -= (getHgap() + 1);
            return min;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                Container container = target;
                while (targetWidth == 0 && container.getParent() != null) {
                    container = container.getParent();
                    targetWidth = container.getSize().width;
                }
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);
                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;

                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width = Math.max(dim.width, rowWidth);
                        if (dim.height > 0) dim.height += vgap;
                        dim.height += rowHeight;
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth > 0) rowWidth += hgap;
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                dim.width = Math.max(dim.width, rowWidth);
                if (dim.height > 0) dim.height += vgap;
                dim.height += rowHeight;
                dim.width += insets.left + insets.right + hgap * 2;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }
    }
}
