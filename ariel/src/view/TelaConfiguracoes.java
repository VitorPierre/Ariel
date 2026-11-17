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
    private final TelaPrincipal.Campo tfLogin = new TelaPrincipal.Campo("login", 13);
    private final CampoSenha pfAtual = new CampoSenha("", 13);
    private final CampoSenha pfNova = new CampoSenha("", 13);
    private final CampoSenha pfConfirma = new CampoSenha("", 13);
    private final JLabel msgMeus = TelaPrincipal.rotulo(" ", Font.BOLD, 12);

    // ----- novo usuário (administrador) -----
    private final TelaPrincipal.Campo tfNovoNome = new TelaPrincipal.Campo("Nome e sobrenome", 13);
    private final TelaPrincipal.Campo tfNovoLogin = new TelaPrincipal.Campo("login", 13);
    private final JComboBox<Perfil> cbPerfil = new JComboBox<>(Perfil.values());
    private final CampoSenha pfNovaSenha = new CampoSenha("", 13);
    private final CampoSenha pfNovaConfirma = new CampoSenha("", 13);
    private final JLabel msgNovo = TelaPrincipal.rotulo(" ", Font.BOLD, 12);
    private final List<Usuario> lista = new ArrayList<>();
    private final ModeloUsuarios modelo = new ModeloUsuarios();

    public TelaConfiguracoes(UsuarioController ctrl, Usuario eu, Runnable aoAlterarMeusDados) {
        super(new BorderLayout());
        this.ctrl = ctrl;
        this.eu = eu;
        this.aoAlterarMeusDados = aoAlterarMeusDados;
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.setBorder(new EmptyBorder(0, 0, 14, 0));
        cab.add(TelaPrincipal.rotulo("Configurações", Font.BOLD, 30), BorderLayout.WEST);
        add(cab, BorderLayout.NORTH);

        // administrador vê duas colunas (meus dados + usuários); funcionário vê só "meus dados"
        JPanel corpo = new JPanel(new GridLayout(1, eu.ehAdmin() ? 2 : 1, 20, 0));
        corpo.setOpaque(false);
        corpo.add(criarMeusDados());
        if (eu.ehAdmin()) corpo.add(criarUsuarios());
        JPanel alinhado = new JPanel(new BorderLayout());
        alinhado.setOpaque(false);
        alinhado.add(corpo, BorderLayout.NORTH);
        add(alinhado, BorderLayout.CENTER);

        atualizar();
    }

    /** Seção com título (DESIGN.md 5 e 6): cartão branco arredondado, rótulo de seção em cima. */
    private JPanel caixa(String titulo) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(TelaPrincipal.BRANCO);
        p.setBorder(new Tema.Arredondada(Tema.BORDA_SECAO, 1, Tema.RAIO_SECAO, 14, 16));
        p.add(tituloSecao(titulo));
        p.add(Box.createVerticalStrut(10));
        return p;
    }

    private static JLabel tituloSecao(String titulo) {
        JLabel t = TelaPrincipal.rotulo(titulo, Font.BOLD, 12);
        t.setFont(Tema.fonteSecao());
        t.setForeground(Tema.TEXTO_SUAVE);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }

    /** Uma linha do formulário: cada campo com o rótulo ACIMA; dois campos dividem a largura. */
    private void linha(JPanel p, String r1, JComponent c1, String r2, JComponent c2) {
        JPanel l = new JPanel(new GridLayout(1, c2 == null ? 1 : 2, 12, 0));
        l.setOpaque(false);
        l.add(Ui.bloco(r1, c1));
        if (c2 != null) l.add(Ui.bloco(r2, c2));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height));
        p.add(l);
        p.add(Box.createVerticalStrut(12));
    }

    private void botao(JPanel p, JButton b) {
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTURA));
        p.add(b);
    }

    private JPanel criarMeusDados() {
        JPanel p = caixa("MEUS DADOS");
        linha(p, "Nome", tfNome, "Nome de usuário (login)", tfLogin);
        JLabel dica = TelaPrincipal.rotulo("Para trocar a senha, preencha os três campos abaixo.", Font.PLAIN, 12);
        dica.setForeground(Tema.TEXTO_SUAVE);
        dica.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(dica);
        p.add(Box.createVerticalStrut(10));
        linha(p, "Senha atual", pfAtual, null, null);
        linha(p, "Nova senha (mín. " + Regras.SENHA_MIN + " caracteres)", pfNova, "Confirmar nova senha", pfConfirma);
        msgMeus.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgMeus.setForeground(Tema.PRIMARIA_ESCURA);
        p.add(msgMeus);
        p.add(Box.createVerticalStrut(8));
        JButton salvar = TelaPrincipal.botaoPrimario("Salvar meus dados", 13);
        botao(p, salvar);
        salvar.addActionListener(e -> salvarMeusDados());
        return p;
    }

    private JPanel criarUsuarios() {
        JPanel p = caixa("USUÁRIOS DO SISTEMA");
        JTable tabela = new JTable(modelo);
        TelaClientes.estilizarTabela(tabela, 12, 6);
        JScrollPane rolagem = Ui.rolagem(tabela);
        rolagem.setAlignmentX(Component.LEFT_ALIGNMENT);
        rolagem.setPreferredSize(new Dimension(100, 150));
        rolagem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        p.add(rolagem);
        p.add(Box.createVerticalStrut(16));

        p.add(tituloSecao("NOVO USUÁRIO"));
        p.add(Box.createVerticalStrut(10));
        TelaPrincipal.estilizarCombo(cbPerfil);
        linha(p, "Nome", tfNovoNome, "Nome de usuário (login)", tfNovoLogin);
        linha(p, "Perfil", cbPerfil, null, null);
        linha(p, "Senha (mín. " + Regras.SENHA_MIN + " caracteres)", pfNovaSenha, "Confirmar senha", pfNovaConfirma);
        msgNovo.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgNovo.setForeground(Tema.PRIMARIA_ESCURA);
        p.add(msgNovo);
        p.add(Box.createVerticalStrut(8));
        JButton criar = TelaPrincipal.botaoCinza("+ Criar usuário", 13);
        botao(p, criar);
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
            msgMeus.setText("<html>" + e.getMessage().replace("\n", "<br>") + "</html>");
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
            msgNovo.setText("<html>" + e.getMessage().replace("\n", "<br>") + "</html>");
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
