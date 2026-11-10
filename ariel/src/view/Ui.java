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

    static final Color CINZA_PAINEL = new Color(0x666666);

    /** Caixa branca com borda e título pequeno em cima (como nos protótipos). */
    static JPanel caixa(String titulo, JComponent conteudo) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(TelaPrincipal.BRANCO);
        p.setBorder(new CompoundBorder(new LineBorder(CINZA_PAINEL, 2), new EmptyBorder(8, 10, 10, 10)));
        JLabel t = TelaPrincipal.rotulo(titulo, Font.BOLD, 12);
        t.setForeground(TelaPrincipal.CINZA_TEXTO);
        p.add(t, BorderLayout.NORTH);
        p.add(conteudo, BorderLayout.CENTER);
        return p;
    }

    /** Rótulo pequeno em cima de um campo. */
    static JPanel bloco(String titulo, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = TelaPrincipal.rotulo(titulo, Font.PLAIN, 11);
        l.setForeground(TelaPrincipal.CINZA_TEXTO);
        campo.setPreferredSize(new Dimension(100, 34));
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

    /** Botão escuro (ação principal). */
    static JButton botaoPreto(String texto, int tam) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, tam));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(TelaPrincipal.PRETO);
        b.setForeground(TelaPrincipal.BRANCO);
        b.setBorder(new CompoundBorder(new LineBorder(TelaPrincipal.PRETO, 2), new EmptyBorder(0, 12, 0, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { if (b.isEnabled()) b.setBackground(new Color(0x444444)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(TelaPrincipal.PRETO); }
        });
        return b;
    }

    /** Botão de escolha (como 10% / 20% / 50%): preto quando marcado. */
    static JToggleButton botaoEscolha(String texto) {
        JToggleButton b = new JToggleButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBorder(new CompoundBorder(new LineBorder(new Color(0x444444), 2), new EmptyBorder(0, 10, 0, 10)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Runnable estilo = () -> {
            b.setBackground(b.isSelected() ? TelaPrincipal.PRETO : TelaPrincipal.BRANCO);
            b.setForeground(b.isSelected() ? TelaPrincipal.BRANCO : TelaPrincipal.PRETO);
        };
        b.addChangeListener(e -> estilo.run());
        estilo.run();
        return b;
    }
}
