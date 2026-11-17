package view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** VIEW: carrega a logo oficial do projeto (src/recursos/logo.png) e entrega redimensionada com boa qualidade. */
final class Logo {
    private Logo() {}

    private static BufferedImage original;

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

    /** Logo com a largura pedida (mantém a proporção). Reduz em etapas para não serrilhar. */
    static Icon icone(int largura) {
        BufferedImage img = original();
        int altura = Math.max(1, Math.round(largura * (float) img.getHeight() / img.getWidth()));
        BufferedImage atual = img;
        int w = img.getWidth(), h = img.getHeight();
        while (w / 2 >= largura * 2) { w /= 2; h /= 2; atual = passo(atual, w, h); }
        return new ImageIcon(passo(atual, largura, altura));
    }

    private static BufferedImage passo(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
