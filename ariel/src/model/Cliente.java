package model;

import java.util.ArrayList;
import java.util.List;

/**
 * MODEL: cadastro de cliente. As regras (validação, CPF único, inadimplência, inativação) ficam no ClienteController.
 * Inadimplência é MANUAL (RF03): só a marcação feita pela proprietária ou por um funcionário vale.
 * UC03 FA03: sem vínculos o cliente pode ser excluído; com vínculos (orçamentos, locações, pagamentos) é
 * DESATIVADO (ativo = false), preservando o histórico.
 */
public class Cliente {

    public String cpf = "";
    public String nome = "";
    public String rg = "";
    public String instagram = "";
    public String whatsapp = "";
    public String endereco = "";
    public String observacoes = "";
    public boolean inadimplenteManual = false;
    public boolean ativo = true;                 // false = desativado (não aparece para novos orçamentos)
    public final List<Autorizada> autorizadas = new ArrayList<>();
    /** Id da linha em `Cliente` no banco. null = ainda não foi salvo no MySQL. Uso exclusivo do DAO. */
    public Integer idCliente;

    public Cliente() { }

    @Override
    public String toString() { return nome + "  ·  " + cpf; }

    public Cliente(String cpf, String nome, String whatsapp) {
        this.cpf = cpf;
        this.nome = nome;
        this.whatsapp = whatsapp;
    }

    /** Pessoa autorizada a retirar e/ou devolver os itens pelo cliente. */
    public static class Autorizada {
        public String nome;
        public String documento;
        public String contato;
        public String tipo;    // "Retira/Devolve", "Retira" ou "Devolve"

        public Autorizada(String nome, String documento, String contato, String tipo) {
            this.nome = nome;
            this.documento = documento;
            this.contato = contato;
            this.tipo = tipo;
        }
    }
}