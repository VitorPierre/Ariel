package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Cadastro de cliente, compartilhado pela TelaPrincipal (painel) e pela TelaClientes.
 * A situação (Em dia / Inadimplente) é calculada pela TelaPrincipal:
 * vale a marcação manual OU uma locação com devolução atrasada.
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
    public final List<Autorizada> autorizadas = new ArrayList<>();

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