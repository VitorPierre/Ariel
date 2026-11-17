package view;

import controller.RelatorioController;
import controller.RelatorioController.Relatorio;
import controller.RelatorioController.Tipo;
import model.Catalogo;
import model.RegraNegocioException;
import model.Usuario;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * VIEW: tela "Relatórios" (UC18/UC17/UC14, RF19). Só o ADMINISTRADOR emite relatórios;
 * os demais perfis veem apenas o aviso de acesso restrito. Os relatórios só leem os dados.
 * Exporta para CSV (abre no Excel) e PDF.
 */
public class TelaRelatorios extends JPanel {

    private static final String TODOS = "(todos)";

    private final TelaPrincipal app;
    private final RelatorioController ctrl;
    private final Usuario usuario;

    private final JComboBox<Tipo> cbTipo = new JComboBox<>(Tipo.values());
    private final JComboBox<String> cbFiltro = new JComboBox<>();
    private final TelaPrincipal.Campo tfDe = new TelaPrincipal.Campo("dd/mm/aaaa", 13);
    private final TelaPrincipal.Campo tfAte = new TelaPrincipal.Campo("dd/mm/aaaa", 13);
    private final JLabel lblFiltro = TelaPrincipal.rotulo("Filtro", Font.BOLD, 11);
    private final JLabel lblTitulo = TelaPrincipal.rotulo(" ", Font.BOLD, 14);
    private final JLabel lblCriterio = TelaPrincipal.rotulo(" ", Font.PLAIN, 11);
    private final JLabel lblTotais = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final JLabel msg = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final JButton btnCsv = TelaPrincipal.botaoCinza("Exportar CSV", 12);
    private final JButton btnPdf = TelaPrincipal.botaoCinza("Exportar PDF", 12);
    private final List<String[]> linhas = new ArrayList<>();
    private String[] colunas = {};
    private Relatorio atual;

    private final AbstractTableModel modelo = new AbstractTableModel() {
        public int getRowCount() { return linhas.size(); }
        public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int c) { return colunas[c]; }
        public Object getValueAt(int r, int c) { String[] l = linhas.get(r); return c < l.length ? l[c] : ""; }
    };
    private final JTable tabela = new JTable(modelo);

    public TelaRelatorios(TelaPrincipal app, RelatorioController ctrl, Usuario usuario) {
        super(new BorderLayout());
        this.app = app;
        this.ctrl = ctrl;
        this.usuario = usuario;
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        add(TelaPrincipal.rotulo("Relatórios", Font.BOLD, 30), BorderLayout.NORTH);

        if (usuario == null || !usuario.ehAdmin()) {
            JLabel aviso = TelaPrincipal.rotulo("Acesso restrito: só o administrador emite relatórios.", Font.BOLD, 15);
            aviso.setHorizontalAlignment(SwingConstants.CENTER);
            add(aviso, BorderLayout.CENTER);
            return;
        }

        TelaPrincipal.estilizarCombo(cbTipo);
        TelaPrincipal.estilizarCombo(cbFiltro);
        Ui.mascaraData(tfDe);
        Ui.mascaraData(tfAte);
        LocalDate hoje = LocalDate.now();
        tfDe.setText(Ui.data(hoje.withDayOfMonth(1)));
        tfAte.setText(Ui.data(hoje.withDayOfMonth(hoje.lengthOfMonth())));
        cbTipo.setPreferredSize(new Dimension(300, Tema.ALTURA));
        cbFiltro.setPreferredSize(new Dimension(160, Tema.ALTURA));
        tfDe.setPreferredSize(new Dimension(110, Tema.ALTURA));
        tfAte.setPreferredSize(new Dimension(110, Tema.ALTURA));
        cbTipo.addActionListener(e -> ajustarFiltro());

        JButton gerar = Ui.botaoPreto("Gerar", 13);
        gerar.setPreferredSize(new Dimension(100, Tema.ALTURA));
        gerar.addActionListener(e -> gerar());
        btnCsv.setPreferredSize(new Dimension(130, Tema.ALTURA));
        btnPdf.setPreferredSize(new Dimension(130, Tema.ALTURA));
        btnCsv.addActionListener(e -> exportar(true));
        btnPdf.addActionListener(e -> exportar(false));
        btnCsv.setEnabled(false);
        btnPdf.setEnabled(false);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        filtros.setOpaque(false);
        filtros.setBorder(new EmptyBorder(0, -12, 0, 0));
        JPanel pt = Ui.bloco("Relatório", cbTipo), pd = Ui.bloco("De", tfDe), pa = Ui.bloco("Até", tfAte);
        pt.setPreferredSize(new Dimension(300, 66));
        pd.setPreferredSize(new Dimension(118, 66));
        pa.setPreferredSize(new Dimension(118, 66));
        filtros.add(pt);
        filtros.add(pd);
        filtros.add(pa);
        JPanel pf = Ui.bloco("Filtro", cbFiltro);
        pf.setPreferredSize(new Dimension(160, 66));
        filtros.add(pf);
        // os botões ficam na linha dos campos (um rótulo vazio em cima alinha as alturas)
        for (JButton b : new JButton[]{(JButton) gerar, btnCsv, btnPdf}) {
            JPanel pb = Ui.bloco(" ", b);
            pb.setPreferredSize(new Dimension(b == gerar ? 100 : 130, 66));
            filtros.add(pb);
        }

        TelaClientes.estilizarTabela(tabela, 12, 6);
        JScrollPane sp = Ui.rolagem(tabela);

        lblCriterio.setForeground(TelaPrincipal.CINZA_TEXTO);
        JPanel cima = new JPanel(new GridLayout(2, 1, 0, 2));
        cima.setOpaque(false);
        cima.add(lblTitulo);
        cima.add(lblCriterio);
        JPanel rodape = new JPanel(new GridLayout(2, 1, 0, 2));
        rodape.setOpaque(false);
        rodape.add(lblTotais);
        rodape.add(msg);
        JPanel resultado = new JPanel(new BorderLayout(0, 8));
        resultado.setOpaque(false);
        resultado.add(cima, BorderLayout.NORTH);
        resultado.add(sp, BorderLayout.CENTER);
        resultado.add(rodape, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.setBorder(new EmptyBorder(12, 0, 0, 0));
        centro.add(filtros, BorderLayout.NORTH);
        centro.add(Ui.caixa("RESULTADO", resultado), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        ajustarFiltro();
    }

    /** O filtro só existe para Agenda (situação) e Demanda (tema). */
    private void ajustarFiltro() {
        Tipo t = (Tipo) cbTipo.getSelectedItem();
        cbFiltro.removeAllItems();
        Container pai = cbFiltro.getParent();
        if (t == Tipo.AGENDA) {
            for (String s : new String[]{"Todas", "Orçamento", "Locado", "Devolvido"}) cbFiltro.addItem(s);
        } else if (t == Tipo.DEMANDA) {
            cbFiltro.addItem(TODOS);
            for (Catalogo.Tema x : Catalogo.TEMAS) cbFiltro.addItem(x.nome);
        }
        if (pai != null) pai.setVisible(t == Tipo.AGENDA || t == Tipo.DEMANDA);
    }

    /** Recarrega a lista de temas e limpa mensagens (chamado ao entrar na tela). */
    public void atualizar() {
        if (usuario == null || !usuario.ehAdmin()) return;
        Object sel = cbFiltro.getSelectedItem();
        ajustarFiltro();
        if (sel != null) cbFiltro.setSelectedItem(sel);
        msg.setText(" ");
    }

    private void gerar() {
        try {
            Tipo t = (Tipo) cbTipo.getSelectedItem();
            String filtro = (String) cbFiltro.getSelectedItem();
            if (filtro == null || TODOS.equals(filtro)) filtro = null;
            LocalDate de = Ui.lerData(tfDe), ate = Ui.lerData(tfAte);
            atual = ctrl.gerar(t, de, ate, filtro, usuario);
            colunas = atual.colunas;
            linhas.clear();
            linhas.addAll(atual.linhas);
            modelo.fireTableStructureChanged();
            lblTitulo.setText(atual.titulo + " — " + atual.periodo);
            lblCriterio.setText(atual.criterio == null || atual.criterio.isEmpty() ? " " : atual.criterio);
            lblTotais.setText(atual.totais.isEmpty() ? " " : String.join("   |   ", atual.totais));
            boolean tem = !atual.linhas.isEmpty();
            btnCsv.setEnabled(true);
            btnPdf.setEnabled(true);
            msg.setText(tem ? " " : model.Mensagens.MSG007);
        } catch (RegraNegocioException e) {
            msg.setText("<html>" + e.getMessage().replace("\n", "<br>") + "</html>");
        }
    }

    private void exportar(boolean csv) {
        if (atual == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("relatorio." + (csv ? "csv" : "pdf")));
        if (fc.showSaveDialog(app) != JFileChooser.APPROVE_OPTION) return;
        try {
            if (csv) ctrl.exportarCsv(atual, fc.getSelectedFile().toPath());
            else ctrl.exportarPdf(atual, fc.getSelectedFile().toPath());
            msg.setText("Arquivo gerado: " + fc.getSelectedFile().getName());
        } catch (Exception e) {
            msg.setText("Não foi possível gerar o arquivo.");
        }
    }
}
