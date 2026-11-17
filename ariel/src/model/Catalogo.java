package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo da empresa (dados em memória):
 *  - Categoria: classifica o tipo de produto (cadeiras, painéis, vestidos...).
 *  - Tema     : assunto da decoração (princesas, realeza...). Agrupa pacotes e mede a demanda (UC17).
 *  - Produto  : item avulso, com valor de locação e estoque POR QUANTIDADE (não por peça física, UC08).
 *  - Pacote   : conjunto de produtos com preço fechado; usa o MESMO estoque dos produtos avulsos (RF12).
 *  - Serviço  : entrega, montagem, balonismo... com valor de referência.
 *
 * Excluir (FA02 de UC06/UC08/UC09/UC10/UC12): sem vínculos o cadastro é apagado; com vínculos é DESATIVADO
 * (ativo = false) e o histórico é preservado.
 */
public class Catalogo {

    public enum Tipo { PRODUTO, PACOTE }

    /** Categoria de produtos. */
    public static class Categoria {
        public String nome, descricao;
        public boolean ativo = true;
        public Categoria(String nome, String descricao) { this.nome = nome; this.descricao = descricao; }
        @Override public String toString() { return nome; }
    }

    /** Tema de decoração. */
    public static class Tema {
        public String nome, descricao;
        public boolean ativo = true;
        public Tema(String nome, String descricao) { this.nome = nome; this.descricao = descricao; }
        @Override public String toString() { return nome; }
    }

    /** Serviço opcional vendido junto com a locação. */
    public static class Servico {
        public String nome, descricao;
        public double valor;                 // valor de referência
        public boolean ativo = true;
        public Servico(String nome, String descricao, double valor) {
            this.nome = nome; this.descricao = descricao; this.valor = valor;
        }
        @Override public String toString() { return nome; }
    }

    /** Produto ou pacote do catálogo. */
    public static class Item {
        public String nome;
        public final Tipo tipo;
        public double preco;
        public int estoque;                  // total de unidades (só produto)
        public int indisponivel;             // unidades danificadas/fora de uso (só produto)
        public String descricao = "";
        public String cor = "";              // cor / material / personagem (só produto)
        public Categoria categoria;          // só produto
        public Tema tema;                    // só pacote
        public boolean ativo = true;
        public final Map<Item, Integer> componentes = new LinkedHashMap<>(); // só pacote

        public Item(String nome, Tipo tipo, double preco, int estoque) {
            this.nome = nome;
            this.tipo = tipo;
            this.preco = preco;
            this.estoque = estoque;
        }

        /** Unidades que podem ser alugadas: total menos as indisponíveis. */
        public int estoqueUtil() { return Math.max(0, estoque - indisponivel); }

        /** Quantas unidades do produto informado este item consome (1 produto = ele mesmo). */
        public int consumo(Item produto) {
            if (tipo == Tipo.PRODUTO) return this == produto ? 1 : 0;
            return componentes.getOrDefault(produto, 0);
        }

        /** Produtos físicos envolvidos (o próprio item, ou os componentes do pacote). */
        public List<Item> produtos() {
            List<Item> l = new ArrayList<>();
            if (tipo == Tipo.PRODUTO) l.add(this);
            else l.addAll(componentes.keySet());
            return l;
        }

        /** Texto curto com o que vem no pacote. */
        public String composicao() {
            if (tipo == Tipo.PRODUTO) return nome;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Item, Integer> e : componentes.entrySet()) {
                if (sb.length() > 0) sb.append(" + ");
                if (e.getValue() > 1) sb.append(e.getValue()).append("× ");
                sb.append(e.getKey().nome);
            }
            return sb.toString();
        }

        @Override
        public String toString() { return nome; }
    }

    /** Uma linha do orçamento: item, quantidade e desconto manual (em %). */
    public static class Linha {
        public final Item item;
        public int qtd;
        public double desconto;              // 0 a 100

        public Linha(Item item, int qtd) {
            this.item = item;
            this.qtd = qtd;
        }

        public double bruto() { return item.preco * qtd; }

        public double subtotal() { return bruto() * (1 - desconto / 100.0); }
    }

    public static final List<Item> ITENS = new ArrayList<>();          // produtos e pacotes
    public static final List<Categoria> CATEGORIAS = new ArrayList<>();
    public static final List<Tema> TEMAS = new ArrayList<>();
    public static final List<Servico> SERVICOS = new ArrayList<>();

    static {
        Categoria vestidos = categoria("Vestidos", "Vestidos de personagens");
        Categoria fantasias = categoria("Fantasias", "Fantasias infantis e adultas");
        Categoria paineis = categoria("Painéis", "Painéis e fundos decorativos");
        Categoria mesas = categoria("Mesas", "Mesas de bolo e de doces");
        Categoria arcos = categoria("Arcos e balões", "Arcos desconstruídos e balões");
        Categoria moveis = categoria("Móveis de luxo", "Tronos e poltronas");
        Categoria tapetes = categoria("Tapetes", "Tapetes e passadeiras");
        Categoria cadeiras = categoria("Cadeiras", "Cadeiras para convidados");
        Categoria texteis = categoria("Têxteis", "Toalhas e cortinas");

        Tema princesas = tema("Princesas", "Contos de fadas e princesas");
        Tema realeza = tema("Realeza", "Festa real, castelos e coroas");
        tema("Fundo do Mar", "Sereias e vida marinha");
        Tema doces = tema("Mesa de doces", "Mesas completas para bolo e doces");

        servico("Entrega", "Entrega dos itens no local do evento", 80);
        servico("Montagem", "Montagem da decoração no local", 150);
        servico("Balonismo", "Arco de balões montado por profissional", 200);

        Item ariel    = produto("Vestido Ariel", "Azul e verde", vestidos, 180, 3);
        Item cinder   = produto("Vestido Cinderela", "Azul claro", vestidos, 180, 3);
        Item principe = produto("Fantasia Príncipe", "Azul e dourado", fantasias, 150, 2);
        Item painel   = produto("Painel redondo", "Branco", paineis, 120, 3);
        Item mesaBolo = produto("Mesa do bolo provençal", "Madeira branca", mesas, 220, 2);
        Item arco     = produto("Arco de balões", "Rosa e dourado", arcos, 250, 2);
        Item trono    = produto("Trono real", "Dourado", moveis, 300, 1);
        Item tapete   = produto("Tapete vermelho", "Vermelho", tapetes, 90, 2);
        Item cadeira  = produto("Cadeira Tiffany", "Transparente", cadeiras, 8, 40);
        Item toalha   = produto("Toalha de mesa", "Branca", texteis, 25, 20);

        pacote("Pacote Princesa", princesas, 460, ariel, 1, painel, 1, mesaBolo, 1);
        pacote("Pacote Festa Real", realeza, 720, trono, 1, arco, 1, painel, 1, tapete, 1, cadeira, 10);
        pacote("Pacote Mesa Completa", doces, 380, mesaBolo, 1, toalha, 2, cadeira, 20);
        // cinder e principe ficam só como produtos avulsos
        assert cinder != null && principe != null;
    }

    private static Categoria categoria(String nome, String desc) {
        Categoria c = new Categoria(nome, desc);
        CATEGORIAS.add(c);
        return c;
    }

    private static Tema tema(String nome, String desc) {
        Tema t = new Tema(nome, desc);
        TEMAS.add(t);
        return t;
    }

    private static void servico(String nome, String desc, double valor) {
        SERVICOS.add(new Servico(nome, desc, valor));
    }

    private static Item produto(String nome, String cor, Categoria cat, double preco, int estoque) {
        Item i = new Item(nome, Tipo.PRODUTO, preco, estoque);
        i.cor = cor;
        i.categoria = cat;
        ITENS.add(i);
        return i;
    }

    private static Item pacote(String nome, Tema tema, double preco, Object... componentes) {
        Item i = new Item(nome, Tipo.PACOTE, preco, 0);
        i.tema = tema;
        for (int k = 0; k < componentes.length; k += 2) {
            i.componentes.put((Item) componentes[k], (Integer) componentes[k + 1]);
        }
        ITENS.add(i);
        return i;
    }

    public static Item buscar(String nome) {
        for (Item i : ITENS) if (i.nome.equals(nome)) return i;
        throw new IllegalArgumentException("Item não existe no catálogo: " + nome);
    }

    public static Servico buscarServico(String nome) {
        for (Servico s : SERVICOS) if (s.nome.equals(nome)) return s;
        throw new IllegalArgumentException("Serviço não existe no catálogo: " + nome);
    }

    /** Todos os produtos (avulsos), ativos ou não. */
    public static List<Item> produtos() {
        List<Item> l = new ArrayList<>();
        for (Item i : ITENS) if (i.tipo == Tipo.PRODUTO) l.add(i);
        return l;
    }

    public static List<Item> pacotes() {
        List<Item> l = new ArrayList<>();
        for (Item i : ITENS) if (i.tipo == Tipo.PACOTE) l.add(i);
        return l;
    }

    /** Soma dos subtotais (já com desconto). */
    public static double total(List<Linha> linhas) {
        double t = 0;
        for (Linha l : linhas) t += l.subtotal();
        return t;
    }
}
