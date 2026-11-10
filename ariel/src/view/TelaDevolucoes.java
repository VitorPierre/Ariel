package view;

import controller.PainelController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

/**
 * VIEW: tela "Devolução e ocorrências".
 *  1) escolhe a locação em andamento (Locado) ou uma devolvida com pendências;
 *  2) confere cada PRODUTO (pacotes são desmembrados): OK, danificado ou perdido, quantas unidades
 *     foram afetadas e o valor a cobrar (digitado pela proprietária);
 *  3) o atraso é só informado (dias); a multa, se houver, é DIGITADA: o sistema não calcula nem sugere (RN05/RN06);
 *  4) qualquer pessoa identificada pode devolver (basta o nome);
 *  5) "Registrar devolução" -> o PainelController valida as regras e cria as ocorrências;
 *  6) depois, "Quitar pendências" registra o pagamento de multa/indenização.
 * A locação só fica finalizada com tudo devolvido, conferido e pago.
 */
public class TelaDevolucoes extends JPanel {

    private final TelaPrincipal app;
    private final PainelController ctrl;
    private final Usuario usuario;

    private final JComboBox<Locacao> cbLocacao = new JComboBox<>();
    private final TelaPrincipal.Campo tfQuem = new TelaPrincipal.Campo("Nome de quem está devolvendo", 13);
    private final ModeloConferencia modelo = new ModeloConferencia();
    private final JTable tabela = new JTable(modelo);
    private final TelaPrincipal.Campo tfMulta = new TelaPrincipal.Campo("Multa de atraso (valor digitado; vazio = sem cobrança)", 13);
    private final JLabel lblAtraso = TelaPrincipal.rotulo(" ", Font.PLAIN, 12);
    private final JTextArea resumo = new JTextArea();
    private final JButton btnDevolver = TelaPrincipal.botaoCinza("Registrar devolução", 13);
    private final JButton btnQuitar = TelaPrincipal.botaoCinza("Quitar pendências", 13);
    private final JLabel lblEstado = TelaPrincipal.rotulo(" ", Font.BOLD, 12);

    private Devolucao dados;          // conferência em edição (só para locações em andamento)
    private Locacao atual;
    private boolean carregando = false;

    public TelaDevolucoes(TelaPrincipal app, PainelController ctrl, Usuario usuario) {
        super(new BorderLayout());
        this.app = app;
        this.ctrl = ctrl;
        this.usuario = usuario;
        setBackground(TelaPrincipal.BRANCO);
        setBorder(new EmptyBorder(14, 16, 16, 16));

        // ----- cabeçalho: título + escolha da locação -----
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Devolução e ocorrências", Font.BOLD, 20), BorderLayout.WEST);
        TelaPrincipal.estilizarCombo(cbLocacao);
        cbLocacao.setPreferredSize(new Dimension(340, 36));
        cbLocacao.addActionListener(e -> { if (!carregando) carregar((Locacao) cbLocacao.getSelectedItem()); });
        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        dir.setOpaque(false);
        dir.add(cbLocacao);
        cab.add(dir, BorderLayout.EAST);
        add(cab, BorderLayout.NORTH);

        // ----- esquerda: conferência dos itens + atraso -----
        TelaClientes.estilizarTabela(tabela, 12, 6);
        int[] larg = {150, 40, 105, 70, 90, 55, 170};
        for (int i = 0; i < larg.length; i++) tabela.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
        JComboBox<Devolucao.Situacao> edSituacao = new JComboBox<>(Devolucao.Situacao.values());
        TelaPrincipal.estilizarCombo(edSituacao);
        tabela.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(edSituacao));
        tabela.setSurrendersFocusOnKeystroke(true);
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(new LineBorder(TelaPrincipal.CINZA_BORDA, 2));
        rolagem.getViewport().setBackground(TelaPrincipal.BRANCO);

        tfMulta.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { recalcular(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { recalcular(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalcular(); }
        });

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(10, 0, 0, 0));
        form.add(TelaPrincipal.rotulo("OCORRÊNCIA: selecione o item na tabela e use os botões (ou a coluna Situação)", Font.BOLD, 11));
        JButton bDano = TelaPrincipal.botaoCinza("Registrar dano", 12), bPerda = TelaPrincipal.botaoCinza("Registrar perda", 12),
                bLimpa = TelaPrincipal.botaoCinza("Item OK", 12);
        bDano.addActionListener(e -> marcarItem(Devolucao.Situacao.DANIFICADO));
        bPerda.addActionListener(e -> marcarItem(Devolucao.Situacao.PERDIDO));
        bLimpa.addActionListener(e -> marcarItem(Devolucao.Situacao.OK));
        JPanel botOc = new JPanel(new GridLayout(1, 3, 8, 0));
        botOc.setOpaque(false);
        botOc.add(bDano);
        botOc.add(bPerda);
        botOc.add(bLimpa);
        form.add(botOc);
        form.add(lblAtraso);
        form.add(tfMulta);
        form.add(TelaPrincipal.rotulo("Quem está devolvendo os itens? (qualquer pessoa identificada)", Font.PLAIN, 12));
        form.add(tfQuem);

        JPanel esq = new JPanel(new BorderLayout());
        esq.setOpaque(false);
        esq.add(TelaPrincipal.rotulo("CONFERÊNCIA DOS ITENS", Font.BOLD, 12), BorderLayout.NORTH);
        JPanel esqMiolo = new JPanel(new BorderLayout());
        esqMiolo.setOpaque(false);
        esqMiolo.setBorder(new EmptyBorder(6, 0, 0, 0));
        esqMiolo.add(rolagem, BorderLayout.CENTER);
        esqMiolo.add(form, BorderLayout.SOUTH);
        esq.add(esqMiolo, BorderLayout.CENTER);

        // ----- direita: resumo, pendências e botões -----
        resumo.setEditable(false);
        resumo.setLineWrap(true);           // quebra o texto na largura da caixa
        resumo.setWrapStyleWord(true);
        resumo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resumo.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane rolRes = new JScrollPane(resumo);
        rolRes.setBorder(new LineBorder(TelaPrincipal.CINZA_BORDA, 2));
        btnDevolver.addActionListener(e -> registrar());
        btnQuitar.addActionListener(e -> { if (atual != null) app.pagar(atual); });
        JPanel botoes = new JPanel(new GridLayout(0, 1, 0, 8));
        botoes.setOpaque(false);
        botoes.setBorder(new EmptyBorder(10, 0, 0, 0));
        botoes.add(lblEstado);
        botoes.add(btnDevolver);
        botoes.add(btnQuitar);

        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(360, 100));
        lateral.add(TelaPrincipal.rotulo("OCORRÊNCIAS E PENDÊNCIAS", Font.BOLD, 12), BorderLayout.NORTH);
        JPanel latMiolo = new JPanel(new BorderLayout());
        latMiolo.setOpaque(false);
        latMiolo.setBorder(new EmptyBorder(6, 0, 0, 0));
        latMiolo.add(rolRes, BorderLayout.CENTER);
        latMiolo.add(botoes, BorderLayout.SOUTH);
        lateral.add(latMiolo, BorderLayout.CENTER);

        JPanel corpo = new JPanel(new BorderLayout(16, 0));
        corpo.setOpaque(false);
        corpo.add(esq, BorderLayout.CENTER);
        corpo.add(lateral, BorderLayout.EAST);
        add(corpo, BorderLayout.CENTER);

        atualizar();
    }

    /** Escolhe uma locação (usado pelo painel ao soltar um cartão em "Devolvido"). */
    void selecionar(Locacao l) {
        atualizar();
        carregando = true;
        cbLocacao.setSelectedItem(l);
        carregando = false;
        carregar(l);
    }

    /** Recarrega a lista de locações que aparecem aqui: em andamento + devolvidas com pendências. */
    void atualizar() {
        if (carregando) return;
        carregando = true;
        Locacao antes = (Locacao) cbLocacao.getSelectedItem();
        cbLocacao.removeAllItems();
        for (Locacao l : ctrl.locacoes) {
            if (l.arquivada) continue;
            if (l.etapa == Etapa.LOCADO || (l.etapa == Etapa.DEVOLVIDO && l.temSaldo())) cbLocacao.addItem(l);
        }
        if (antes != null && ctrl.locacoes.contains(antes)) cbLocacao.setSelectedItem(antes);
        carregando = false;
        Locacao nova = (Locacao) cbLocacao.getSelectedItem();
        // se a mesma locação continua selecionada e a conferência está em edição, não perde o que foi digitado
        if (nova != atual || dados == null || atual.etapa != Etapa.LOCADO) carregar(nova);
        else mostrarResumo();
    }

    /** Prepara a tela para a locação escolhida (ou deixa vazia se não houver nenhuma). */
    private void carregar(Locacao l) {
        pararEdicao();
        atual = l;
        dados = (l != null && l.etapa == Etapa.LOCADO) ? new Devolucao(l) : null;
        modelo.fireTableDataChanged();

        boolean emAndamento = dados != null;
        tabela.setEnabled(emAndamento);
        tfMulta.setEnabled(emAndamento);
        tfQuem.setEnabled(emAndamento);
        tfQuem.setText(emAndamento ? l.cliente.nome : "");
        tfMulta.setText("");
        btnDevolver.setVisible(emAndamento);
        btnQuitar.setVisible(!emAndamento);
        mostrarResumo();
    }

    private void pararEdicao() {
        if (tabela.isEditing() && tabela.getCellEditor() != null) tabela.getCellEditor().stopCellEditing();
    }

    private void recalcular() { if (!carregando) mostrarResumo(); }

    /** Escreve o resumo (prévia da cobrança antes da devolução, ou o histórico depois). */
    private void mostrarResumo() {
        StringBuilder sb = new StringBuilder();
        if (atual == null) {
            lblAtraso.setText("Nenhuma locação aguardando devolução.");
            lblEstado.setText(" ");
            resumo.setText("Não há locações em andamento nem devoluções com pendência.");
            return;
        }
        Locacao l = atual;
        if (dados != null) {                                   // ----- conferência em andamento -----
            long atraso = ctrl.diasAtraso(l);
            double multa = Math.max(0, TelaPrincipal.lerValor(tfMulta.getText()));
            lblAtraso.setText("Devolução prevista " + TelaPrincipal.FMT_COMPLETA.format(l.devolucao)
                    + "  ·  hoje " + TelaPrincipal.FMT_COMPLETA.format(ctrl.hoje())
                    + "  ·  " + (atraso == 0 ? "no prazo" : atraso + (atraso == 1 ? " dia" : " dias") + " de atraso"));
            tfMulta.setVisible(atraso > 0);
            double previa = atraso > 0 ? multa : 0;
            sb.append(l.codigo()).append(" · ").append(l.cliente.nome).append("\n\nPRÉVIA DA COBRANÇA (valores digitados)\n");
            if (atraso > 0) sb.append("Atraso de ").append(atraso).append(atraso == 1 ? " dia" : " dias").append(": ")
                    .append(multa > 0.005 ? TelaPrincipal.moeda(multa) : "sem cobrança").append('\n');
            for (Devolucao.ItemConferido c : dados.itens) {
                if (c.situacao == Devolucao.Situacao.OK) continue;
                sb.append(c.situacao).append(": ").append(c.qtdAfetada).append("× ").append(c.produto.nome)
                  .append(" = ").append(TelaPrincipal.moeda(c.valorCobrado)).append('\n');
                previa += c.valorCobrado;
            }
            sb.append("\nTotal a cobrar na devolução: ").append(TelaPrincipal.moeda(previa));
            sb.append("\nSaldo dos itens: ").append(TelaPrincipal.moeda(l.saldoItens()));
            sb.append("\n\nA devolução não exige pagamento; o que ficar pendente continua registrado.");
            lblEstado.setText("Em andamento · aguardando conferência");
        } else {                                               // ----- locação já devolvida -----
            lblAtraso.setText("Devolvida em " + TelaPrincipal.FMT_COMPLETA.format(l.devolvidoEm)
                    + (l.devolvidoPor.isEmpty() ? "" : " por " + l.devolvidoPor));
            tfMulta.setVisible(false);
            sb.append(l.codigo()).append(" · ").append(l.cliente.nome).append("\n\nOCORRÊNCIAS\n");
            if (l.ocorrencias.isEmpty()) sb.append("(nenhuma)\n");
            for (Ocorrencia o : l.ocorrencias)
                sb.append(o.tipo).append(": ").append(o.descricao).append(" = ").append(TelaPrincipal.moeda(o.valor))
                  .append("  (").append(TelaPrincipal.FMT_COMPLETA.format(o.data)).append(", ").append(o.registradoPor).append(")\n");
            sb.append("\nMulta/indenização: ").append(TelaPrincipal.moeda(l.totalOcorrencias()));
            sb.append("\nPago: ").append(TelaPrincipal.moeda(l.pagoCobrancas()));
            sb.append("\nPendente: ").append(TelaPrincipal.moeda(l.saldoCobrancas()));
            lblEstado.setText(l.finalizada() ? "Finalizada" : "Devolvida · com pendências");
            btnQuitar.setEnabled(ctrl.rotuloPagamento(l) != null);
        }
        resumo.setText(sb.toString());
        resumo.setCaretPosition(0);
    }

    /** Registra a devolução: monta o Devolucao com o que está na tela e entrega ao controller. */
    private void registrar() {
        if (atual == null || dados == null) return;
        pararEdicao();
        dados.devolvidoPor = tfQuem.getText().trim();
        dados.multa = Math.max(0, TelaPrincipal.lerValor(tfMulta.getText()));
        try {
            // pergunta antes se houver cobrança (a devolução não pode ser desfeita)
            int r = JOptionPane.showOptionDialog(this,
                    "Registrar a devolução de " + atual.codigo() + " · " + atual.cliente.nome + "?\n\n"
                            + resumo.getText().split("Saldo dos itens")[0].replaceFirst("(?s)^.*?PRÉVIA DA COBRANÇA[^\n]*\n", ""),
                    "Confirmar devolução", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                    new Object[]{"Registrar", "Cancelar"}, "Cancelar");
            if (r != 0) return;
            Locacao l = atual;
            ctrl.registrarDevolucao(l, dados, usuario);     // o controller valida as regras
            selecionar(l);                                  // mostra o resultado
        } catch (RegraNegocioException e) {
            app.aviso(e.getMessage());
        }
    }

    /** Aplica a situação ao item selecionado (mesmo efeito de escolher na coluna Situação) e leva o foco à descrição. */
    private void marcarItem(Devolucao.Situacao s) {
        int i = tabela.getSelectedRow();
        if (dados == null || i < 0) { app.aviso("Selecione na tabela o item da ocorrência."); return; }
        if (tabela.isEditing()) tabela.getCellEditor().stopCellEditing();
        tabela.getModel().setValueAt(s, i, 2);
        if (s != Devolucao.Situacao.OK) tabela.editCellAt(i, 6);
    }

    /** Tabela da conferência: produto, qtd, situação, unidades afetadas, valor cobrado e se fica indisponível (dano). */
    class ModeloConferencia extends AbstractTableModel {
        private final String[] colunas = {"Produto", "Qtd", "Situação", "Afetadas", "Valor cobrado", "Indisp.?", "Descrição da ocorrência"};
        public int getRowCount() { return dados == null ? 0 : dados.itens.size(); }
        public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int c) { return colunas[c]; }
        @Override public Class<?> getColumnClass(int c) { return c == 5 ? Boolean.class : Object.class; }
        @Override public boolean isCellEditable(int r, int c) {
            if (dados == null || c < 2) return false;
            Devolucao.ItemConferido it = dados.itens.get(r);
            return c == 2 || it.situacao != Devolucao.Situacao.OK && (c != 5 || it.situacao == Devolucao.Situacao.DANIFICADO);   // 6 = descrição
        }

        public Object getValueAt(int i, int c) {
            Devolucao.ItemConferido it = dados.itens.get(i);
            switch (c) {
                case 0:  return it.produto.nome;
                case 1:  return it.qtd;
                case 2:  return it.situacao;
                case 3:  return it.situacao == Devolucao.Situacao.OK ? "" : String.valueOf(it.qtdAfetada);
                case 4:  return it.valorCobrado == 0 ? "" : String.format(TelaPrincipal.PT_BR, "%.2f", it.valorCobrado).replace('.', ',');
                case 5:  return it.situacao == Devolucao.Situacao.DANIFICADO && it.indisponibilizar;
                default: return it.situacao == Devolucao.Situacao.OK ? "" : it.observacao;
            }
        }

        @Override public void setValueAt(Object v, int i, int c) {
            Devolucao.ItemConferido it = dados.itens.get(i);
            if (c == 2) {
                it.situacao = (Devolucao.Situacao) v;
                if (it.situacao == Devolucao.Situacao.OK) { it.valorCobrado = 0; it.qtdAfetada = 0; it.indisponibilizar = false; it.observacao = ""; }
                else { if (it.qtdAfetada < 1) it.qtdAfetada = 1; it.indisponibilizar = it.situacao == Devolucao.Situacao.DANIFICADO; }   // começa com 1 unidade (ex.: 1 cadeira de 30)
            } else if (c == 3) {
                try { it.qtdAfetada = Math.max(0, Math.min(it.qtd, Integer.parseInt(String.valueOf(v).trim()))); }
                catch (NumberFormatException e) { /* mantém o valor anterior */ }
            } else if (c == 4) {
                it.valorCobrado = Math.max(0, TelaPrincipal.lerValor(String.valueOf(v)));
            } else if (c == 5) {
                it.indisponibilizar = Boolean.TRUE.equals(v);
            } else if (c == 6) {
                it.observacao = String.valueOf(v).trim();
            }
            fireTableRowsUpdated(i, i);
            mostrarResumo();
        }
    }
}
