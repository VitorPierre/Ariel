package view;

import model.Etapa;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * VIEW: tema visual oficial do sistema (DESIGN.md v1.0 — rosa e branco).
 * Todas as cores, fontes e peças desenhadas (painel arredondado, botão, selo, borda) ficam AQUI:
 * nenhuma tela declara cor própria. Para mudar o visual do programa inteiro, muda-se esta classe
 * (e antes dela, o DESIGN.md).
 * (As constantes antigas de TelaPrincipal — PRETO, CINZA_* — apontam para estas, então todas as telas acompanham.)
 */
final class Tema {
    private Tema() {}

    // ----- marca (DESIGN.md 3.1) -----
    static final Color PRIMARIA        = new Color(0xC2255C);   // rosa principal: ação principal, menu ativo, selo urgente, foco
    static final Color PRIMARIA_ESCURA = new Color(0xA61E4D);   // rosa escuro: hover do principal, texto de destaque, números
    static final Color PRIMARIA_PRESS  = new Color(0x7A1238);   // pressionado

    // ----- superfícies (3.2) -----
    static final Color SUPERFICIE  = Color.WHITE;               // cartões, seções, campos, menu, tabelas
    static final Color FUNDO       = new Color(0xFFF7FA);       // fundo da área de trabalho
    static final Color ROSA_CLARO  = new Color(0xFFF0F5);       // caixa "Hoje", botões suaves, seleção, selo normal

    // ----- linhas e bordas (3.3) -----
    static final Color BORDA_SECAO  = new Color(0xF6DCE5);      // contorno de seções; divisória do menu; cabeçalho de tabela
    static final Color BORDA_CARTAO = new Color(0xF1D3DD);      // cartões de reserva
    static final Color BORDA_CAMPO  = new Color(0xE8BFCD);      // campos, selects, botões secundários; hover de cartão/linha
    static final Color BORDA_CLARA  = new Color(0xEFC4D3);      // botão "Sair" e "Excluir" do cartão
    static final Color LINHA_TABELA = new Color(0xFBE9EF);      // divisória entre linhas
    static final Color BORDA_SELO   = new Color(0xD9B8C4);      // contorno do selo neutro

    // ----- texto (3.4) -----
    static final Color TEXTO        = new Color(0x3B1F2B);
    static final Color TEXTO_MEDIO  = new Color(0x5E4350);
    static final Color TEXTO_SUAVE  = new Color(0x7A5C68);

    // ----- status (3.5) -----
    static final Color ERRO         = new Color(0x9B1C1C), ERRO_BG = new Color(0xFDECEC);

    // ----- etapas do painel (3.6): variam também em intensidade -----
    static final Color ORCAMENTO    = new Color(0xF8BBD0);      // rosa claro
    static final Color LOCADO       = new Color(0xE64980);      // rosa médio
    static final Color DEVOLVIDO    = new Color(0x8E1F4D);      // vinho

    // ----- sombra única do sistema (3.7): só nos cartões de reserva -----
    static final Color SOMBRA       = new Color(166, 30, 77, 15);

    // ----- nomes de função usados pelas telas (apontam para as cores acima) -----
    static final Color PRIMARIA_CLARA = BORDA_CAMPO;            // borda em hover
    static final Color MENU        = SUPERFICIE;                // menu lateral e barra de título
    static final Color COLUNA      = SUPERFICIE;                // colunas do painel
    static final Color CABECALHO   = SUPERFICIE;                // cabeçalho de tabela (com linha BORDA_SECAO embaixo)
    static final Color BOTAO       = SUPERFICIE;                // botão secundário (com borda BORDA_CAMPO)
    static final Color SELECAO     = ROSA_CLARO;                // linha selecionada
    static final Color HOVER       = ROSA_CLARO;
    static final Color BORDA       = BORDA_SECAO;
    static final Color BORDA_MEDIA = BORDA_SECAO;               // contorno de caixas e seções
    static final Color BORDA_FORTE = BORDA_CAMPO;               // contorno de campos e botões
    static final Color ALERTA      = ERRO, ALERTA_BG = ERRO_BG;

    static Color corEtapa(Etapa e) { return e == Etapa.ORCAMENTO ? ORCAMENTO : e == Etapa.LOCADO ? LOCADO : DEVOLVIDO; }

    // ================= Medidas (DESIGN.md 5) =================
    static final int RAIO_SECAO = 9, RAIO_CAIXA = 8, RAIO_CARTAO = 7, RAIO_CONTROLE = 6, RAIO_PEQUENO = 4;   // em "raio" (metade do diâmetro: 18/16/14/12/8 px)
    static final int ALTURA = 44;                               // altura mínima de botão, campo e item de menu

    // ================= Fontes (DESIGN.md 4) =================
    static {
        // Inter vem embutida no projeto (src/recursos/fontes) para o visual ser igual em todo computador.
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String arq : new String[]{"Inter-Regular.otf", "Inter-Bold.otf", "Outfit-SemiBold.ttf", "Outfit-Bold.ttf"}) {
            try (java.io.InputStream in = Tema.class.getResourceAsStream("/recursos/fontes/" + arq)) {
                if (in != null) ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
            } catch (Exception ignorado) { /* sem o arquivo, cai na próxima fonte da lista */ }
        }
    }

    private static final Set<String> INSTALADAS = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

    private static String primeira(String padrao, String... opcoes) {
        for (String o : opcoes) if (INSTALADAS.contains(o)) return o;
        return padrao;
    }

    /** Texto, rótulos, tabelas e botões: Inter → a sans do sistema. */
    private static final String SANS = primeira(Font.SANS_SERIF, "Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", "Arial");
    /** Títulos e números grandes: Outfit → Poppins → Inter. */
    private static final String TITULO = primeira(SANS, "Outfit", "Poppins");


    static {
        // texto de controles desabilitados (DESIGN.md 7): legível, sem parecer clicável
        UIManager.put("Button.disabledText", TEXTO_SUAVE);
        UIManager.put("ToggleButton.disabledText", TEXTO_SUAVE);
        UIManager.put("Label.disabledForeground", TEXTO_SUAVE);
        UIManager.put("ComboBox.disabledForeground", TEXTO_SUAVE);
        UIManager.put("Table.scrollPaneBorder", javax.swing.BorderFactory.createEmptyBorder());   // sem moldura azul do Swing
        UIManager.put("ScrollBarUI", BarraRolagem.class.getName());
        UIManager.put("ScrollBar.background", SUPERFICIE);
        UIManager.put("ScrollPane.background", SUPERFICIE);
        UIManager.put("Viewport.background", SUPERFICIE);
        UIManager.put("ScrollBar.squareButtons", Boolean.FALSE);
        // diálogos (JOptionPane): mesmas cores e fontes do sistema
        UIManager.put("OptionPane.messageFont", new Font(SANS, Font.PLAIN, 13));
        UIManager.put("OptionPane.buttonFont", new Font(SANS, Font.BOLD, 13));
        UIManager.put("Button.font", new Font(SANS, Font.BOLD, 13));
        UIManager.put("Button.background", SUPERFICIE);
        UIManager.put("Button.foreground", PRIMARIA_ESCURA);
        UIManager.put("Button.select", ROSA_CLARO);
        UIManager.put("Button.focus", PRIMARIA);
        UIManager.put("Button.gradient", null);
        UIManager.put("ButtonUI", BotaoUI.class.getName());       // botões comuns e dos diálogos
        UIManager.put("Label.font", new Font(SANS, Font.PLAIN, 13));
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("TextField.font", new Font(SANS, Font.PLAIN, 13));
        UIManager.put("ComboBox.font", new Font(SANS, Font.PLAIN, 13));
        UIManager.put("CheckBox.font", new Font(SANS, Font.PLAIN, 13));
        UIManager.put("CheckBox.background", SUPERFICIE);
        UIManager.put("MenuItem.font", new Font(SANS, Font.PLAIN, 13));
        UIManager.put("MenuItem.selectionBackground", ROSA_CLARO);
        UIManager.put("MenuItem.selectionForeground", TEXTO);
        UIManager.put("PopupMenu.border", javax.swing.BorderFactory.createLineBorder(BORDA_CAMPO));
        UIManager.put("ToolTip.background", SUPERFICIE);
        UIManager.put("ToolTip.foreground", TEXTO);
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createLineBorder(BORDA_CAMPO));
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("Panel.background", SUPERFICIE);
        UIManager.put("OptionPane.messageForeground", TEXTO);
    }

    static Font fonte(int estilo, int tam)  { return new Font(SANS, estilo, tam); }

    /** Rótulo de seção ("RESUMO", "ITENS DA PROPOSTA"): 12 px, negrito, espaçado, texto suave (DESIGN.md 4.1). */
    static Font fonteSecao() {
        return fonte(Font.BOLD, 12).deriveFont(java.util.Map.of(java.awt.font.TextAttribute.TRACKING, 0.06f));
    }
    static Font titulo(int estilo, int tam) { return new Font(TITULO, estilo, tam); }

    // ================= Peças desenhadas =================
    static void suave(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Borda com cantos arredondados; pinta os cantos de fora com a cor do componente-pai (some o "quadrado"). */
    static class Arredondada implements Border {
        private final Color cor;
        private final int esp, raio;
        private final Insets pad;

        Arredondada(Color cor, int esp, int raio, int vertical, int horizontal) {
            this.cor = cor; this.esp = esp; this.raio = raio;
            this.pad = new Insets(vertical, horizontal, vertical, horizontal);
        }

        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            suave(g);
            Container pai = c.getParent();
            if (pai != null && pai.getBackground() != null) {
                Area fora = new Area(new Rectangle(x, y, w, h));
                fora.subtract(new Area(new RoundRectangle2D.Float(x, y, w, h, raio * 2f, raio * 2f)));
                g.setColor(pai.getBackground());
                g.fill(fora);
            }
            g.setColor(cor);
            g.setStroke(new BasicStroke(esp));
            float m = esp / 2f;
            g.draw(new RoundRectangle2D.Float(x + m, y + m, w - esp, h - esp, raio * 2f, raio * 2f));
            g.dispose();
        }

        @Override public Insets getBorderInsets(Component c) {
            return new Insets(pad.top + esp, pad.left + esp, pad.bottom + esp, pad.right + esp);
        }
        @Override public boolean isBorderOpaque() { return false; }
    }

    /** Painel de fundo arredondado (cartões, colunas, caixas), com borda e sombra suaves opcionais. */
    static class Painel extends JPanel {
        int raio;
        Color borda;          // null = sem borda
        boolean sombra;
        Color faixa;          // faixa colorida de 6 px no topo (colunas do painel)

        Painel(LayoutManager lm, Color fundo, Color borda, int raio) {
            super(lm);
            this.borda = borda;
            this.raio = raio;
            setOpaque(false);
            setBackground(fundo);
        }

        Painel(Color fundo, Color borda, int raio) { this(new BorderLayout(), fundo, borda, raio); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suave(g);
            int w = getWidth(), h = getHeight(), baixo = sombra ? 3 : 0;
            if (sombra) {
                for (int i = 3; i >= 1; i--) {
                    g.setColor(new Color(SOMBRA.getRed(), SOMBRA.getGreen(), SOMBRA.getBlue(), 6));
                    g.fill(new RoundRectangle2D.Float(0, i, w, h - baixo + i, raio * 2f, raio * 2f));
                }
            }
            RoundRectangle2D forma = new RoundRectangle2D.Float(0, 0, w - 1, h - baixo - 1, raio * 2f, raio * 2f);
            g.setColor(getBackground());
            g.fill(forma);
            if (faixa != null) {
                Shape antes = g.getClip();
                g.clip(forma);
                g.setColor(faixa);
                g.fillRect(0, 0, w, 6);
                g.setClip(antes);
            }
            if (borda != null) {
                g.setColor(borda);
                g.setStroke(new BasicStroke(1f));
                g.draw(forma);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Botão arredondado: usa a cor de fundo do próprio botão (setBackground) e a borda indicada. */
    static class Botao extends JButton {
        int raio = RAIO_CONTROLE;
        Color borda;          // null = sem contorno
        boolean compacto;     // true = botão pequeno dentro do cartão (28 px); os demais têm 44 px no mínimo

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            int minH = compacto ? 28 : ALTURA;
            Dimension nat = getUI() != null ? getUI().getPreferredSize(this) : d;
            int w = nat != null ? Math.max(d.width, nat.width) : d.width;
            int h = Math.max(d.height, minH);
            return new Dimension(w, h);
        }

        Botao(String texto) {
            super(texto);
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suave(g);
            RoundRectangle2D forma = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, raio * 2f, raio * 2f);
            boolean principal = getBackground().equals(PRIMARIA) || getBackground().equals(PRIMARIA_ESCURA);
            if (!isEnabled()) g.setColor(ROSA_CLARO);                                   // desabilitado
            else if (principal && getModel().isPressed()) g.setColor(PRIMARIA_PRESS);    // pressionado
            else g.setColor(getBackground());
            g.fill(forma);
            if (borda != null && isEnabled()) {
                g.setColor(borda);
                g.setStroke(new BasicStroke(1f));
                g.draw(forma);
            }
            if (isFocusOwner() && isEnabled()) {                  // anel de foco do teclado (nunca removido)
                g.setColor(principal ? SUPERFICIE : PRIMARIA);
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Float(3, 3, getWidth() - 7, getHeight() - 7, raio * 2f - 4, raio * 2f - 4));
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Selo em pílula (DESIGN.md 3.5 e 6.3). O texto sempre diz a situação por extenso: nunca só cor. */
    static class Selo extends JLabel {
        private final Color contorno;

        Selo(String texto, Color fundo, Color letra) { this(texto, fundo, letra, null); }

        Selo(String texto, Color fundo, Color letra, Color contorno) {
            super(texto);
            this.contorno = contorno;
            setFont(fonte(Font.BOLD, 12));
            setForeground(letra);
            setBackground(fundo);
            setOpaque(false);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 11, 3, 11));
        }

        /** Exige ação hoje ("Devolução hoje", "Inadimplente", "Sem estoque"). */
        static Selo urgente(String t) { return new Selo(t, PRIMARIA, SUPERFICIE); }
        /** Situação comum ("Vence em 2 dias", "Aguardando entrada"). */
        static Selo normal(String t)  { return new Selo(t, ROSA_CLARO, PRIMARIA_ESCURA); }
        /** Situação resolvida ("Quitado", "Em dia", "Finalizada"). */
        static Selo neutro(String t)  { return new Selo(t, SUPERFICIE, TEXTO, BORDA_SELO); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suave(g);
            g.setColor(getBackground());
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()));
            if (contorno != null) {
                g.setColor(contorno);
                g.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight()));
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
