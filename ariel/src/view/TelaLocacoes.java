package view;

import controller.PainelController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * VIEW: tela "Locações". Lista todas as locações (todas as etapas) com busca e filtro, e mostra
 * à direita o detalhe da selecionada: datas, valores, itens e pagamentos.
 * Botões: Quitar saldo (ou registrar entrada), Registrar retirada, Devolução, Excluir,
 * contrato e recibo em PDF (UC13), + Serviço (UC06) e Autorizar terceiro (pessoa autorizada, UC03/UC05).
 * Não tem regra de negócio: tudo que decide vem do PainelController.
 */
public class TelaLocacoes extends JPanel {

    private static final String[] FILTROS = {"Todas", "Orçamento", "Locado", "Devolvido", "Com saldo pendente"};

    private final TelaPrincipal app;
    private final PainelController ctrl;
    private final List<Locacao> visiveis = new ArrayList<>();

    private final TelaPrincipal.Campo busca = new TelaPrincipal.Campo("Buscar por cliente, código ou produto", 12);
    private final JComboBox<String> filtro = new JComboBox<>(FILTROS);
    private final ModeloLocacoes modelo = new ModeloLocacoes();
    private final JTable tabela = new JTable(modelo);
    private final JTextArea detalhe = new JTextArea();
    private final JButton btnPagar = TelaPrincipal.botaoCinza("Quitar saldo", 12);
    private final JButton btnRetirar = TelaPrincipal.botaoCinza("Registrar retirada", 12);
    private final JButton btnDevolver = TelaPrincipal.botaoCinza("Devolução", 12);
    private final JButton btnExcluir = TelaPrincipal.botaoNeutro("Excluir", 12);
    private final JButton btnContrato = TelaPrincipal.botaoCinza("Contrato PDF", 12);
    private final JButton btnRecibo = TelaPrincipal.botaoCinza("Recibo PDF", 12);
    private final JButton btnServico = TelaPrincipal.botaoCinza("+ Serviço", 12);
    private final JButton btnTerceiro = TelaPrincipal.botaoCinza("Autorizar terceiro", 12);
    private boolean atualizando = false;

    public TelaLocacoes(TelaPrincipal app, PainelController ctrl) {
        super(new BorderLayout());
        this.app = app;
        this.ctrl = ctrl;
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // ----- cabeçalho: título, busca e filtro -----
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Locações", Font.BOLD, 30), BorderLayout.WEST);
        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        dir.setOpaque(false);
        busca.setPreferredSize(new Dimension(260, Tema.ALTURA));
        busca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { atualizar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { atualizar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { atualizar(); }
        });
        dir.add(busca);
        TelaPrincipal.estilizarCombo(filtro);
        filtro.setPreferredSize(new Dimension(170, Tema.ALTURA));
        filtro.addActionListener(e -> atualizar());
        dir.add(filtro);
        cab.add(dir, BorderLayout.EAST);
        add(cab, BorderLayout.NORTH);

        // ----- corpo: lista (esquerda) + detalhe (direita) -----
        TelaClientes.estilizarTabela(tabela, 12, 6);
        int[] larguras = {62, 140, 92, 100, 104, 180};     // Código, Cliente, Etapa, Evento, Total, Situação
        for (int i = 0; i < larguras.length; i++) tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        tabela.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) mostrarDetalhe(); });
        JScrollPane rolagem = Ui.rolagem(tabela);

        detalhe.setEditable(false);
        detalhe.setLineWrap(true);           // quebra o texto na largura da caixa
        detalhe.setWrapStyleWord(true);
        detalhe.setFont(Tema.fonte(Font.PLAIN, 13));
        detalhe.setForeground(Tema.TEXTO);
        detalhe.setBorder(new EmptyBorder(0, 0, 0, 0));
        JScrollPane rolDet = Ui.rolagem(detalhe);

        JPanel botoes = new JPanel(new GridLayout(4, 2, 10, 10));
        botoes.setOpaque(false);
        botoes.setBorder(new EmptyBorder(12, 0, 0, 0));
        botoes.add(btnPagar);
        botoes.add(btnRetirar);
        botoes.add(btnDevolver);
        botoes.add(btnExcluir);
        botoes.add(btnContrato);
        botoes.add(btnRecibo);
        botoes.add(btnServico);
        botoes.add(btnTerceiro);
        btnPagar.addActionListener(e -> { Locacao l = selecionada(); if (l != null) app.pagar(l); });
        btnRetirar.addActionListener(e -> { Locacao l = selecionada(); if (l != null) app.retirar(l); });
        btnDevolver.addActionListener(e -> { Locacao l = selecionada(); if (l != null) app.abrirDevolucao(l); });
        btnExcluir.addActionListener(e -> { Locacao l = selecionada(); if (l != null) app.excluirDoPainel(l); });
        btnContrato.addActionListener(e -> { Locacao l = selecionada(); if (l != null) app.documento(l, false); });
        btnRecibo.addActionListener(e -> { Locacao l = selecionada(); if (l != null) app.documento(l, true); });
        btnServico.addActionListener(e -> { Locacao l = selecionada(); if (l != null) app.adicionarServico(l); });
        btnTerceiro.addActionListener(e -> { Locacao l = selecionada(); if (l != null) app.autorizarTerceiro(l); });

        JPanel miolo = new JPanel(new BorderLayout());
        miolo.setOpaque(false);
        miolo.add(rolDet, BorderLayout.CENTER);
        miolo.add(botoes, BorderLayout.SOUTH);
        JPanel lateral = Ui.caixa("DETALHE DA LOCAÇÃO", miolo);
        lateral.setPreferredSize(new Dimension(340, 100));

        JPanel corpo = new JPanel(new BorderLayout(20, 0));
        corpo.setOpaque(false);
        corpo.add(Ui.secao(rolagem), BorderLayout.CENTER);
        corpo.add(lateral, BorderLayout.EAST);
        add(corpo, BorderLayout.CENTER);

        atualizar();
    }

    private Locacao selecionada() {
        int i = tabela.getSelectedRow();
        return i >= 0 && i < visiveis.size() ? visiveis.get(i) : null;
    }

    /** Recarrega a lista (mantém a seleção quando possível). */
    void atualizar() {
        if (atualizando) return;
        atualizando = true;
        try {
            Locacao antes = selecionada();
            String q = busca.getText().trim().toLowerCase();
            String f = (String) filtro.getSelectedItem();
            visiveis.clear();
            for (Locacao l : ctrl.locacoes) {
                if (l.arquivada) continue;
                if (f.equals("Orçamento") && l.etapa != Etapa.ORCAMENTO) continue;
                if (f.equals("Locado") && l.etapa != Etapa.LOCADO) continue;
                if (f.equals("Devolvido") && l.etapa != Etapa.DEVOLVIDO) continue;
                if (f.equals("Com saldo pendente") && !(l.temSaldo() && (l.etapa != Etapa.ORCAMENTO || l.temEntrada()))) continue;
                if (!q.isEmpty()) {
                    StringBuilder it = new StringBuilder();
                    for (Catalogo.Linha li : l.itens) it.append(' ').append(li.item.nome);
                    if (!(l.codigo() + " " + l.cliente.nome + it).toLowerCase().contains(q)) continue;
                }
                visiveis.add(l);
            }
            visiveis.sort((a, b) -> a.evento.compareTo(b.evento));
            modelo.fireTableDataChanged();
            int i = visiveis.indexOf(antes);
            if (i >= 0) tabela.setRowSelectionInterval(i, i); else tabela.clearSelection();
        } finally {
            atualizando = false;
        }
        mostrarDetalhe();
    }

    private void mostrarDetalhe() {
        Locacao l = selecionada();
        boolean tem = l != null;
        boolean admin = PainelController.podeRegistrarPagamento(app.usuario());   // UC07/UC13: só o Administrador
        btnPagar.setEnabled(admin && tem && ctrl.rotuloPagamento(l) != null);
        btnPagar.setText(tem && ctrl.rotuloPagamento(l) != null ? ctrl.rotuloPagamento(l) : "Sem pagamento");
        btnRetirar.setEnabled(tem && l.etapa == Etapa.ORCAMENTO);
        btnDevolver.setEnabled(tem && l.etapa == Etapa.LOCADO);
        btnExcluir.setEnabled(tem);
        boolean confirmada = tem && !l.arquivada && (l.etapa != Etapa.ORCAMENTO || l.temEntrada());
        btnContrato.setEnabled(admin && confirmada);
        btnRecibo.setEnabled(admin && tem && !l.pagamentos.isEmpty());
        btnServico.setEnabled(tem && !l.arquivada && l.etapa != Etapa.DEVOLVIDO);
        btnTerceiro.setEnabled(tem && !l.arquivada && l.etapa == Etapa.ORCAMENTO && l.temEntrada());
        if (!tem) { detalhe.setText("Selecione uma locação na lista."); return; }

        StringBuilder sb = new StringBuilder();
        sb.append(l.codigo()).append(" · ").append(l.cliente.nome);
        sb.append("\nEtapa: ").append(l.etapa.nome).append("  ·  ").append(ctrl.aviso(l));
        sb.append("\n\nRetirada: ").append(l.retirada == null ? "-" : TelaPrincipal.FMT_COMPLETA.format(l.retirada));
        sb.append("\nEvento: ").append(TelaPrincipal.FMT_COMPLETA.format(l.evento));
        sb.append("\nDevolução prevista: ").append(l.devolucao == null ? "-" : TelaPrincipal.FMT_COMPLETA.format(l.devolucao));
        if (l.devolvidoEm != null) sb.append("\nDevolvido em: ").append(TelaPrincipal.FMT_COMPLETA.format(l.devolvidoEm));
        if (!l.retiradoPor.isEmpty()) sb.append("\nRetirado por: ").append(l.retiradoPor);
        sb.append("\nModalidade: ").append(l.modalidade);
        if (!l.autorizadoInadimplentePor.isEmpty()) sb.append("\nCliente inadimplente: locação autorizada por ").append(l.autorizadoInadimplentePor);
        sb.append("\n\nItens:");
        for (Catalogo.Linha li : l.itens)
            sb.append("\n  ").append(li.qtd).append("× ").append(li.item.nome)
              .append(li.desconto > 0.005 ? " (-" + String.format(TelaPrincipal.PT_BR, "%.1f", li.desconto) + "%)" : "")
              .append(" — ").append(TelaPrincipal.moeda(li.subtotal()));
        if (!l.servicos.isEmpty()) {
            sb.append("\n\nServiços:");
            for (ServicoContratado sc : l.servicos)
                sb.append("\n  ").append(sc.servico.nome).append(" — ").append(TelaPrincipal.moeda(sc.valor));
        }
        if (!l.autorizados.isEmpty()) {
            sb.append("\n\nAutorizados a retirar:");
            for (Cliente.Autorizada a : l.autorizados) sb.append("\n  ").append(a.nome).append(" (").append(a.documento).append(")");
        }
        if (!l.documentos.isEmpty() || l.contratoPendente) {
            sb.append("\n\nDocumentos:");
            for (Documento d : l.documentos)
                sb.append("\n  ").append(d.tipo()).append(" v").append(d.versao()).append(" · ").append(TelaPrincipal.FMT_COMPLETA.format(d.data()));
            if (l.contratoPendente) sb.append("\n  PENDENTE: falha ao gerar o contrato (use Contrato PDF para tentar de novo)");
        }
        if (!l.ocorrencias.isEmpty()) {
            sb.append("\n\nOcorrências:");
            for (Ocorrencia o : l.ocorrencias)
                sb.append("\n  ").append(o.tipo).append(": ").append(o.descricao).append(" — ").append(TelaPrincipal.moeda(o.valor));
        }
        sb.append("\n\nPagamentos:");
        if (l.pagamentos.isEmpty()) sb.append("\n  (nenhum)");
        for (Pagamento p : l.pagamentos)
            sb.append("\n  ").append(TelaPrincipal.FMT_COMPLETA.format(p.data())).append(" · ").append(p.tipo().nome)
              .append(" · ").append(p.forma()).append(" — ").append(TelaPrincipal.moeda(p.valor()));
        sb.append("\n\nTotal: ").append(TelaPrincipal.moeda(l.total()));
        sb.append("\nPago: ").append(TelaPrincipal.moeda(l.pago()));
        sb.append("\nSaldo: ").append(TelaPrincipal.moeda(l.saldo()));
        detalhe.setText(sb.toString());
        detalhe.setCaretPosition(0);
    }

    /** Modelo da tabela (a JTable pergunta aqui o que mostrar em cada célula). */
    class ModeloLocacoes extends AbstractTableModel {
        private final String[] colunas = {"Código", "Cliente", "Etapa", "Evento", "Total", "Situação"};
        public int getRowCount() { return visiveis.size(); }
        public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int c) { return colunas[c]; }
        public Object getValueAt(int i, int c) {
            Locacao l = visiveis.get(i);
            switch (c) {
                case 0:  return l.codigo();
                case 1:  return l.cliente.nome;
                case 2:  return l.etapa.nome;
                case 3:  return TelaPrincipal.FMT_COMPLETA.format(l.evento);
                case 4:  return TelaPrincipal.moeda(l.total());
                default: return ctrl.aviso(l);
            }
        }
    }
}
