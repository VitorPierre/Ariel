package controller;

import model.*;

import java.util.Map;

/**
 * CONTROLLER do catálogo (UC06, UC08, UC09, UC10, UC12): produtos, pacotes, temas, categorias e serviços.
 * Regras principais:
 *  - nada é excluído: o cadastro é INATIVADO (some das novas escolhas, mas o histórico fica);
 *  - estoque por QUANTIDADE: total e indisponíveis (dano), nunca por peça física;
 *  - pacotes usam o mesmo estoque dos produtos avulsos;
 *  - não dá para reduzir o estoque abaixo do que já está reservado (UC08 FA07).
 */
public class CatalogoController {

    private final PainelController painel;

    public CatalogoController(PainelController painel) { this.painel = painel; }

    // ================= Disponibilidade =================

    /** Unidades livres HOJE (estoque útil menos reservas e itens que estão fora). Para pacote: o menor entre os componentes. */
    public int disponivelHoje(Catalogo.Item item) {
        int menor = Integer.MAX_VALUE;
        for (Catalogo.Item p : item.produtos()) {
            int por = Math.max(1, item.consumo(p));
            menor = Math.min(menor, Math.max(0, painel.livre(p, painel.hoje(), painel.hoje(), null)) / por);
        }
        return menor == Integer.MAX_VALUE ? 0 : menor;
    }

    // ================= Produtos =================

    /** Cria (existente == null) ou atualiza um produto. */
    public Catalogo.Item salvarProduto(Catalogo.Item existente, String nome, String cor, Catalogo.Categoria categoria,
                                       double preco, int total, int indisponivel) {
        nome = obrigatorio(nome);
        if (categoria == null) throw new RegraNegocioException(Mensagens.MSG002 + "\n(escolha a categoria; cadastre uma na aba Temas e categorias se não houver)");
        if (preco <= 0 || total < 0 || indisponivel < 0 || indisponivel > total)
            throw new RegraNegocioException(Mensagens.MSG011 + "\n(valor maior que zero; indisponíveis não podem passar do total)");
        nomeLivre(nome, existente);
        if (existente != null && existente.tipo != Catalogo.Tipo.PRODUTO) throw new IllegalArgumentException("não é produto");

        if (existente != null) {            // UC08 FA07: ajuste incompatível com reservas
            int util = total - indisponivel;
            for (Locacao k : painel.locacoes) {
                if (k.arquivada || k.etapa == Etapa.DEVOLVIDO || (k.etapa == Etapa.ORCAMENTO && !k.temEntrada())) continue;
                int usa = 0;
                for (Catalogo.Linha li : k.itens) usa += li.qtd * li.item.consumo(existente);
                if (usa > 0 && painel.comprometido(existente, k.retirada, k.devolucao) > util)
                    throw new RegraNegocioException("Ajuste incompatível com reservas: a locação " + k.codigo() + " · "
                            + k.cliente.nome + " precisa de \"" + existente.nome + "\" e ficaria sem estoque.");
            }
        }
        Catalogo.Item p = existente != null ? existente : new Catalogo.Item(nome, Catalogo.Tipo.PRODUTO, preco, total);
        p.nome = nome;
        p.cor = cor == null ? "" : cor.trim();
        p.categoria = categoria;
        p.preco = preco;
        p.estoque = total;
        p.indisponivel = indisponivel;
        painel.catalogoDAO().salvarItem(p);
        if (existente == null && !Catalogo.ITENS.contains(p)) Catalogo.ITENS.add(p);
        painel.notificar();
        return p;
    }

    // ================= Pacotes =================

    public Catalogo.Item salvarPacote(Catalogo.Item existente, String nome, String descricao, Catalogo.Tema tema,
                                      double preco, Map<Catalogo.Item, Integer> componentes) {
        nome = obrigatorio(nome);
        if (preco <= 0) throw new RegraNegocioException(Mensagens.MSG011 + "\n(o preço do pacote deve ser maior que zero)");
        if (componentes.isEmpty())
            throw new RegraNegocioException("Composição inválida: o pacote precisa de ao menos um produto.");
        for (Map.Entry<Catalogo.Item, Integer> e : componentes.entrySet())
            if (e.getKey().tipo != Catalogo.Tipo.PRODUTO || e.getValue() == null || e.getValue() <= 0)
                throw new RegraNegocioException("Composição inválida: use produtos com quantidades positivas.");
        nomeLivre(nome, existente);
        Catalogo.Item p = existente != null ? existente : new Catalogo.Item(nome, Catalogo.Tipo.PACOTE, preco, 0);
        p.nome = nome;
        p.descricao = descricao == null ? "" : descricao.trim();
        p.tema = tema;
        p.preco = preco;
        p.componentes.clear();                  // contratos e locações antigas guardam o que foi vendido (as Linhas), não mudam
        p.componentes.putAll(componentes);
        painel.catalogoDAO().salvarItem(p);
        if (existente == null && !Catalogo.ITENS.contains(p)) Catalogo.ITENS.add(p);
        painel.notificar();
        return p;
    }

    // ================= Categorias, temas e serviços =================

    public Catalogo.Categoria salvarCategoria(Catalogo.Categoria existente, String nome, String descricao) {
        nome = obrigatorio(nome);
        for (Catalogo.Categoria c : Catalogo.CATEGORIAS)
            if (c != existente && c.nome.equalsIgnoreCase(nome)) throw new RegraNegocioException(Mensagens.MSG003);
        Catalogo.Categoria c = existente != null ? existente : new Catalogo.Categoria(nome, "");
        c.nome = nome;
        c.descricao = descricao == null ? "" : descricao.trim();
        painel.catalogoDAO().salvarCategoria(c);
        if (existente == null && !Catalogo.CATEGORIAS.contains(c)) Catalogo.CATEGORIAS.add(c);
        painel.notificar();
        return c;
    }

    public Catalogo.Tema salvarTema(Catalogo.Tema existente, String nome, String descricao) {
        nome = obrigatorio(nome);
        for (Catalogo.Tema t : Catalogo.TEMAS)
            if (t != existente && t.nome.equalsIgnoreCase(nome)) throw new RegraNegocioException(Mensagens.MSG003);
        Catalogo.Tema t = existente != null ? existente : new Catalogo.Tema(nome, "");
        t.nome = nome;
        t.descricao = descricao == null ? "" : descricao.trim();
        painel.catalogoDAO().salvarTema(t);
        if (existente == null && !Catalogo.TEMAS.contains(t)) Catalogo.TEMAS.add(t);
        painel.notificar();
        return t;
    }

    public Catalogo.Servico salvarServico(Catalogo.Servico existente, String nome, String descricao, double valor) {
        nome = obrigatorio(nome);
        if (valor < 0) throw new RegraNegocioException(Mensagens.MSG011);
        for (Catalogo.Servico x : Catalogo.SERVICOS)
            if (x != existente && x.nome.equalsIgnoreCase(nome)) throw new RegraNegocioException(Mensagens.MSG003);
        Catalogo.Servico s = existente != null ? existente : new Catalogo.Servico(nome, "", valor);
        s.nome = nome;
        s.descricao = descricao == null ? "" : descricao.trim();
        s.valor = valor;       // preço de catálogo: contratos anteriores NÃO mudam (cada um guarda o valor contratado)
        painel.catalogoDAO().salvarServico(s);
        if (existente == null && !Catalogo.SERVICOS.contains(s)) Catalogo.SERVICOS.add(s);
        painel.notificar();
        return s;
    }

    // ================= Inativar / reativar =================

    /**
     * Inativa um cadastro ativo (ou reativa um inativo): produto, pacote, categoria, tema ou serviço.
     * Reativar produto NÃO desfaz indisponibilidade por dano (UC08). Devolve um aviso (ou null).
     */
    public String alternarAtivo(Object cadastro) {
        String aviso = null;
        boolean novo;
        if (cadastro instanceof Catalogo.Item i) {
            novo = !i.ativo;
            i.ativo = novo;
            painel.catalogoDAO().salvarItem(i);
            if (!novo) {
                int n = 0;
                for (Locacao k : painel.locacoes) {
                    if (k.arquivada || k.etapa == Etapa.DEVOLVIDO) continue;
                    for (Catalogo.Linha li : k.itens) if (li.item == i || li.item.consumo(i) > 0) { n++; break; }
                }
                if (n > 0) aviso = n + " reserva(s) em aberto usam este item e continuam valendo.";
            }
        } else if (cadastro instanceof Catalogo.Categoria c) { novo = !c.ativo; c.ativo = novo; painel.catalogoDAO().salvarCategoria(c); }
        else if (cadastro instanceof Catalogo.Tema t) { novo = !t.ativo; t.ativo = novo; painel.catalogoDAO().salvarTema(t); }
        else if (cadastro instanceof Catalogo.Servico s) { novo = !s.ativo; s.ativo = novo; painel.catalogoDAO().salvarServico(s); }
        else throw new IllegalArgumentException("cadastro desconhecido");
        painel.notificar();
        return aviso;
    }

    // ================= Excluir ou desativar (UC06, UC08, UC09, UC10, UC12 — FA02) =================

    /** O cadastro tem vínculos (está em orçamentos/locações, compõe pacotes, classifica produtos...)? */
    public boolean temVinculos(Object cadastro) {
        if (cadastro instanceof Catalogo.Item i) {
            for (Locacao k : painel.locacoes)
                for (Catalogo.Linha li : k.itens) if (li.item == i || li.item.consumo(i) > 0) return true;
            if (i.tipo == Catalogo.Tipo.PRODUTO)
                for (Catalogo.Item p : Catalogo.ITENS) if (p.componentes.containsKey(i)) return true;
            return false;
        }
        if (cadastro instanceof Catalogo.Categoria c) {
            for (Catalogo.Item p : Catalogo.ITENS) if (p.categoria == c) return true;
            return false;
        }
        if (cadastro instanceof Catalogo.Tema t) {
            for (Catalogo.Item p : Catalogo.ITENS) if (p.tema == t) return true;
            return false;
        }
        if (cadastro instanceof Catalogo.Servico s) {
            for (Locacao k : painel.locacoes)
                for (ServicoContratado sc : k.servicos) if (sc.servico == s) return true;
            return false;
        }
        throw new IllegalArgumentException("cadastro desconhecido");
    }

    /**
     * FA02 "Excluir ou desativar": SEM vínculos o cadastro é excluído (MSG005); COM vínculos não pode ser
     * excluído e é DESATIVADO, preservando o histórico (MSG006). Devolve a mensagem para a tela.
     */
    public String excluirOuDesativar(Object cadastro) {
        if (!temVinculos(cadastro)) {
            if (cadastro instanceof Catalogo.Item i) { painel.catalogoDAO().excluirItem(i); Catalogo.ITENS.remove(i); }
            else if (cadastro instanceof Catalogo.Categoria c) { painel.catalogoDAO().excluirCategoria(c); Catalogo.CATEGORIAS.remove(c); }
            else if (cadastro instanceof Catalogo.Tema t) { painel.catalogoDAO().excluirTema(t); Catalogo.TEMAS.remove(t); }
            else if (cadastro instanceof Catalogo.Servico s) { painel.catalogoDAO().excluirServico(s); Catalogo.SERVICOS.remove(s); }
            painel.notificar();
            return Mensagens.MSG005;
        }
        String aviso = alternarAtivo(cadastro);           // ativo -> desativado
        return Mensagens.MSG006 + (aviso == null ? "" : "\n" + aviso);
    }

    // ================= Auxiliares =================

    private static String obrigatorio(String nome) {
        if (nome == null || nome.isBlank()) throw new RegraNegocioException(Mensagens.MSG002);
        return nome.trim();
    }

    private static void nomeLivre(String nome, Catalogo.Item existente) {
        for (Catalogo.Item i : Catalogo.ITENS)
            if (i != existente && i.nome.equalsIgnoreCase(nome)) throw new RegraNegocioException(Mensagens.MSG003);
    }
}
