package controller;

import model.*;
import util.Pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CONTROLLER dos documentos (UC13/RF13): gera o contrato e o recibo em PDF e controla as VERSÕES.
 *  - Se os dados não mudaram desde a última versão, devolve a mesma (só consulta).
 *  - Se mudaram (itens, datas, valores...), cria uma versão nova e MANTÉM as anteriores.
 * A assinatura do contrato é feita fora do sistema (não há controle de assinatura).
 * Os arquivos vão para a pasta "documentos" (ou a indicada em -Dariel.docs=pasta).
 */
public class DocumentoController {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private final Path pasta = Path.of(System.getProperty("ariel.docs", "documentos"));

    public Path pasta() { return pasta; }

    /** Contrato da locação (nova versão só se algo mudou). */
    public Documento contrato(Locacao l, LocalDate hoje) throws IOException {
        return gerar(l, "Contrato", assinaturaContrato(l), hoje);
    }

    /** Recibo dos pagamentos recebidos até agora (nova versão só se entrou pagamento novo). */
    public Documento recibo(Locacao l, LocalDate hoje) throws IOException {
        return gerar(l, "Recibo", assinaturaRecibo(l), hoje);
    }

    private Documento gerar(Locacao l, String tipo, String assinatura, LocalDate hoje) throws IOException {
        int versoes = 0;
        for (Documento d : l.documentos) {
            if (!d.tipo().equals(tipo)) continue;
            versoes = Math.max(versoes, d.versao());
            if (d.assinatura().equals(assinatura) && d.versao() == versoes && Path.of(d.arquivo()).toFile().exists())
                return d;                                          // nada mudou: é só consultar
        }
        int versao = versoes + 1;
        Path arquivo = pasta.resolve(tipo.toLowerCase() + "_L" + String.format("%03d", l.id) + "_v" + versao + ".pdf");
        Pdf pdf = tipo.equals("Contrato") ? montarContrato(l, versao, hoje) : montarRecibo(l, versao, hoje);
        pdf.gravar(arquivo);
        Documento doc = new Documento(tipo, versao, hoje, arquivo.toString(), assinatura);
        l.documentos.add(doc);
        return doc;
    }

    // ================= Conteúdo =================

    private Pdf montarContrato(Locacao l, int versao, LocalDate hoje) {
        Cliente c = l.cliente;
        Pdf p = new Pdf();
        p.titulo("CONTRATO DE LOCAÇÃO Nº " + l.codigo().substring(1) + " · versão " + versao);
        p.texto("Conto de Fadas Festas e Eventos (LOCADORA)  ×  " + c.nome + " (LOCATÁRIO(A))");
        p.texto("Emitido em " + DATA.format(hoje) + ".");

        p.subtitulo("1. IDENTIFICAÇÃO DO LOCATÁRIO");
        p.texto("Nome: " + c.nome + "    CPF: " + c.cpf + (c.rg.isEmpty() ? "" : "    RG: " + c.rg));
        p.texto("Endereço: " + c.endereco);
        p.texto("Contato: " + c.whatsapp + (c.instagram.isEmpty() ? "" : "    Instagram: " + c.instagram));

        p.subtitulo("2. OBJETO E PRAZOS");
        p.texto("Locação dos produtos e serviços abaixo, na modalidade \"" + l.modalidade + "\".");
        p.texto("Retirada/entrega: " + data(l.retirada) + "    Evento: " + data(l.evento)
                + "    Devolução: " + data(l.devolucao));

        p.subtitulo("3. ITENS");
        for (Catalogo.Linha li : l.itens) {
            p.mono(String.format("%3d x %-34s %12s", li.qtd, corta(li.item.nome, 34), PainelController.moeda(li.subtotal())));
            if (li.desconto > 0.005) p.mono("      (desconto de " + String.format(TelaFmt.PT, "%.1f", li.desconto) + "% aplicado)");
            if (li.item.tipo == Catalogo.Tipo.PACOTE) p.mono("      inclui: " + li.item.composicao());
        }
        for (ServicoContratado s : l.servicos)
            p.mono(String.format("  1 x %-34s %12s", corta("Serviço: " + s.servico.nome, 34), PainelController.moeda(s.valor)));

        p.subtitulo("4. VALORES E PAGAMENTO");
        p.mono(String.format("Valor total da locação ........ %12s", PainelController.moeda(l.totalItens())));
        p.mono(String.format("Pago até agora (sinal/saldo) .... %12s", PainelController.moeda(l.pagoItens())));
        p.mono(String.format("Saldo a pagar ................... %12s", PainelController.moeda(l.saldoItens())));
        p.texto("O saldo deve ser quitado integralmente até a retirada. O sinal pago reserva as datas e os itens e NÃO é "
                + "reembolsado em caso de cancelamento. Eventual valor pago a maior é devolvido (não vira crédito).");

        p.subtitulo("5. RETIRADA E DEVOLUÇÃO");
        StringBuilder aut = new StringBuilder();
        for (Cliente.Autorizada a : l.autorizados) aut.append(aut.length() > 0 ? "; " : "").append(a.nome).append(" (").append(a.documento).append(")");
        p.texto("A retirada é feita pelo(a) locatário(a)" + (aut.length() > 0 ? " ou pelas pessoas autorizadas: " + aut : "")
                + ". A devolução pode ser feita por qualquer pessoa identificada.");
        p.texto("Na devolução os itens são conferidos. Danos, perdas e atrasos geram cobrança definida pela LOCADORA, "
                + "conforme o caso, e registrada em ocorrência.");

        p.subtitulo("6. ASSINATURAS (fora do sistema)");
        p.espaco();
        p.texto("______________________________          ______________________________");
        p.texto("Conto de Fadas Festas e Eventos             " + c.nome);
        return p;
    }

    private Pdf montarRecibo(Locacao l, int versao, LocalDate hoje) {
        Pdf p = new Pdf();
        p.titulo("RECIBO DE PAGAMENTO · Locação " + l.codigo() + " · versão " + versao);
        p.texto("Conto de Fadas Festas e Eventos recebeu de " + l.cliente.nome + " (CPF " + l.cliente.cpf + ") os valores abaixo.");
        p.texto("Emitido em " + DATA.format(hoje) + ".");
        p.subtitulo("PAGAMENTOS");
        if (l.pagamentos.isEmpty()) p.texto("(nenhum pagamento registrado)");
        for (Pagamento g : l.pagamentos)
            p.mono(String.format("%s  %-20s %-18s %12s", DATA.format(g.data()), g.tipo().nome, g.forma(), PainelController.moeda(g.valor())));
        p.subtitulo("RESUMO");
        p.mono(String.format("Total da locação .......... %12s", PainelController.moeda(l.total())));
        p.mono(String.format("Total recebido ............ %12s", PainelController.moeda(l.pago())));
        p.mono(String.format("Saldo em aberto ........... %12s", PainelController.moeda(l.saldo())));
        return p;
    }

    // ================= Assinaturas (para saber se algo mudou) =================

    private static String assinaturaContrato(Locacao l) {
        StringBuilder sb = new StringBuilder();
        sb.append(l.cliente.cpf).append(l.cliente.nome).append(l.cliente.endereco).append(l.modalidade)
          .append(l.retirada).append(l.evento).append(l.devolucao);
        for (Catalogo.Linha li : l.itens) sb.append('|').append(li.item.nome).append(li.qtd).append(li.desconto).append(li.item.preco);
        for (ServicoContratado s : l.servicos) sb.append('|').append(s.servico.nome).append(s.valor);
        for (Cliente.Autorizada a : l.autorizados) sb.append('|').append(a.nome);
        return Integer.toHexString(sb.toString().hashCode());
    }

    private static String assinaturaRecibo(Locacao l) {
        StringBuilder sb = new StringBuilder();
        for (Pagamento g : l.pagamentos) sb.append(g.data()).append(g.valor()).append(g.tipo()).append(g.forma());
        sb.append(l.total());
        return Integer.toHexString(sb.toString().hashCode());
    }

    private static String data(LocalDate d) { return d == null ? "-" : DATA.format(d); }

    private static String corta(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "."; }

    /** Só para formatar números com vírgula. */
    private static final class TelaFmt { static final java.util.Locale PT = java.util.Locale.forLanguageTag("pt-BR"); }

    /** Documentos de um tipo, do mais novo para o mais antigo. */
    public static List<Documento> versoes(Locacao l, String tipo) {
        List<Documento> r = new java.util.ArrayList<>();
        for (Documento d : l.documentos) if (d.tipo().equals(tipo)) r.add(0, d);
        return r;
    }
}
