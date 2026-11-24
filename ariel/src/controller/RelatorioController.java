package controller;

import model.*;
import util.Pdf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CONTROLLER dos relatórios gerenciais (UC18, UC17, UC14, RF19). Só o ADMINISTRADOR emite relatórios.
 * Os relatórios só LEEM os dados (nunca alteram) e podem ser exportados em CSV (abre no Excel) e PDF.
 * Regras: orçamento pendente (sem sinal) não é faturamento; pagamentos recebidos ficam separados de cobranças.
 */
public class RelatorioController {

    public enum Tipo {
        FATURAMENTO("Faturamento (pagamentos recebidos)"),
        LOCACOES("Locações do período"),
        PRODUTOS("Produtos mais locados"),
        OCORRENCIAS("Ocorrências (atrasos, danos e perdas)"),
        DEMANDA("Demanda por tema (clientes distintos)"),
        INADIMPLENCIA("Inadimplência (clientes e valores em aberto)"),
        FLUXO_CAIXA("Fluxo de caixa (recebimentos por dia)"),
        AGENDA("Agenda do período");

        public final String nome;
        Tipo(String nome) { this.nome = nome; }
        @Override public String toString() { return nome; }
    }

    /** Resultado pronto para a tela e para exportar. */
    public static class Relatorio {
        public Tipo tipo;
        public String titulo, periodo, criterio = "";
        public String[] colunas = {};
        public final List<String[]> linhas = new ArrayList<>();
        public final List<String> totais = new ArrayList<>();
    }

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private final PainelController painel;

    public RelatorioController(PainelController painel) { this.painel = painel; }

    /**
     * Gera o relatório. filtro: situação (AGENDA: Todas/Orçamento/Locado/Devolvido) ou nome do tema (DEMANDA; null = todos).
     */
    public Relatorio gerar(Tipo tipo, LocalDate de, LocalDate ate, String filtro, Usuario quem) {
        if (quem == null || !quem.ehAdmin()) throw new RegraNegocioException("Acesso restrito: só o administrador emite relatórios.");
        if (de == null || ate == null || ate.isBefore(de)) throw new RegraNegocioException(Mensagens.MSG011 + "\n(período inválido)");
        Relatorio r = new Relatorio();
        r.tipo = tipo;
        r.titulo = tipo.nome;
        r.periodo = DATA.format(de) + " a " + DATA.format(ate);
        switch (tipo) {
            case FATURAMENTO: faturamento(r, de, ate); break;
            case LOCACOES:    locacoes(r, de, ate); break;
            case PRODUTOS:    produtos(r, de, ate); break;
            case OCORRENCIAS: ocorrencias(r, de, ate); break;
            case DEMANDA:     demanda(r, de, ate, filtro); break;
            case INADIMPLENCIA: inadimplencia(r); break;
            case FLUXO_CAIXA: fluxoCaixa(r, de, ate); break;
            default:          agenda(r, de, ate, filtro); break;
        }
        return r;
    }

    private static boolean dentro(LocalDate d, LocalDate de, LocalDate ate) {
        return d != null && !d.isBefore(de) && !d.isAfter(ate);
    }

    /** Orçamento em negociação (sem sinal) ou arquivado não é locação confirmada. */
    private static boolean confirmada(Locacao l) {
        return !l.arquivada && (l.etapa != Etapa.ORCAMENTO || l.temEntrada());
    }

    private static String d(LocalDate x) { return x == null ? "-" : DATA.format(x); }

    private static String m(double v) { return PainelController.moeda(v); }

    // ---------- faturamento ----------
    private void faturamento(Relatorio r, LocalDate de, LocalDate ate) {
        r.colunas = new String[]{"Data", "Locação", "Cliente", "Tipo", "Forma", "Valor"};
        double entradas = 0, saldos = 0, cobrancas = 0;
        List<Object[]> tmp = new ArrayList<>();
        for (Locacao l : painel.locacoes)
            for (Pagamento p : l.pagamentos)
                if (dentro(p.data(), de, ate)) tmp.add(new Object[]{p, l});
        tmp.sort(Comparator.comparing(o -> ((Pagamento) o[0]).data()));
        for (Object[] o : tmp) {
            Pagamento p = (Pagamento) o[0];
            Locacao l = (Locacao) o[1];
            r.linhas.add(new String[]{d(p.data()), l.codigo(), l.cliente.nome, p.tipo().nome, p.forma().toString(), m(p.valor())});
            if (p.tipo() == Pagamento.Tipo.ENTRADA) entradas += p.valor();
            else if (p.tipo() == Pagamento.Tipo.SALDO) saldos += p.valor();
            else cobrancas += p.valor();
        }
        r.totais.add("Entradas (sinais): " + m(entradas));
        r.totais.add("Saldos: " + m(saldos));
        r.totais.add("Multas e indenizações recebidas: " + m(cobrancas));
        r.totais.add("TOTAL RECEBIDO: " + m(entradas + saldos + cobrancas));
        r.criterio = "Pagamentos efetivamente recebidos no período. Orçamentos pendentes não entram. (Estornos e reembolsos: ainda não implementados.)";
    }

    // ---------- locações ----------
    private void locacoes(Relatorio r, LocalDate de, LocalDate ate) {
        r.colunas = new String[]{"Locação", "Cliente", "Etapa", "Evento", "Total", "Pago", "Saldo"};
        double total = 0, pago = 0, saldo = 0;
        List<Locacao> lista = new ArrayList<>();
        for (Locacao l : painel.locacoes) if (confirmada(l) && dentro(l.evento, de, ate)) lista.add(l);
        lista.sort(Comparator.comparing(l -> l.evento));
        for (Locacao l : lista) {
            r.linhas.add(new String[]{l.codigo(), l.cliente.nome, l.etapa.nome, d(l.evento), m(l.total()), m(l.pago()), m(l.saldo())});
            total += l.total(); pago += l.pago(); saldo += l.saldo();
        }
        r.totais.add(lista.size() + " locação(ões) com evento no período");
        r.totais.add("Valor total: " + m(total) + "   Pago: " + m(pago) + "   A receber: " + m(saldo));
        r.criterio = "Locações confirmadas (com sinal) cujo evento cai no período. Orçamentos sem sinal não são contados.";
    }

    // ---------- produtos ----------
    private void produtos(Relatorio r, LocalDate de, LocalDate ate) {
        r.colunas = new String[]{"Produto", "Categoria", "Qtd. locada", "Locações", "Estoque total", "Indisponíveis"};
        Map<Catalogo.Item, int[]> uso = new LinkedHashMap<>();
        for (Locacao l : painel.locacoes) {
            if (!confirmada(l) || !dentro(l.evento, de, ate)) continue;
            Map<Catalogo.Item, Integer> porProduto = new LinkedHashMap<>();
            for (Catalogo.Linha li : l.itens)
                for (Catalogo.Item p : li.item.produtos()) porProduto.merge(p, li.qtd * li.item.consumo(p), Integer::sum);
            for (Map.Entry<Catalogo.Item, Integer> e : porProduto.entrySet()) {
                int[] u = uso.computeIfAbsent(e.getKey(), k -> new int[2]);
                u[0] += e.getValue();
                u[1]++;
            }
        }
        List<Map.Entry<Catalogo.Item, int[]>> ordem = new ArrayList<>(uso.entrySet());
        ordem.sort((a, b) -> b.getValue()[0] - a.getValue()[0]);
        int unidades = 0;
        for (Map.Entry<Catalogo.Item, int[]> e : ordem) {
            Catalogo.Item p = e.getKey();
            r.linhas.add(new String[]{p.nome, p.categoria == null ? "-" : p.categoria.nome, String.valueOf(e.getValue()[0]),
                    String.valueOf(e.getValue()[1]), String.valueOf(p.estoque), String.valueOf(p.indisponivel)});
            unidades += e.getValue()[0];
        }
        r.totais.add(ordem.size() + " produto(s) movimentado(s), " + unidades + " unidade(s) locada(s)");
        r.criterio = "Quantidades por produto, desmembrando pacotes. Considera locações confirmadas com evento no período.";
    }

    // ---------- ocorrências ----------
    private void ocorrencias(Relatorio r, LocalDate de, LocalDate ate) {
        r.colunas = new String[]{"Data", "Locação", "Cliente", "Tipo", "Descrição", "Valor", "Registrada por"};
        double total = 0;
        int n = 0;
        for (Locacao l : painel.locacoes)
            for (Ocorrencia o : l.ocorrencias)
                if (dentro(o.data, de, ate)) {
                    r.linhas.add(new String[]{d(o.data), l.codigo(), l.cliente.nome, o.tipo.toString(), o.descricao, m(o.valor), o.registradoPor});
                    total += o.valor;
                    n++;
                }
        r.linhas.sort(Comparator.comparing(x -> x[0].substring(6) + x[0].substring(3, 5) + x[0].substring(0, 2)));
        r.totais.add(n + " ocorrência(s)   Valor cobrado: " + m(total));
        r.criterio = "Valores definidos manualmente pela proprietária. Cobrança lançada não significa cobrança paga.";
    }

    // ---------- demanda por tema (UC17) ----------
    private void demanda(Relatorio r, LocalDate de, LocalDate ate, String filtroTema) {
        r.colunas = new String[]{"Tema", "Clientes distintos", "Orçamentos"};
        boolean todos = filtroTema == null || filtroTema.isEmpty() || filtroTema.startsWith("Todos");
        Map<Catalogo.Tema, Set<String>> clientes = new LinkedHashMap<>();
        Map<Catalogo.Tema, Integer> orcs = new LinkedHashMap<>();
        for (Catalogo.Tema t : Catalogo.TEMAS) if (todos || t.nome.equals(filtroTema)) { clientes.put(t, new TreeSet<>()); orcs.put(t, 0); }
        for (Locacao l : painel.locacoes) {
            if (!dentro(l.emissao, de, ate)) continue;
            Set<Catalogo.Tema> deste = new HashSet<>();
            for (Catalogo.Linha li : l.itens) if (li.item.tipo == Catalogo.Tipo.PACOTE && li.item.tema != null) deste.add(li.item.tema);
            for (Catalogo.Tema t : deste)
                if (clientes.containsKey(t)) { clientes.get(t).add(l.cliente.cpf); orcs.merge(t, 1, Integer::sum); }
        }
        for (Map.Entry<Catalogo.Tema, Set<String>> e : clientes.entrySet())
            r.linhas.add(new String[]{e.getKey().nome + (e.getKey().ativo ? "" : " (inativo)"),
                    String.valueOf(e.getValue().size()), String.valueOf(orcs.get(e.getKey()))});
        r.totais.add(todos ? "Cada cliente conta uma só vez por tema, mesmo com vários orçamentos."
                : "Tema \"" + filtroTema + "\": " + clientes.values().iterator().next().size() + " cliente(s) distinto(s).");
        r.criterio = "Conta clientes distintos com orçamentos emitidos no período que incluem pacotes do tema. "
                + "Interesse não é locação confirmada; contatos não registrados não contam.";
    }

    // ---------- inadimplência (UC18) ----------
    /** Clientes marcados como inadimplentes e todos os valores em aberto (saldo de itens e multas/indenizações). */
    private void inadimplencia(Relatorio r) {
        r.colunas = new String[]{"Cliente", "CPF", "Situação", "Locações em aberto", "Saldo dos itens", "Multas/indenizações", "Total em aberto"};
        double totItens = 0, totCob = 0;
        int marcados = 0;
        for (Cliente c : painel.clientes) {
            double itens = 0, cob = 0;
            int abertas = 0;
            for (Locacao l : painel.locacoes) {
                if (l.cliente != c || !confirmada(l) || !l.temSaldo()) continue;
                abertas++;
                itens += l.saldoItens();
                cob += l.saldoCobrancas();
            }
            boolean inad = painel.inadimplente(c);
            if (!inad && abertas == 0) continue;
            if (inad) marcados++;
            r.linhas.add(new String[]{c.nome + (c.ativo ? "" : " (inativo)"), c.cpf, inad ? "Inadimplente" : "Em dia",
                    String.valueOf(abertas), m(itens), m(cob), m(itens + cob)});
            totItens += itens;
            totCob += cob;
        }
        r.linhas.sort((a, b) -> Double.compare(valor(b[6]), valor(a[6])));
        r.totais.add(marcados + " cliente(s) marcado(s) como inadimplente(s)");
        r.totais.add("Em aberto: itens " + m(totItens) + " + multas/indenizações " + m(totCob) + " = " + m(totItens + totCob));
        r.criterio = "Situação atual (não depende do período). Inadimplência é marcada manualmente; "
                + "valores em aberto vêm das locações confirmadas.";
    }

    private static double valor(String moeda) {
        String t = moeda.replaceAll("[^0-9,]", "").replace(",", ".");
        try { return Double.parseDouble(t); } catch (NumberFormatException e) { return 0; }
    }

    // ---------- fluxo de caixa (UC18) ----------
    /** Recebimentos agrupados por dia, separados por forma de pagamento, com acumulado do período. */
    private void fluxoCaixa(Relatorio r, LocalDate de, LocalDate ate) {
        Pagamento.Forma[] formas = Pagamento.Forma.values();
        r.colunas = new String[formas.length + 3];
        r.colunas[0] = "Data";
        for (int i = 0; i < formas.length; i++) r.colunas[i + 1] = formas[i].toString();
        r.colunas[formas.length + 1] = "Total do dia";
        r.colunas[formas.length + 2] = "Acumulado";
        java.util.TreeMap<LocalDate, double[]> dias = new java.util.TreeMap<>();
        for (Locacao l : painel.locacoes)
            for (Pagamento p : l.pagamentos)
                if (dentro(p.data(), de, ate)) dias.computeIfAbsent(p.data(), k -> new double[formas.length])[p.forma().ordinal()] += p.valor();
        double acumulado = 0;
        double[] porForma = new double[formas.length];
        for (Map.Entry<LocalDate, double[]> e : dias.entrySet()) {
            String[] linha = new String[formas.length + 3];
            linha[0] = d(e.getKey());
            double dia = 0;
            for (int i = 0; i < formas.length; i++) { linha[i + 1] = m(e.getValue()[i]); dia += e.getValue()[i]; porForma[i] += e.getValue()[i]; }
            acumulado += dia;
            linha[formas.length + 1] = m(dia);
            linha[formas.length + 2] = m(acumulado);
            r.linhas.add(linha);
        }
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < formas.length; i++) t.append(i > 0 ? "   " : "").append(formas[i]).append(": ").append(m(porForma[i]));
        r.totais.add(t.toString());
        r.totais.add("TOTAL DE ENTRADAS NO CAIXA: " + m(acumulado));
        r.criterio = "Só entradas de caixa efetivamente registradas (sinais, saldos, multas e indenizações). "
                + "Saídas e estornos não são controlados pelo sistema.";
    }

    // ---------- agenda (UC14) ----------
    private void agenda(Relatorio r, LocalDate de, LocalDate ate, String situacao) {
        r.colunas = new String[]{"Data", "Movimento", "Locação", "Cliente", "Situação"};
        List<String[]> l2 = new ArrayList<>();
        for (Locacao l : painel.locacoes) {
            if (l.arquivada) continue;
            if (situacao != null && !situacao.equals("Todas") && !l.etapa.nome.equals(situacao)) continue;
            String sit = l.etapa == Etapa.ORCAMENTO ? (l.temEntrada() ? "Reserva confirmada" : "Orçamento (não reserva)") : l.etapa.nome;
            if (l.etapa == Etapa.DEVOLVIDO) { add(l2, l.retirada, "Retirada efetiva", l, sit, de, ate); }
            else add(l2, l.retirada, l.etapa == Etapa.LOCADO ? "Retirada efetiva" : "Retirada prevista", l, sit, de, ate);
            add(l2, l.evento, "Evento", l, sit, de, ate);
            add(l2, l.etapa == Etapa.DEVOLVIDO ? l.devolvidoEm : l.devolucao,
                    l.etapa == Etapa.DEVOLVIDO ? "Devolução efetiva" : "Devolução prevista", l, sit, de, ate);
        }
        l2.sort(Comparator.comparing(x -> x[0].substring(6) + x[0].substring(3, 5) + x[0].substring(0, 2)));
        r.linhas.addAll(l2);
        r.totais.add(l2.size() + " movimento(s) no período");
        r.criterio = "Orçamento em negociação não é reserva confirmada; a agenda não substitui a validação de disponibilidade.";
    }

    private static void add(List<String[]> l, LocalDate data, String mov, Locacao loc, String sit, LocalDate de, LocalDate ate) {
        if (dentro(data, de, ate)) l.add(new String[]{d(data), mov, loc.codigo(), loc.cliente.nome, sit});
    }

    // ================= Exportação =================

    /** CSV com ponto e vírgula e BOM (o Excel brasileiro abre direto, com acentos corretos). */
    public void exportarCsv(Relatorio r, Path destino) throws IOException {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(r.titulo).append(" (").append(r.periodo).append(")\n");
        sb.append(String.join(";", r.colunas)).append('\n');
        for (String[] l : r.linhas) {
            for (int i = 0; i < l.length; i++) sb.append(i > 0 ? ";" : "").append('"').append(l[i].replace("\"", "\"\"")).append('"');
            sb.append('\n');
        }
        for (String t : r.totais) sb.append(t).append('\n');
        Files.write(destino, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** PDF simples com a tabela em fonte monoespaçada. */
    public void exportarPdf(Relatorio r, Path destino) throws IOException {
        int[] larg = new int[r.colunas.length];
        for (int i = 0; i < larg.length; i++) {
            larg[i] = r.colunas[i].length();
            for (String[] l : r.linhas) larg[i] = Math.max(larg[i], l[i].length());
            larg[i] = Math.min(larg[i], 24);
        }
        Pdf pdf = new Pdf();
        pdf.titulo("Ariel · " + r.titulo);
        pdf.texto("Período: " + r.periodo + "    Emitido em " + DATA.format(painel.hoje()));
        pdf.espaco();
        pdf.mono(linha(r.colunas, larg));
        for (String[] l : r.linhas) pdf.mono(linha(l, larg));
        pdf.espaco();
        for (String t : r.totais) pdf.texto(t);
        pdf.espaco();
        pdf.texto(r.criterio);
        pdf.gravar(destino);
    }

    private static String linha(String[] cel, int[] larg) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cel.length; i++) {
            String c = cel[i].length() > larg[i] ? cel[i].substring(0, larg[i] - 1) + "." : cel[i];
            sb.append(String.format("%-" + larg[i] + "s ", c));
        }
        return sb.toString();
    }
}
