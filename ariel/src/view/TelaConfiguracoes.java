package view;

import controller.UsuarioController;
import model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * VIEW: tela "Configurações".
 *  - Todos os usuários: "Meus dados" (nome, login e troca de senha, que exige a senha atual).
 *  - Somente administrador: lista de usuários e botão para criar novos usuários.
 * As regras (login único, tamanho da senha, quem pode criar) estão no UsuarioController;
 * esta tela só coleta os campos e mostra a mensagem de sucesso ou de erro.
 */
public class TelaConfiguracoes extends JPanel {

    private final UsuarioController ctrl;
    private final Usuario eu;                 // usuário logado
    private final Runnable aoAlterarMeusDados;

    // ----- meus dados -----
    private final TelaPrincipal.Campo tfNome = new TelaPrincipal.Campo("Nome", 13);
    private final TelaPrincipal.Campo tfLogin = new TelaPrincipal.Campo("Nome de usuário", 13);
    private final CampoSenha pfAtual = new CampoSenha("Senha atual (só para trocar a senha)", 13);
    private final CampoSenha pfNova = new CampoSenha("Nova senha (mín. " + Regras.SENHA_MIN + " caracteres)", 13);
    private final CampoSenha pfConfirma = new CampoSenha("Confirmar nova senha", 13);
    private final JLabel msgMeus = TelaPrincipal.rotulo(" ", Font.BOLD, 12);

    // ----- novo usuário (administrador) -----
    private final TelaPrincipal.Campo tfNovoNome = new TelaPrincipal.Campo("Nome do usuário", 13);
    private final TelaPrincipal.Campo tfNovoLogin = new TelaPrincipal.Campo("Nome de usuário (login)", 13);
    private final JComboBox<Perfil> cbPerfil = new JComboBox<>(Perfil.values());
    private final CampoSenha pfNovaSenha = new CampoSenha("Senha (mín. " + Regras.SENHA_MIN + " caracteres)", 13);
    private final CampoSenha pfNovaConfirma = new CampoSenha("Confirmar senha", 13);
    private final JLabel msgNovo = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final List<Usuario> lista = new ArrayList<>();
    private final ModeloUsuarios modelo = new ModeloUsuarios();

    public TelaConfiguracoes(UsuarioController ctrl, Usuario eu, Runnable aoAlterarMeusDados) {
        super(new BorderLayout());
        this.ctrl = ctrl;
        this.eu = eu;
        this.aoAlterarMeusDados = aoAlterarMeusDados;
        setBackground(TelaPrincipal.BRANCO);
        setBorder(new EmptyBorder(14, 16, 16, 16));

        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Configurações", Font.BOLD, 20), BorderLayout.WEST);
        add(cab, BorderLayout.NORTH);

        // administrador vê duas colunas (meus dados + usuários); funcionário vê só "meus dados"
        JPanel corpo = new JPanel(new GridLayout(1, eu.ehAdmin() ? 2 : 1, 16, 0));
        corpo.setOpaque(false);
        corpo.add(criarMeusDados());
        if (eu.ehAdmin()) corpo.add(criarUsuarios());
        JPanel alinhado = new JPanel(new BorderLayout());
        alinhado.setOpaque(false);
        alinhado.add(corpo, BorderLayout.NORTH);
        add(alinhado, BorderLayout.CENTER);

        atualizar();
    }

    /** Caixa com título, no estilo das outras telas (borda cinza escura). */
    private JPanel caixa(String titulo) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(TelaPrincipal.BRANCO);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(0x666666), 2), new EmptyBorder(10, 12, 12, 12)));
        JLabel t = TelaPrincipal.rotulo(titulo, Font.BOLD, 12);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(8));
        return p;
    }

    private void adicionar(JPanel p, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        c.setPreferredSize(new Dimension(100, 36));
        p.add(c);
        p.add(Box.createVerticalStrut(8));
    }

    private JPanel criarMeusDados() {
        JPanel p = caixa("MEUS DADOS");
        adicionar(p, tfNome);
        adicionar(p, tfLogin);
        JLabel dica = TelaPrincipal.rotulo("Para trocar a senha, preencha os três campos abaixo:", Font.PLAIN, 11);
        dica.setForeground(TelaPrincipal.CINZA_TEXTO);
        dica.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(dica);
        p.add(Box.createVerticalStrut(6));
        adicionar(p, pfAtual);
        adicionar(p, pfNova);
        adicionar(p, pfConfirma);
        msgMeus.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(msgMeus);
        p.add(Box.createVerticalStrut(8));
        JButton salvar = TelaPrincipal.botaoCinza("Salvar meus dados", 13);
        adicionar(p, salvar);
        salvar.addActionListener(e -> salvarMeusDados());
        return p;
    }

    private JPanel criarUsuarios() {
        JPanel p = caixa("USUÁRIOS DO SISTEMA");
        JTable tabela = new JTable(modelo);
        TelaClientes.estilizarTabela(tabela, 12, 6);
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(new LineBorder(TelaPrincipal.CINZA_LINHA, 1));
        rolagem.getViewport().setBackground(TelaPrincipal.BRANCO);
        rolagem.setAlignmentX(Component.LEFT_ALIGNMENT);
        rolagem.setPreferredSize(new Dimension(100, 100));
        rolagem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        p.add(rolagem);
        p.add(Box.createVerticalStrut(12));

        JLabel t = TelaPrincipal.rotulo("NOVO USUÁRIO", Font.BOLD, 12);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(8));
        adicionar(p, tfNovoNome);
        adicionar(p, tfNovoLogin);
        TelaPrincipal.estilizarCombo(cbPerfil);
        adicionar(p, cbPerfil);
        adicionar(p, pfNovaSenha);
        adicionar(p, pfNovaConfirma);
        msgNovo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(msgNovo);
        p.add(Box.createVerticalStrut(8));
        JButton criar = TelaPrincipal.botaoCinza("+ Criar usuário", 13);
        adicionar(p, criar);
        criar.addActionListener(e -> criarUsuario());
        return p;
    }

    /** Volta os campos para os dados atuais (chamado ao abrir a tela). */
    void atualizar() {
        tfNome.setText(eu.nome());
        tfLogin.setText(eu.login());
        pfAtual.setText("");
        pfNova.setText("");
        pfConfirma.setText("");
        msgMeus.setText(" ");
        msgNovo.setText(" ");
        lista.clear();
        lista.addAll(ctrl.listar());
        modelo.fireTableDataChanged();
    }

    private void salvarMeusDados() {
        try {
            ctrl.alterarMeusDados(eu, tfNome.getText(), tfLogin.getText(),
                    pfAtual.getPassword(), pfNova.getPassword(), pfConfirma.getPassword());
            aoAlterarMeusDados.run();           // atualiza o nome no menu lateral
            atualizar();
            msgMeus.setText("Dados salvos.");
        } catch (RegraNegocioException e) {
            msgMeus.setText("ALERTA: " + e.getMessage());
        }
    }

    private void criarUsuario() {
        try {
            Usuario u = ctrl.criar(eu, tfNovoNome.getText(), tfNovoLogin.getText(),
                    pfNovaSenha.getPassword(), pfNovaConfirma.getPassword(), (Perfil) cbPerfil.getSelectedItem());
            tfNovoNome.setText("");
            tfNovoLogin.setText("");
            pfNovaSenha.setText("");
            pfNovaConfirma.setText("");
            atualizar();
            msgNovo.setText("Usuário \"" + u.login() + "\" criado.");
        } catch (RegraNegocioException e) {
            msgNovo.setText("ALERTA: " + e.getMessage());
        }
    }

    class ModeloUsuarios extends AbstractTableModel {
        private final String[] colunas = {"Nome", "Login", "Perfil"};
        public int getRowCount() { return lista.size(); }
        public int getColumnCount() { return colunas.length; }
        @Override public String getColumnName(int c) { return colunas[c]; }
        public Object getValueAt(int i, int c) {
            Usuario u = lista.get(i);
            return c == 0 ? u.nome() : c == 1 ? u.login() : u.perfil().toString();
        }
    }
}
