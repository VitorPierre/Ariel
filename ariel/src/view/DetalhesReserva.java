package view;

import controller.PainelController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * VIEW: "Detalhes da reserva". Mostra tudo da locação e os botões de ação.
 * Botões: Fechar, Ver cadastro, Registrar retirada / Abrir devolução (conforme a etapa),
 * Quitar saldo (ou Registrar entrada / Quitar pendências) e Excluir do painel.
 * Depois de cada ação o diálogo é reaberto já atualizado.
 */
class DetalhesReserva {

    static void abrir(TelaPrincipal app, PainelController ctrl, Locacao l) {
        JTextArea area = new JTextArea(texto(ctrl, l));
        area.setEditable(false);
        area.setFont(Tema.fonte(Font.PLAIN, 13));
        area.setBackground(TelaPrincipal.BRANCO);
        area.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new Tema.Arredondada(Tema.BORDA_SECAO, 1, Tema.RAIO_CONTROLE, 4, 4));
        sp.setPreferredSize(new Dimension(460, 380));

        // monta os botões: o texto do botão de pagamento vem do controller (null = nada a pagar)
        java.util.List<String> botoes = new java.util.ArrayList<>();
        botoes.add("Fechar");
        botoes.add("Ver cadastro");
        String pagar = PainelController.podeRegistrarPagamento(app.usuario()) ? ctrl.rotuloPagamento(l) : null;   // UC07
        if (pagar != null) botoes.add(pagar);
        if (l.etapa == Etapa.ORCAMENTO && l.temEntrada() && !l.arquivada) { botoes.add("Alterar reserva"); botoes.add("Registrar retirada"); }
        if (l.etapa == Etapa.LOCADO) botoes.add("Registrar devolução");
        botoes.add("Excluir do painel");

        int r = JOptionPane.showOptionDialog(app, sp, "Detalhes da reserva",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, botoes.toArray(), "Fechar");
        if (r < 0) return;
        String escolhido = botoes.get(r);
        if (escolhido.equals("Ver cadastro")) app.abrirCadastro(l.cliente);
        else if (escolhido.equals(pagar)) { app.pagar(l); abrir(app, ctrl, l); }      // reabre com os valores novos
        else if (escolhido.equals("Registrar retirada")) app.retirar(l);
        else if (escolhido.equals("Alterar reserva")) app.alterarReserva(l);
        else if (escolhido.equals("Registrar devolução")) app.abrirDevolucao(l);
        else if (escolhido.equals("Excluir do painel")) app.excluirDoPainel(l);
    }

    private static String texto(PainelController ctrl, Locacao l) {
        StringBuilder sb = new StringBuilder();
        sb.append(l.codigo()).append(" · ").append(l.cliente.nome);
        sb.append("\nEtapa: ").append(l.arquivada ? "Arquivado (" + (l.motivoArquivo.isEmpty() ? "orçamento vencido" : l.motivoArquivo) + ")"
                : l.etapa.nome + (l.ocultaNoPainel ? " (fora do painel)" : ""));
        sb.append("\nSituação: ").append(ctrl.aviso(l));
        for (String p : l.devolucoesParciais) sb.append("\nDevolução parcial: ").append(p);
        sb.append("\n\nEvento: ").append(TelaPrincipal.FMT_COMPLETA.format(l.evento));
        if (l.etapa == Etapa.ORCAMENTO) sb.append("\nOrçamento válido até: ").append(TelaPrincipal.FMT_COMPLETA.format(l.validade)).append(" (30 dias)");
        if (l.etapa != Etapa.ORCAMENTO) {
            sb.append("\nRetirada: ").append(TelaPrincipal.FMT_COMPLETA.format(l.retirada))
              .append(l.retiradoPor.isEmpty() ? "" : " (por " + l.retiradoPor + ")");
            sb.append("\nDevolução prevista: ").append(TelaPrincipal.FMT_COMPLETA.format(l.devolucao));
        }
        if (l.etapa == Etapa.DEVOLVIDO)
            sb.append("\nDevolvido em: ").append(TelaPrincipal.FMT_COMPLETA.format(l.devolvidoEm))
              .append(l.devolvidoPor.isEmpty() ? "" : " (por " + l.devolvidoPor + ")");
        if (l.etapa == Etapa.ORCAMENTO)
            sb.append(l.temEntrada() ? "\nData do evento RESERVADA (entrada paga)"
                    : "\nData do evento NÃO reservada (sem entrada)");

        sb.append("\n\nItens:");
        for (Catalogo.Linha li : l.itens)
            sb.append("\n  ").append(li.qtd).append("× ").append(li.item.nome).append("  —  ").append(TelaPrincipal.moeda(li.subtotal()));
        if (!l.servicos.isEmpty()) {
            sb.append("\n\nServiços:");
            for (ServicoContratado sc : l.servicos)
                sb.append("\n  ").append(sc.servico.nome).append("  —  ").append(TelaPrincipal.moeda(sc.valor));
        }
        if (!l.autorizadoInadimplentePor.isEmpty())
            sb.append("\n\nCliente inadimplente: locação autorizada por ").append(l.autorizadoInadimplentePor);

        if (!l.ocorrencias.isEmpty()) {
            sb.append("\n\nOcorrências:");
            for (Ocorrencia o : l.ocorrencias)
                sb.append("\n  ").append(o.tipo).append(": ").append(o.descricao).append("  —  ").append(TelaPrincipal.moeda(o.valor));
        }
        if (!l.pagamentos.isEmpty()) {
            sb.append("\n\nPagamentos:");
            for (Pagamento p : l.pagamentos)
                sb.append("\n  ").append(TelaPrincipal.FMT_COMPLETA.format(p.data())).append(" · ").append(p.tipo().nome)
                  .append(" · ").append(p.forma()).append("  —  ").append(TelaPrincipal.moeda(p.valor()));
        }
        sb.append("\n\nTotal: ").append(TelaPrincipal.moeda(l.total()));
        sb.append("\nPago: ").append(TelaPrincipal.moeda(l.pago()));
        sb.append("\nSaldo: ").append(TelaPrincipal.moeda(l.saldo()));
        return sb.toString();
    }
}
