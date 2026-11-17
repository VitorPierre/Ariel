package app;

import controller.LoginController;
import controller.PainelController;
import controller.UsuarioController;
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

    // controllers compartilhados (dados em memória enquanto o programa estiver aberto)
    private static final UsuarioController USUARIOS = new UsuarioController();
    private static final PainelController PAINEL = new PainelController();

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
            abrirLogin();
        });
    }

    /** Abre a tela de login (um LoginController novo, para zerar o contador de tentativas). */
    private static void abrirLogin() {
        TelaLogin login = new TelaLogin(new LoginController(USUARIOS), Main::abrirPrincipal);
        login.setVisible(true);
        login.toFront();
    }

    /** Login certo: abre o sistema. Ao clicar em "Sair" volta para o login. */
    private static void abrirPrincipal(Usuario usuario) {
        TelaPrincipal tela = new TelaPrincipal(PAINEL, USUARIOS, usuario, Main::abrirLogin);
        tela.setVisible(true);
        tela.toFront();
    }
}
