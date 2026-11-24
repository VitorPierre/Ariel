package view;

import controller.RelatorioController.Relatorio;
import controller.RelatorioController.Tipo;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * VIEW: constrói gráficos JFreeChart estilizados com a paleta oficial do sistema (Tema.java)
 * para cada tipo de relatório de RelatorioController.
 */
final class GraficosRelatorio {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final Color[] PALETA = {
            Tema.PRIMARIA,          // rosa principal
            Tema.LOCADO,            // rosa médio
            Tema.DEVOLVIDO,         // vinho
            Tema.ORCAMENTO,         // rosa claro
            Tema.PRIMARIA_PRESS,    // vinho escuro
            Tema.BORDA_CAMPO        // rosa suave
    };

    private GraficosRelatorio() {}

    /** Cria o gráfico correspondente ao relatório, ou null se não houver dados. */
    static JFreeChart criar(Relatorio r) {
        if (r == null || r.tipo == null || r.linhas.isEmpty()) return null;
        JFreeChart chart = switch (r.tipo) {
            case FATURAMENTO   -> faturamento(r);
            case LOCACOES      -> locacoes(r);
            case PRODUTOS      -> produtos(r);
            case OCORRENCIAS   -> ocorrencias(r);
            case DEMANDA       -> demanda(r);
            case INADIMPLENCIA -> inadimplencia(r);
            case FLUXO_CAIXA   -> fluxoCaixa(r);
            case AGENDA        -> agenda(r);
        };
        if (chart != null) estilizar(chart);
        return chart;
    }

    // ---------- Faturamento: pizza por forma de pagamento ----------
    private static JFreeChart faturamento(Relatorio r) {
        Map<String, Double> porForma = new LinkedHashMap<>();
        for (String[] l : r.linhas) {
            if (l.length < 6) continue;
            porForma.merge(l[4], moeda(l[5]), Double::sum);
        }
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        for (Map.Entry<String, Double> e : porForma.entrySet()) {
            if (e.getValue() > 0) ds.setValue(e.getKey(), e.getValue());
        }
        if (ds.getItemCount() == 0) return null;
        JFreeChart chart = ChartFactory.createPieChart("Faturamento por forma de pagamento", ds, true, true, false);
        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})", formatoMoeda(), NumberFormat.getPercentInstance(PT_BR)));
        return chart;
    }

    // ---------- Locações: barras Pago vs Saldo por etapa ----------
    private static JFreeChart locacoes(Relatorio r) {
        Map<String, double[]> porEtapa = new LinkedHashMap<>();
        for (String[] l : r.linhas) {
            if (l.length < 7) continue;
            double[] acc = porEtapa.computeIfAbsent(l[2], k -> new double[2]);
            acc[0] += moeda(l[5]); // Pago
            acc[1] += moeda(l[6]); // Saldo
        }
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map.Entry<String, double[]> e : porEtapa.entrySet()) {
            ds.addValue(e.getValue()[0], "Pago", e.getKey());
            ds.addValue(e.getValue()[1], "A receber", e.getKey());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Valores das locações por etapa", "Etapa", "Valor (R$)", ds,
                PlotOrientation.VERTICAL, true, true, false);
        aplicarEixoMoeda(chart);
        return chart;
    }

    // ---------- Produtos: barras horizontais dos Top 10 mais locados ----------
    private static JFreeChart produtos(Relatorio r) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        int limite = Math.min(10, r.linhas.size());
        for (int i = 0; i < limite; i++) {
            String[] l = r.linhas.get(i);
            if (l.length < 3) continue;
            ds.addValue(inteiro(l[2]), "Unidades locadas", truncar(l[0], 26));
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Produtos mais locados (Top " + limite + ")", "Produto", "Qtd. locada", ds,
                PlotOrientation.HORIZONTAL, false, true, false);
        aplicarEixoInteiro(chart);
        return chart;
    }

    // ---------- Ocorrências: pizza por tipo ----------
    private static JFreeChart ocorrencias(Relatorio r) {
        Map<String, Integer> porTipo = new LinkedHashMap<>();
        for (String[] l : r.linhas) {
            if (l.length < 4) continue;
            porTipo.merge(l[3], 1, Integer::sum);
        }
        DefaultPieDataset<String> ds = new DefaultPieDataset<>();
        for (Map.Entry<String, Integer> e : porTipo.entrySet()) ds.setValue(e.getKey(), e.getValue());
        if (ds.getItemCount() == 0) return null;
        JFreeChart chart = ChartFactory.createPieChart("Ocorrências por tipo", ds, true, true, false);
        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})", NumberFormat.getIntegerInstance(PT_BR), NumberFormat.getPercentInstance(PT_BR)));
        return chart;
    }

    // ---------- Demanda por tema: barras Clientes distintos vs Orçamentos ----------
    private static JFreeChart demanda(Relatorio r) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (String[] l : r.linhas) {
            if (l.length < 3) continue;
            String tema = truncar(l[0], 22);
            ds.addValue(inteiro(l[1]), "Clientes distintos", tema);
            ds.addValue(inteiro(l[2]), "Orçamentos", tema);
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Demanda por tema", "Tema", "Quantidade", ds,
                PlotOrientation.VERTICAL, true, true, false);
        aplicarEixoInteiro(chart);
        return chart;
    }

    // ---------- Inadimplência: barras horizontais de saldo em aberto por cliente ----------
    private static JFreeChart inadimplencia(Relatorio r) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        int limite = Math.min(10, r.linhas.size());
        for (int i = 0; i < limite; i++) {
            String[] l = r.linhas.get(i);
            if (l.length < 7) continue;
            double v = moeda(l[6]);
            if (v > 0) ds.addValue(v, "Total em aberto", truncar(l[0], 24));
        }
        if (ds.getColumnCount() == 0) return null;
        JFreeChart chart = ChartFactory.createBarChart(
                "Valores em aberto por cliente", "Cliente", "Valor em aberto (R$)", ds,
                PlotOrientation.HORIZONTAL, false, true, false);
        aplicarEixoMoeda(chart);
        return chart;
    }

    // ---------- Fluxo de caixa: linhas (Total do dia e Acumulado) ----------
    private static JFreeChart fluxoCaixa(Relatorio r) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        int cols = r.colunas.length;
        if (cols < 3) return null;
        for (String[] l : r.linhas) {
            if (l.length < cols) continue;
            String data = l[0].length() >= 5 ? l[0].substring(0, 5) : l[0];
            ds.addValue(moeda(l[cols - 2]), "Total do dia", data);
            ds.addValue(moeda(l[cols - 1]), "Acumulado", data);
        }
        JFreeChart chart = ChartFactory.createLineChart(
                "Fluxo de caixa no período", "Data", "Valor (R$)", ds,
                PlotOrientation.VERTICAL, true, true, false);
        aplicarEixoMoeda(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        LineAndShapeRenderer ren = (LineAndShapeRenderer) plot.getRenderer();
        ren.setDefaultShapesVisible(true);
        ren.setSeriesStroke(0, new BasicStroke(2.5f));
        ren.setSeriesStroke(1, new BasicStroke(2.5f));
        return chart;
    }

    // ---------- Agenda: barras por tipo de movimento ----------
    private static JFreeChart agenda(Relatorio r) {
        Map<String, Integer> porMov = new LinkedHashMap<>();
        for (String[] l : r.linhas) {
            if (l.length < 2) continue;
            porMov.merge(l[1], 1, Integer::sum);
        }
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> e : porMov.entrySet()) {
            ds.addValue(e.getValue(), "Movimentos", e.getKey());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Movimentos na agenda", "Tipo de movimento", "Quantidade", ds,
                PlotOrientation.VERTICAL, false, true, false);
        aplicarEixoInteiro(chart);
        return chart;
    }

    // ================= Estilização (DESIGN.md / Tema.java) =================

    private static void estilizar(JFreeChart chart) {
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        chart.setBackgroundPaint(Tema.SUPERFICIE);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(Tema.titulo(Font.BOLD, 16));
            chart.getTitle().setPaint(Tema.TEXTO);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(Tema.fonte(Font.PLAIN, 12));
            chart.getLegend().setItemPaint(Tema.TEXTO);
            chart.getLegend().setBackgroundPaint(Tema.SUPERFICIE);
            chart.getLegend().setFrame(org.jfree.chart.block.BlockBorder.NONE);
        }

        if (chart.getPlot() instanceof CategoryPlot plot) {
            plot.setBackgroundPaint(Tema.SUPERFICIE);
            plot.setOutlineVisible(false);
            plot.setRangeGridlinePaint(Tema.LINHA_TABELA);
            plot.setRangeGridlineStroke(new BasicStroke(1f));
            plot.setDomainGridlinesVisible(false);

            CategoryAxis eixoX = plot.getDomainAxis();
            eixoX.setLabelFont(Tema.fonte(Font.BOLD, 12));
            eixoX.setLabelPaint(Tema.TEXTO_SUAVE);
            eixoX.setTickLabelFont(Tema.fonte(Font.PLAIN, 11));
            eixoX.setTickLabelPaint(Tema.TEXTO);
            if (plot.getOrientation() == PlotOrientation.VERTICAL && plot.getDataset().getColumnCount() > 5) {
                eixoX.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
            }

            NumberAxis eixoY = (NumberAxis) plot.getRangeAxis();
            eixoY.setLabelFont(Tema.fonte(Font.BOLD, 12));
            eixoY.setLabelPaint(Tema.TEXTO_SUAVE);
            eixoY.setTickLabelFont(Tema.fonte(Font.PLAIN, 11));
            eixoY.setTickLabelPaint(Tema.TEXTO);

            if (plot.getRenderer() instanceof BarRenderer br) {
                br.setBarPainter(new StandardBarPainter());
                br.setShadowVisible(false);
                br.setMaximumBarWidth(0.18);
                for (int i = 0; i < PALETA.length; i++) br.setSeriesPaint(i, PALETA[i]);
            } else {
                for (int i = 0; i < PALETA.length; i++) plot.getRenderer().setSeriesPaint(i, PALETA[i]);
            }
        } else if (chart.getPlot() instanceof PiePlot<?> plot) {
            plot.setBackgroundPaint(Tema.SUPERFICIE);
            plot.setOutlineVisible(false);
            plot.setShadowPaint(null);
            plot.setLabelFont(Tema.fonte(Font.PLAIN, 12));
            plot.setLabelPaint(Tema.TEXTO);
            plot.setLabelBackgroundPaint(Tema.ROSA_CLARO);
            plot.setLabelOutlinePaint(Tema.BORDA_SECAO);
            plot.setLabelShadowPaint(null);
            int idx = 0;
            for (Object key : plot.getDataset().getKeys()) {
                plot.setSectionPaint((Comparable<?>) key, PALETA[idx % PALETA.length]);
                idx++;
            }
        }
    }

    private static void aplicarEixoMoeda(JFreeChart chart) {
        NumberAxis y = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        y.setNumberFormatOverride(formatoMoeda());
    }

    private static void aplicarEixoInteiro(JFreeChart chart) {
        NumberAxis y = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        y.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    }

    private static NumberFormat formatoMoeda() {
        DecimalFormat df = new DecimalFormat("R$ #,##0.00", new DecimalFormatSymbols(PT_BR));
        return df;
    }

    private static double moeda(String texto) {
        if (texto == null) return 0;
        String limpo = texto.replaceAll("[^0-9,-]", "").replace(",", ".");
        try { return Double.parseDouble(limpo); } catch (NumberFormatException e) { return 0; }
    }

    private static int inteiro(String texto) {
        if (texto == null) return 0;
        try { return Integer.parseInt(texto.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static String truncar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
