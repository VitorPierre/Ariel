package controller;

import model.*;
import util.Cpf;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CONTROLLER de clientes (UC03): cadastro, validação, inadimplência manual,
 * pessoas autorizadas e inativação. A TelaClientes só coleta os dados e mostra as mensagens.
 *
 * Os clientes ficam na mesma lista do PainelController (o painel e os orçamentos usam os mesmos objetos).
 * Quando o banco de dados entrar, é aqui que o "salvar cliente" vai chamar o DAO.
 */
public class ClienteController {

    /** Filtros da lista de clientes. "Todos" = todos os ATIVOS. */
    public static final String[] FILTROS = {"Todos", "Em dia", "Inadimplente", "Inativos"};
    public static final String[] TIPOS_AUTORIZACAO = {"Retira/Devolve", "Retira", "Devolve"};

    // nomes de campo usados em ValidacaoException (a tela põe o cursor no campo certo)
    public static final String CAMPO_CPF = "cpf", CAMPO_NOME = "nome", CAMPO_WHATS = "whatsapp",
            CAMPO_ENDERECO = "endereco", CAMPO_DOCUMENTO = "documento", CAMPO_CONTATO = "contato";

    private final PainelController painel;

    public ClienteController(PainelController painel) { this.painel = painel; }

    // ================= Consultas =================

    public boolean inadimplente(Cliente c) { return painel.inadimplente(c); }

    public String situacao(Cliente c) {
        if (!c.ativo) return "Inativo";
        return inadimplente(c) ? "Inadimplente" : "Em dia";
    }

    /** Clientes para a lista: filtro (FILTROS) + busca por nome ou CPF, em ordem alfabética. */
    public List<Cliente> buscar(String texto, String filtro) {
        String q = texto == null ? "" : texto.trim().toLowerCase();
        String qDigitos = Cpf.digitos(q);
        List<Cliente> r = new ArrayList<>();
        for (Cliente c : painel.clientes) {
            boolean inativos = "Inativos".equals(filtro);
            if (c.ativo == inativos) continue;                    // "Inativos" mostra só inativos; os outros, só ativos
            if ("Em dia".equals(filtro) && inadimplente(c)) continue;
            if ("Inadimplente".equals(filtro) && !inadimplente(c)) continue;
            if (!q.isEmpty()) {
                boolean porNome = c.nome.toLowerCase().contains(q);
                boolean porCpf = !qDigitos.isEmpty() && Cpf.digitos(c.cpf).contains(qDigitos);
                if (!porNome && !porCpf) continue;
            }
            r.add(c);
        }
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        r.sort((a, b) -> ordem.compare(a.nome, b.nome));
        return r;
    }

    /** Clientes que podem receber um NOVO orçamento (ativos). */
    public List<Cliente> ativos() { return painel.clientesAtivos(); }

    public List<Locacao> historicoDe(Cliente c) { return painel.historicoDe(c); }

    /** Situação de uma locação do histórico (mesmo texto dos cartões do painel). */
    public String situacaoLocacao(Locacao l) {
        if (l.arquivada) return l.motivoArquivo.isEmpty() ? "Arquivado" : l.motivoArquivo;
        return painel.aviso(l);
    }

    // ================= Cadastro =================

    /**
     * Cria (existente == null) ou altera um cliente. Valida CPF (dígitos verificadores e único),
     * nome, telefone (se informado) e endereço (obrigatório para locar, MSG013).
     */
    public Cliente salvar(Cliente existente, String cpf, String nome, String rg, String instagram,
                          String whatsapp, String endereco, String observacoes) {
        String cpfDigitos = Cpf.digitos(cpf);
        if (cpfDigitos.isEmpty()) throw new ValidacaoException(CAMPO_CPF, "Informe o CPF do cliente.");
        if (!Cpf.valido(cpfDigitos)) throw new ValidacaoException(CAMPO_CPF, "CPF inválido. Confira os números digitados.");
        for (Cliente o : painel.clientes)
            if (o != existente && Cpf.digitos(o.cpf).equals(cpfDigitos))
                throw new ValidacaoException(CAMPO_CPF, "Já existe um cliente com este CPF: " + o.nome
                        + (o.ativo ? "." : " (inativo — reative o cadastro em vez de criar outro)."));
        if (vazio(nome)) throw new ValidacaoException(CAMPO_NOME, "Informe o nome do cliente.");
        if (!vazio(whatsapp) && Cpf.digitos(whatsapp).length() < 10)
            throw new ValidacaoException(CAMPO_WHATS, "Telefone / WhatsApp incompleto. Informe o DDD e o número.");
        if (vazio(endereco)) throw new ValidacaoException(CAMPO_ENDERECO, "Informe o endereço completo do cliente.");

        Cliente c = existente != null ? existente : new Cliente();
        c.cpf = cpf.trim();
        c.nome = nome.trim();
        c.rg = texto(rg);
        c.instagram = texto(instagram);
        c.whatsapp = texto(whatsapp);
        c.endereco = endereco.trim();
        c.observacoes = texto(observacoes);
        painel.clienteDAO().salvar(c);
        if (existente == null && !painel.clientes.contains(c)) painel.clientes.add(c);
        painel.notificar();
        return c;
    }

    /** Inadimplência é decisão MANUAL (RF03). Devolve a nova situação. */
    public boolean alternarInadimplencia(Cliente c) {
        c.inadimplenteManual = !c.inadimplenteManual;
        painel.clienteDAO().salvar(c);
        painel.notificar();
        return c.inadimplenteManual;
    }

    /** O cliente tem vínculos (orçamentos, locações ou pagamentos)? */
    public boolean temVinculos(Cliente c) { return !painel.historicoDe(c).isEmpty(); }

    /**
     * UC03 FA03 — Excluir ou desativar: SEM vínculos, o cliente é excluído (MSG005); COM vínculos
     * (orçamentos, locações, pagamentos), não pode ser excluído e é DESATIVADO, preservando o histórico (MSG006).
     * A desativação não apaga histórico nem elimina dívidas.
     * @return true se foi excluído; false se foi desativado.
     */
    public boolean excluirOuDesativar(Cliente c) {
        if (!temVinculos(c)) {
            painel.clienteDAO().excluir(c);
            painel.clientes.remove(c);
            painel.notificar();
            return true;
        }
        c.ativo = false;
        painel.clienteDAO().salvar(c);
        painel.notificar();
        return false;
    }

    public void reativar(Cliente c) {
        c.ativo = true;
        painel.clienteDAO().salvar(c);
        painel.notificar();
    }

    // ================= Pessoas autorizadas (UC03) =================

    public Cliente.Autorizada autorizar(Cliente c, String nome, String documento, String contato, String tipo) {
        if (c == null) throw new RegraNegocioException("Salve o cadastro do cliente antes de autorizar pessoas.");
        if (vazio(nome)) throw new ValidacaoException(CAMPO_NOME, "Informe o nome da pessoa.");
        if (vazio(documento)) throw new ValidacaoException(CAMPO_DOCUMENTO, "Informe o documento da pessoa.");
        if (!vazio(contato) && Cpf.digitos(contato).length() < 10)
            throw new ValidacaoException(CAMPO_CONTATO, "Contato incompleto. Informe o DDD e o número.");
        for (Cliente.Autorizada a : c.autorizadas)
            if (a.nome.equalsIgnoreCase(nome.trim())) throw new RegraNegocioException(a.nome + " já está entre as pessoas autorizadas.");
        Cliente.Autorizada a = new Cliente.Autorizada(nome.trim(), documento.trim(), texto(contato),
                vazio(tipo) ? TIPOS_AUTORIZACAO[0] : tipo);
        c.autorizadas.add(a);
        painel.notificar();
        return a;
    }

    public void removerAutorizada(Cliente c, Cliente.Autorizada a) {
        c.autorizadas.remove(a);
        painel.notificar();
    }

    private static boolean vazio(String s) { return s == null || s.trim().isEmpty(); }

    private static String texto(String s) { return s == null ? "" : s.trim(); }
}
