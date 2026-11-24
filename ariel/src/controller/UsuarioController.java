package controller;

import dao.ConexaoFabrica;
import dao.ModoPersistencia;
import dao.UsuarioDAO;
import dao.UsuarioDAOJdbc;
import dao.UsuarioDAOMemoria;
import model.Perfil;
import model.RegraNegocioException;
import model.Regras;
import model.Usuario;
import util.Senha;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CONTROLLER dos usuários: aplica as regras de cadastro e persiste via UsuarioDAO
 * (MySQL ou memória, conforme ariel.properties).
 */
public class UsuarioController {
    private final List<Usuario> usuarios = new ArrayList<>();
    private final UsuarioDAO dao;

    public UsuarioController() {
        if (ConexaoFabrica.modo() == ModoPersistencia.MYSQL) {
            this.dao = new UsuarioDAOJdbc();
            this.usuarios.addAll(dao.listar());
            if (this.usuarios.isEmpty()) {
                // Usuários iniciais para o primeiro acesso no banco vazio. TROQUE as senhas antes de usar de verdade.
                Usuario admin = new Usuario("Proprietária", "admin", Senha.gerarHash("admin123".toCharArray()), Perfil.ADMIN);
                Usuario func = new Usuario("Funcionário", "funcionario", Senha.gerarHash("func123".toCharArray()), Perfil.FUNCIONARIO);
                dao.salvar(admin);
                dao.salvar(func);
                usuarios.add(admin);
                usuarios.add(func);
            }
        } else {
            // Usuários iniciais para testar. TROQUE as senhas antes de usar de verdade.
            usuarios.add(new Usuario("Proprietária", "admin", Senha.gerarHash("admin123".toCharArray()), Perfil.ADMIN));
            usuarios.add(new Usuario("Funcionário", "funcionario", Senha.gerarHash("func123".toCharArray()), Perfil.FUNCIONARIO));
            this.dao = new UsuarioDAOMemoria(usuarios);
        }
    }

    public List<Usuario> listar() { return new ArrayList<>(usuarios); }

    /** Procura pelo login, sem diferenciar maiúsculas de minúsculas. */
    public Usuario buscar(String login) {
        if (login == null) return null;
        String l = login.trim();
        for (Usuario u : usuarios) if (u.login().equalsIgnoreCase(l)) return u;
        return null;
    }

    /** Só o administrador cria usuários. Valida nome, login único, senha e confirmação. */
    public Usuario criar(Usuario quem, String nome, String login, char[] senha, char[] confirma, Perfil perfil) {
        try {
            if (quem == null || !quem.ehAdmin())
                throw new RegraNegocioException("Somente administradores podem criar usuários.");
            exigirNome(nome);
            String lg = validarLogin(login, null);
            validarNovaSenha(senha, confirma);
            if (perfil == null) throw new RegraNegocioException("Escolha o perfil do usuário.");
            Usuario u = new Usuario(nome.trim(), lg, Senha.gerarHash(senha), perfil);
            dao.salvar(u);
            if (!usuarios.contains(u)) usuarios.add(u);
            return u;
        } finally {
            apagar(senha, confirma);
        }
    }

    /**
     * Cada usuário altera apenas os PRÓPRIOS dados (nome, login e, se quiser, a senha).
     * Para trocar a senha é obrigatório informar a senha atual.
     */
    public void alterarMeusDados(Usuario u, String nome, String login,
                                 char[] senhaAtual, char[] nova, char[] confirma) {
        try {
            exigirNome(nome);
            String lg = validarLogin(login, u);
            boolean trocaSenha = nova != null && nova.length > 0;
            if (trocaSenha) {
                if (senhaAtual == null || !Senha.confere(senhaAtual, u.senhaHash()))
                    throw new RegraNegocioException("A senha atual está incorreta.");
                validarNovaSenha(nova, confirma);
            }
            // só altera depois de validar tudo (não deixa o cadastro pela metade)
            u.alterarNome(nome.trim());
            u.alterarLogin(lg);
            if (trocaSenha) u.alterarSenhaHash(Senha.gerarHash(nova));
            dao.salvar(u);
        } finally {
            apagar(senhaAtual, nova, confirma);
        }
    }

    // ----- validações -----
    private void exigirNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) throw new RegraNegocioException("Informe o nome.");
    }

    /** Login: mínimo de caracteres, só letras/números/ponto/traço, e único (ignorando o próprio usuário). */
    private String validarLogin(String login, Usuario dono) {
        String l = login == null ? "" : login.trim().toLowerCase();
        if (l.length() < Regras.LOGIN_MIN)
            throw new RegraNegocioException("O usuário deve ter pelo menos " + Regras.LOGIN_MIN + " caracteres.");
        if (!l.matches("[a-z0-9._-]+"))
            throw new RegraNegocioException("O usuário só pode ter letras, números, ponto, traço e sublinhado (sem espaços).");
        Usuario outro = buscar(l);
        if (outro != null && outro != dono) throw new RegraNegocioException("Já existe um usuário com este login.");
        return l;
    }

    private void validarNovaSenha(char[] senha, char[] confirma) {
        if (senha == null || senha.length < Regras.SENHA_MIN)
            throw new RegraNegocioException("A senha deve ter pelo menos " + Regras.SENHA_MIN + " caracteres.");
        if (!Arrays.equals(senha, confirma))
            throw new RegraNegocioException("A confirmação da senha não confere.");
    }

    private static void apagar(char[]... senhas) {   // não deixa senha em memória
        for (char[] s : senhas) if (s != null) Arrays.fill(s, '\0');
    }
}
