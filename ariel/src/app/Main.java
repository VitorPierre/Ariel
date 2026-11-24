package app;

import controller.LoginController;
import controller.PainelController;
import controller.UsuarioController;
import dao.ConexaoFabrica;
import dao.ModoPersistencia;
import dao.RepositorioException;
import model.RegraNegocioException;
import model.Usuario;
import view.TelaLogin;
import view.TelaPrincipal;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;

/**
 * Ponto de entrada do sistema "Ariel · Conto de Fadas".
 *
 * Fluxo: Main -> TelaLogin -> (login certo) -> TelaPrincipal -> (Sair) -> TelaLogin ...
 * Os controllers são criados UMA vez aqui, então os dados continuam os mesmos ao sair e entrar de novo.
 *
 * Como compilar e executar: use compilar_e_executar.bat (Windows) ou compilar_e_executar.sh.
 */
public class Main {

    // controllers compartilhados (criado dentro do main: no modo MySQL o construtor já acessa o banco)
    private static UsuarioController usuarios;
    private static PainelController painelMemoria;

    public static void main(String[] args) {
        // Sem ambiente gráfico (ex.: servidor sem tela) não é possível abrir a janela
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Ambiente sem interface gráfica: não foi possível abrir a tela.");
            return;
        }

        // Suavização de texto para os textos ficarem nítidos
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Qualquer erro inesperado aparece numa janela em vez de passar despercebido
        Thread.setDefaultUncaughtExceptionHandler((thread, erro) -> {
            erro.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Ocorreu um erro inesperado:\n" + erro,
                    "Erro", JOptionPane.ERROR_MESSAGE);
        });

        // Tudo que mexe na interface roda na thread própria do Swing
        SwingUtilities.invokeLater(() -> {
            try {
                // Visual base do Java (Metal); cores, fontes e formas vêm de view/Tema.java (DESIGN.md)
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Não foi possível aplicar o visual padrão: " + e.getMessage());
            }
            try {
                usuarios = new UsuarioController();
            } catch (RepositorioException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Não foi possível conectar ao banco de dados.\n\n" + detalhe(e)
                                + "\n\nConfira se o MySQL está ligado e se host, porta, usuário e senha"
                                + "\nem ariel.properties estão certos (ou use modo=memoria).",
                        "Banco de dados indisponível", JOptionPane.ERROR_MESSAGE);
                return;                    // nenhuma janela aberta: o programa termina
            }
            abrirLogin();
        });
    }

    /** Mensagem do erro junto com a causa (ex.: "Access denied for user 'root'"). */
    private static String detalhe(RepositorioException e) {
        Throwable causa = e.getCause();
        return causa == null || causa.getMessage() == null ? e.getMessage() : e.getMessage() + "\n" + causa.getMessage();
    }

    /** Abre a tela de login (um LoginController novo, para zerar o contador de tentativas). */
    private static void abrirLogin() {
        TelaLogin login = new TelaLogin(new LoginController(usuarios), Main::abrirPrincipal);
        login.setVisible(true);
        login.toFront();
    }

    /**
     * Login certo: abre o sistema. Ao clicar em "Sair" volta para o login.
     * Se o banco cair entre o login e a carga dos dados, a falha volta como RegraNegocioException:
     * a TelaLogin mostra a mensagem e continua aberta.
     */
    private static void abrirPrincipal(Usuario usuario) {
        PainelController painel;
        try {
            painel = ConexaoFabrica.modo() == ModoPersistencia.MYSQL
                    ? new PainelController(usuario)
                    : (painelMemoria != null ? painelMemoria : (painelMemoria = new PainelController()));
        } catch (RepositorioException e) {
            e.printStackTrace();
            throw new RegraNegocioException("Não foi possível carregar os dados do banco. Confira se o MySQL está ligado.");
        }
        TelaPrincipal tela = new TelaPrincipal(painel, usuarios, usuario, Main::abrirLogin);
        tela.setVisible(true);
        tela.toFront();
    }
}
