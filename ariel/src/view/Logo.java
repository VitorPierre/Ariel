package view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VIEW: carrega a logo oficial do projeto (src/recursos/logo.png) e entrega redimensionada
 * com suporte a telas HiDPI (125%, 150%, 200%) e redução progressiva para nunca ficar pixelada.
 */
final class Logo {
    private Logo() {}

    private static BufferedImage original;
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    private static BufferedImage original() {
        if (original == null) {
            try (InputStream in = Logo.class.getResourceAsStream("/recursos/logo.png")) {
                if (in != null) original = ImageIO.read(in);
            } catch (IOException ignorado) { }
            if (original == null) original = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);   // sem logo: não quebra a tela
        }
        return original;
    }

    static Image imagem() { return original(); }

    /**
     * Logo com a largura lógica pedida (mantém a proporção).
     * Renderiza na resolução física real do monitor (HiDPI) com redução progressiva suave.
     */
    static Icon icone(int largura) {
        BufferedImage img = original();
        int altura = Math.max(1, Math.round(largura * (float) img.getHeight() / img.getWidth()));
        return new Icon() {
            @Override public int getIconWidth()  { return largura; }
            @Override public int getIconHeight() { return altura; }

            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                AffineTransform tx = g.getTransform();
                double sx = Math.max(1.0, Math.abs(tx.getScaleX()));
                double sy = Math.max(1.0, Math.abs(tx.getScaleY()));
                int pw = Math.max(1, (int) Math.round(largura * sx));
                int ph = Math.max(1, (int) Math.round(altura * sy));

                BufferedImage escalada = CACHE.computeIfAbsent(pw + "x" + ph, k -> redimensionar(img, pw, ph));
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(escalada, x, y, largura, altura, null);
                g.dispose();
            }
        };
    }

    /** Reduz em etapas de no máximo 50% por passo para preservar nitidez sem serrilhar. */
    private static BufferedImage redimensionar(BufferedImage src, int alvoW, int alvoH) {
        BufferedImage atual = src;
        int w = src.getWidth(), h = src.getHeight();
        while (w / 2 > alvoW && h / 2 > alvoH) {
            w = Math.max(alvoW, w / 2);
            h = Math.max(alvoH, h / 2);
            atual = passo(atual, w, h, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        return passo(atual, alvoW, alvoH, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static BufferedImage passo(BufferedImage src, int w, int h, Object interpolacao) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolacao);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
