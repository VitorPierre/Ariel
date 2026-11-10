package util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerador de PDF SIMPLES, sem bibliotecas externas (o Java não traz um): cria páginas A4 só com texto.
 * Serve para contratos, recibos e relatórios (RNF008/RNF014).
 *
 * Três estilos de linha: normal (Helvetica), negrito (Helvetica-Bold) e monoespaçado (Courier, bom para tabelas).
 * Linhas longas quebram sozinhas e, quando a página enche, começa outra.
 */
public final class Pdf {

    private static final Charset WIN = Charset.forName("windows-1252");
    private static final double LARGURA = 595, ALTURA = 842, MARGEM = 56;

    private enum Estilo { NORMAL("F1", 0.52), NEGRITO("F2", 0.56), MONO("F3", 0.60);
        final String fonte; final double largura;       // largura média de um caractere, em "em"
        Estilo(String f, double l) { fonte = f; largura = l; } }

    private record Linha(Estilo estilo, double tamanho, String texto) { }

    private final List<Linha> linhas = new ArrayList<>();

    /** Título grande. */
    public Pdf titulo(String t) { adicionar(Estilo.NEGRITO, 16, t); return espaco(); }

    /** Subtítulo em negrito. */
    public Pdf subtitulo(String t) { espaco(); adicionar(Estilo.NEGRITO, 11, t); return this; }

    /** Texto normal (quebra automática). */
    public Pdf texto(String t) { adicionar(Estilo.NORMAL, 10, t); return this; }

    /** Texto em fonte de máquina de escrever (alinha colunas). */
    public Pdf mono(String t) { adicionar(Estilo.MONO, 9, t); return this; }

    public Pdf espaco() { linhas.add(new Linha(Estilo.NORMAL, 10, "")); return this; }

    private void adicionar(Estilo e, double tam, String t) {
        int max = Math.max(20, (int) ((LARGURA - 2 * MARGEM) / (tam * e.largura)));
        for (String bruta : t.split("\n", -1)) {
            String resto = bruta;
            while (resto.length() > max) {
                int corte = resto.lastIndexOf(' ', max);
                if (corte < max / 2) corte = max;               // sem espaço bom: corta na marra
                linhas.add(new Linha(e, tam, resto.substring(0, corte)));
                resto = resto.substring(corte).stripLeading();
            }
            linhas.add(new Linha(e, tam, resto));
        }
    }

    /** Grava o arquivo (cria as pastas que faltarem). */
    public void gravar(Path destino) throws IOException {
        // 1) divide as linhas em páginas
        List<List<Linha>> paginas = new ArrayList<>();
        List<Linha> atual = new ArrayList<>();
        double y = ALTURA - MARGEM;
        for (Linha l : linhas) {
            double alt = l.tamanho * 1.35;
            if (y - alt < MARGEM) { paginas.add(atual); atual = new ArrayList<>(); y = ALTURA - MARGEM; }
            atual.add(l);
            y -= alt;
        }
        paginas.add(atual);

        // 2) monta os objetos: 1 catálogo, 2 páginas, 3-5 fontes, depois (página, conteúdo) para cada página
        List<byte[]> objs = new ArrayList<>();
        objs.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < paginas.size(); i++) kids.append(6 + 2 * i).append(" 0 R ");
        objs.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + paginas.size() + " >>"));
        objs.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objs.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        objs.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"));
        for (int i = 0; i < paginas.size(); i++) {
            ByteArrayOutputStream c = new ByteArrayOutputStream();
            double yy = ALTURA - MARGEM;
            for (Linha l : paginas.get(i)) {
                yy -= l.tamanho * 1.35;
                if (l.texto.isEmpty()) continue;
                c.write(bytes("BT /" + l.estilo.fonte + " " + l.tamanho + " Tf " + MARGEM + " " + yy + " Td ("));
                c.write(escapar(l.texto));
                c.write(bytes(") Tj ET\n"));
            }
            byte[] conteudo = c.toByteArray();
            objs.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + LARGURA + " " + ALTURA + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> /Contents " + (7 + 2 * i) + " 0 R >>"));
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            s.write(bytes("<< /Length " + conteudo.length + " >>\nstream\n"));
            s.write(conteudo);
            s.write(bytes("\nendstream"));
            objs.add(s.toByteArray());
        }

        // 3) escreve o arquivo com a tabela de referências (xref)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bytes("%PDF-1.4\n"));
        long[] pos = new long[objs.size() + 1];
        for (int i = 0; i < objs.size(); i++) {
            pos[i + 1] = out.size();
            out.write(bytes((i + 1) + " 0 obj\n"));
            out.write(objs.get(i));
            out.write(bytes("\nendobj\n"));
        }
        long xref = out.size();
        out.write(bytes("xref\n0 " + (objs.size() + 1) + "\n0000000000 65535 f \n"));
        for (int i = 1; i <= objs.size(); i++) out.write(bytes(String.format("%010d 00000 n \n", pos[i])));
        out.write(bytes("trailer\n<< /Size " + (objs.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));

        if (destino.getParent() != null) Files.createDirectories(destino.getParent());
        Files.write(destino, out.toByteArray());
    }

    private static byte[] bytes(String s) { return s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1); }

    /** Texto em WinAnsi, com \, ( e ) protegidos. Caracteres que a fonte não tem viram "?". */
    private static byte[] escapar(String t) {
        byte[] b = t.getBytes(WIN);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte x : b) {
            if (x == '\\' || x == '(' || x == ')') o.write('\\');
            o.write(x);
        }
        return o.toByteArray();
    }
}
