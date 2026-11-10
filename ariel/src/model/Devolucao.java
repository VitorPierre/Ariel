package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dados preenchidos na tela de Devolução (conferência POR PRODUTO, UC16/RNF006).
 * Pacotes são desmembrados nos produtos que os compõem: cada produto é conferido com sua quantidade.
 * A tela só preenche este objeto; o PainelController valida e aplica as regras.
 */
public class Devolucao {

    public enum Situacao {
        OK("Devolvido OK"), DANIFICADO("Danificado"), PERDIDO("Perdido");

        private final String nome;
        Situacao(String nome) { this.nome = nome; }
        @Override public String toString() { return nome; }
    }

    /** Uma linha da conferência: o produto, quantas unidades saíram, como voltaram e quanto cobrar (manual). */
    public static class ItemConferido {
        public final Catalogo.Item produto;
        public final int qtd;                       // unidades que saíram na locação
        public Situacao situacao = Situacao.OK;
        public int qtdAfetada = 0;                  // quantas das unidades estão danificadas/perdidas
        public double valorCobrado = 0;             // digitado pela proprietária (0 = sem cobrança)
        public boolean indisponibilizar = false;    // dano: tira as unidades do estoque até serem reavaliadas
        public String observacao = "";              // descrição livre da ocorrência (ex.: "perna da cadeira quebrou")

        public ItemConferido(Catalogo.Item produto, int qtd) {
            this.produto = produto;
            this.qtd = qtd;
        }
    }

    public final List<ItemConferido> itens = new ArrayList<>();
    /** Qualquer pessoa identificada pode devolver (RN27/RN28): basta informar o nome. */
    public String devolvidoPor = "";
    /** Multa de atraso DIGITADA (o sistema não calcula nem sugere; 0 = sem cobrança). */
    public double multa = 0;

    public Devolucao(Locacao locacao) {
        Map<Catalogo.Item, Integer> soma = new LinkedHashMap<>();
        for (Catalogo.Linha l : locacao.itens)
            for (Catalogo.Item p : l.item.produtos()) soma.merge(p, l.qtd * l.item.consumo(p), Integer::sum);
        for (Map.Entry<Catalogo.Item, Integer> e : soma.entrySet()) itens.add(new ItemConferido(e.getKey(), e.getValue()));
    }
}
