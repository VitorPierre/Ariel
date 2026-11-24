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
 *  3) o atraso é só informado (dias); a multa, se houver, é DIGITADA: o sistema não calcula nem sugere (decisão da equipe);
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
    private final TelaPrincipal.Campo tfQuem = new TelaPrincipal.Campo("Nome da pessoa", 13);
    private final ModeloConferencia modelo = new ModeloConferencia();
    private final JTable tabela = new JTable(modelo);
    private final TelaPrincipal.Campo tfMulta = new TelaPrincipal.Campo("0,00", 13);
    private JPanel blocoMulta;          // rótulo + campo da multa (só aparece quando há atraso)
    private final JLabel lblAtraso = TelaPrincipal.rotulo(" ", Font.PLAIN, 12);
    private final JTextArea resumo = new JTextArea();
    private final JButton btnDevolver = TelaPrincipal.botaoPrimario("Registrar devolução", 13);
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
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // ----- cabeçalho: título + escolha da locação -----
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Devolução e ocorrências", Font.BOLD, 30), BorderLayout.WEST);
        TelaPrincipal.estilizarCombo(cbLocacao);
        cbLocacao.setPreferredSize(new Dimension(340, Tema.ALTURA));
        cbLocacao.addActionListener(e -> { if (!carregando) carregar((Locacao) cbLocacao.getSelectedItem()); });
        JPanel dir = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        dir.setOpaque(false);
        dir.add(cbLocacao);
        cab.add(dir, BorderLayout.EAST);
        add(cab, BorderLayout.NORTH);

        // ----- esquerda: conferência dos itens + atraso -----
        TelaClientes.estilizarTabela(tabela, 12, 6);
        int[] larg = {150, 52, 130, 84, 96, 76, 140};
        for (int i = 0; i < larg.length; i++) tabela.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
        JComboBox<Devolucao.Situacao> edSituacao = new JComboBox<>(Devolucao.Situacao.values());
        TelaPrincipal.estilizarCombo(edSituacao);
        tabela.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(edSituacao));
        tabela.setSurrendersFocusOnKeystroke(true);
        JScrollPane rolagem = Ui.rolagem(tabela);

        tfMulta.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { recalcular(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { recalcular(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalcular(); }
        });

        JButton bDano = TelaPrincipal.botaoSuave("Registrar dano", 13), bPerda = TelaPrincipal.botaoSuave("Registrar perda", 13),
                bFalta = TelaPrincipal.botaoSuave("Ainda não voltou", 13), bLimpa = TelaPrincipal.botaoNeutro("Item OK", 13);
        bDano.addActionListener(e -> marcarItem(Devolucao.Situacao.DANIFICADO));
        bPerda.addActionListener(e -> marcarItem(Devolucao.Situacao.PERDIDO));
        bFalta.addActionListener(e -> marcarItem(Devolucao.Situacao.NAO_DEVOLVIDO));   // devolução parcial (UC16 FA02)
        bLimpa.addActionListener(e -> marcarItem(Devolucao.Situacao.OK));
        JPanel botOc = new JPanel(new Ui.WrapLayout(FlowLayout.LEFT, 8, 6));
        botOc.setOpaque(false);
        botOc.setBorder(new EmptyBorder(0, -8, 0, 0));
        botOc.add(bDano);
        botOc.add(bPerda);
        botOc.add(bFalta);
        botOc.add(bLimpa);
        JLabel dica = TelaPrincipal.rotulo("Selecione o item na tabela e use os botões (ou a coluna Situação):", Font.PLAIN, 12);
        dica.setForeground(Tema.TEXTO_SUAVE);
        lblAtraso.setForeground(Tema.TEXTO_MEDIO);
        JPanel linhaCampos = new JPanel(new GridLayout(1, 2, 12, 0));
        linhaCampos.setOpaque(false);
        blocoMulta = Ui.bloco("Multa de atraso (R$) — vazio = sem cobrança", tfMulta);
        linhaCampos.add(blocoMulta);
        linhaCampos.add(Ui.bloco("Quem está devolvendo os itens?", tfQuem));
        JPanel form = Ui.pilha(8, dica, botOc, lblAtraso, linhaCampos);
        botOc.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        form.setBorder(new EmptyBorder(12, 0, 0, 0));

        JPanel esqMiolo = new JPanel(new BorderLayout());
        esqMiolo.setOpaque(false);
        esqMiolo.add(rolagem, BorderLayout.CENTER);
        esqMiolo.add(form, BorderLayout.SOUTH);
        JPanel esq = Ui.caixa("CONFERÊNCIA DOS ITENS", esqMiolo);
        esq.setMinimumSize(new Dimension(340, 200));

        // ----- direita: resumo, pendências e botões -----
        resumo.setEditable(false);
        resumo.setLineWrap(true);           // quebra o texto na largura da caixa
        resumo.setWrapStyleWord(true);
        resumo.setFont(Tema.fonte(Font.PLAIN, 13));
        resumo.setForeground(Tema.TEXTO);
        resumo.setBorder(new EmptyBorder(0, 0, 0, 0));
        JScrollPane rolRes = Ui.rolagem(resumo);
        btnDevolver.addActionListener(e -> registrar());
        btnQuitar.addActionListener(e -> { if (atual != null) app.pagar(atual); });
        lblEstado.setForeground(Tema.PRIMARIA_ESCURA);
        JPanel botoes = Ui.pilha(10, lblEstado, btnDevolver, btnQuitar);
        botoes.setBorder(new EmptyBorder(12, 0, 0, 0));

        JPanel latMiolo = new JPanel(new BorderLayout());
        latMiolo.setOpaque(false);
        latMiolo.add(rolRes, BorderLayout.CENTER);
        latMiolo.add(botoes, BorderLayout.SOUTH);
        JPanel lateral = Ui.caixa("OCORRÊNCIAS E PENDÊNCIAS", latMiolo);
        lateral.setPreferredSize(new Dimension(350, 100));
        lateral.setMinimumSize(new Dimension(300, 200));

        add(Ui.divisor(esq, lateral, 0.60), BorderLayout.CENTER);

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
            blocoMulta.setVisible(atraso > 0);
            double previa = atraso > 0 ? multa : 0;
            sb.append(l.codigo()).append(" · ").append(l.cliente.nome).append("\n\nPRÉVIA DA COBRANÇA (valores digitados)\n");
            if (atraso > 0) sb.append("Atraso de ").append(atraso).append(atraso == 1 ? " dia" : " dias").append(": ")
                    .append(multa > 0.005 ? TelaPrincipal.moeda(multa) : "sem cobrança").append('\n');
            StringBuilder faltam = new StringBuilder();
            for (Devolucao.ItemConferido c : dados.itens) {
                if (c.situacao == Devolucao.Situacao.OK) continue;
                if (c.situacao == Devolucao.Situacao.NAO_DEVOLVIDO) {
                    faltam.append("  ").append(c.qtdAfetada).append("× ").append(c.produto.nome).append('\n');
                    continue;
                }
                sb.append(c.situacao).append(": ").append(c.qtdAfetada).append("× ").append(c.produto.nome)
                  .append(" = ").append(TelaPrincipal.moeda(c.valorCobrado)).append('\n');
                previa += c.valorCobrado;
            }
            sb.append("\nTotal a cobrar na devolução: ").append(TelaPrincipal.moeda(previa));
            if (faltam.length() > 0)
                sb.append("\n\nDEVOLUÇÃO PARCIAL — continuam pendentes (não é perda):\n").append(faltam);
            if (!l.devolucoesParciais.isEmpty()) {
                sb.append("\n\nDevoluções parciais anteriores:\n");
                for (String x : l.devolucoesParciais) sb.append("  ").append(x).append('\n');
            }
            sb.append("\nSaldo dos itens: ").append(TelaPrincipal.moeda(l.saldoItens()));
            sb.append("\n\nA devolução não exige pagamento; o que ficar pendente continua registrado.");
            lblEstado.setText(l.devolucoesParciais.isEmpty() ? "Em andamento · aguardando conferência"
                    : "Devolução parcial · conferindo o que falta");
        } else {                                               // ----- locação já devolvida -----
            lblAtraso.setText("Devolvida em " + TelaPrincipal.FMT_COMPLETA.format(l.devolvidoEm)
                    + (l.devolvidoPor.isEmpty() ? "" : " por " + l.devolvidoPor));
            blocoMulta.setVisible(false);
            sb.append(l.codigo()).append(" · ").append(l.cliente.nome).append("\n\nOCORRÊNCIAS\n");
            if (l.ocorrencias.isEmpty()) sb.append("(nenhuma)\n");
            for (Ocorrencia o : l.ocorrencias)
                sb.append(o.tipo).append(": ").append(o.descricao).append(" = ").append(TelaPrincipal.moeda(o.valor))
                  .append("  (").append(TelaPrincipal.FMT_COMPLETA.format(o.data)).append(", ").append(o.registradoPor).append(")\n");
            sb.append("\nMulta/indenização: ").append(TelaPrincipal.moeda(l.totalOcorrencias()));
            sb.append("\nPago: ").append(TelaPrincipal.moeda(l.pagoCobrancas()));
            sb.append("\nPendente: ").append(TelaPrincipal.moeda(l.saldoCobrancas()));
            lblEstado.setText(l.finalizada() ? "Finalizada" : "Devolvida · com pendências");
            btnQuitar.setEnabled(PainelController.podeRegistrarPagamento(app.usuario()) && ctrl.rotuloPagamento(l) != null);   // UC07
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
            // prévia: reservas confirmadas que ficariam sem estoque por causa de perda/dano desta devolução
            java.util.List<PainelController.ReservaAfetada> afetadas = ctrl.reservasAfetadas(atual, dados);
            StringBuilder impacto = new StringBuilder();
            if (!afetadas.isEmpty()) {
                impacto.append("\n\nATENÇÃO: com isso, estas reservas confirmadas ficam SEM ESTOQUE:\n");
                for (PainelController.ReservaAfetada a : afetadas) impacto.append("  • ").append(a).append('\n');
                impacto.append("A devolução será registrada mesmo assim; ajuste essas reservas (troca de item ou estoque).");
            }
            // pergunta antes se houver cobrança (a devolução não pode ser desfeita)
            int r = JOptionPane.showOptionDialog(this,
                    "Registrar a devolução de " + atual.codigo() + " · " + atual.cliente.nome + "?\n\n"
                            + resumo.getText().split("Saldo dos itens")[0].replaceFirst("(?s)^.*?PRÉVIA DA COBRANÇA[^\n]*\n", "")
                            + impacto,
                    "Confirmar devolução", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                    new Object[]{"Registrar", "Cancelar"}, "Cancelar");
            if (r != 0) return;
            Locacao l = atual;
            ctrl.registrarDevolucao(l, dados, usuario);     // o controller valida as regras
            selecionar(l);                                  // mostra o resultado
            if (!afetadas.isEmpty())
                app.aviso("Devolução registrada.\n\nReservas que ficaram sem estoque (o cartão delas no Painel mostra "
                        + "\"Sem estoque\"):\n" + impacto.toString().replaceFirst("(?s)^.*?ESTOQUE:\n", "")
                        .replaceFirst("A devolução será registrada mesmo assim; ", "Próximo passo: "));
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
        if (s == Devolucao.Situacao.NAO_DEVOLVIDO) tabela.editCellAt(i, 3);          // quantas ainda faltam
        else if (s != Devolucao.Situacao.OK) tabela.editCellAt(i, 6);
    }

    /** Tabela da conferência: produto, qtd, situação, unidades afetadas, valor cobrado e se fica indisponível (dano). */
    class ModeloConferencia extends AbstractTableModel {
        private final String[] colunas = {"Produto", "Qtd", "Situação", "Afetadas", "Valor (R$)", "Indisp.?", "Descrição da ocorrência"};
        public int getRowCount() { return dados == null ? 0 : dados.itens.size(); }
        public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int c) { return colunas[c]; }
        @Override public Class<?> getColumnClass(int c) { return c == 5 ? Boolean.class : Object.class; }
        @Override public boolean isCellEditable(int r, int c) {
            if (dados == null || c < 2) return false;
            Devolucao.ItemConferido it = dados.itens.get(r);
            if (c == 2) return true;
            if (it.situacao == Devolucao.Situacao.OK) return false;
            if (it.situacao == Devolucao.Situacao.NAO_DEVOLVIDO) return c == 3;          // só quantas ainda faltam
            return c != 5 || it.situacao == Devolucao.Situacao.DANIFICADO;              // 6 = descrição
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
                else {
                    if (it.qtdAfetada < 1) it.qtdAfetada = 1;                 // começa com 1 unidade (ex.: 1 cadeira de 30)
                    it.indisponibilizar = it.situacao == Devolucao.Situacao.DANIFICADO;
                    if (it.situacao == Devolucao.Situacao.NAO_DEVOLVIDO) { it.valorCobrado = 0; it.observacao = ""; }
                }
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
