package model;

/** Serviço opcional (entrega, montagem, balonismo...) contratado numa locação, com valor individual (UC06). */
public class ServicoContratado {
    public final Catalogo.Servico servico;
    /** Valor cobrado nesta locação (pode diferir do valor de referência do catálogo). */
    public double valor;

    public ServicoContratado(Catalogo.Servico servico, double valor) {
        this.servico = servico;
        this.valor = valor;
    }
}
