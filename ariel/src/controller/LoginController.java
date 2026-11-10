package controller;

import model.RegraNegocioException;
import model.Regras;
import model.Usuario;
import util.Senha;

import java.util.Arrays;

/**
 * CONTROLLER do login: confere usuário e senha. Regra de segurança: depois de
 * várias tentativas erradas seguidas, o login fica travado por alguns segundos.
 */
public class LoginController {
    private final UsuarioController usuarios;
    private int falhas = 0;
    private long bloqueadoAte = 0;

    public LoginController(UsuarioController usuarios) { this.usuarios = usuarios; }

    /** Devolve o usuário autenticado ou lança RegraNegocioException com a mensagem para a tela. */
    public Usuario entrar(String login, char[] senha) {
        try {
            long agora = System.currentTimeMillis();
            if (agora < bloqueadoAte) {
                long seg = (bloqueadoAte - agora + 999) / 1000;
                throw new RegraNegocioException("Muitas tentativas. Aguarde " + seg + " s para tentar de novo.");
            }
            if (login == null || login.trim().isEmpty() || senha.length == 0)
                throw new RegraNegocioException("Informe o usuário e a senha.");

            Usuario u = usuarios.buscar(login);
            boolean ok = u != null && Senha.confere(senha, u.senhaHash());
            if (!ok) {
                if (++falhas >= Regras.MAX_TENTATIVAS_LOGIN) {
                    falhas = 0;
                    bloqueadoAte = agora + Regras.BLOQUEIO_LOGIN_SEGUNDOS * 1000L;
                    throw new RegraNegocioException("Muitas tentativas. Aguarde "
                            + Regras.BLOQUEIO_LOGIN_SEGUNDOS + " s para tentar de novo.");
                }
                // mesma mensagem para "usuário não existe" e "senha errada": não revela quais logins existem
                throw new RegraNegocioException("Usuário ou senha inválidos.");
            }
            falhas = 0;
            return u;
        } finally {
            Arrays.fill(senha, '\0');
        }
    }
}
