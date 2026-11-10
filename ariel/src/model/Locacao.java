package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MODEL: uma reserva do painel. Nasce como orçamento e segue para locado e devolvido
 * (a etapa define a coluna). Guarda os dados e contas simples; as REGRAS de mudança de
 * etapa ficam no PainelController.
 */
public class Locacao {
    public final int id;
    public final Cliente cliente;
    public Etapa etapa;
    public LocalDate evento, validade, retirada, devolucao, devolvidoEm;
    public final List<Catalogo.Linha> itens;
    public final List<Pagamento> pagamentos = new ArrayList<>();
    public final List<Ocorrencia> ocorrencias = new ArrayList<>();
    public boolean arquivada = false;     // orçamento vencido sem entrada: sai do painel (regra 3)
    public String retiradoPor = "";
    public String devolvidoPor = "";

    // ----- campos da documentação v0.5 -----
    public LocalDate emissao;                                   // dia em que o orçamento foi emitido (validade = emissão + 30 dias)
    public String modalidade = "Retira";                        // Retira / Entrega / Entrega + montagem
    public String motivoArquivo = "";                           // "Expirado" ou "Cancelado" quando arquivada
    public String autorizadoInadimplentePor = "";               // responsável que autorizou a locação de cliente inadimplente (UC22)
    public boolean contratoPendente = false;                    // falha ao gerar o PDF (MSG016): tentar de novo
    public final List<ServicoContratado> servicos = new ArrayList<>();
    public final List<Cliente.Autorizada> autorizados = new ArrayList<>();   // terceiros autorizados a RETIRAR nesta locação (UC20)
    public final List<Documento> documentos = new ArrayList<>();

    public Locacao(int id, Cliente cliente, Etapa etapa, LocalDate evento, LocalDate validade,
                   LocalDate retirada, LocalDate devolucao, LocalDate devolvidoEm,
                   List<Catalogo.Linha> itens) {
        this.id = id;
        this.cliente = cliente;
        this.etapa = etapa;
        this.evento = evento;
        this.validade = validade;
        this.retirada = retirada;
        this.devolucao = devolucao;
        this.devolvidoEm = devolvidoEm;
        this.itens = itens;
        this.emissao = validade != null ? validade.minusDays(Regras.DIAS_VALIDADE_ORCAMENTO)
                : (retirada != null ? retirada.minusDays(5) : evento);
    }

    /** Texto usado nas caixas de seleção. */
    @Override
    public String toString() { return codigo() + " · " + cliente.nome + " · " + etapa.nome; }

    public String codigo() { return String.format("#%03d", id); }

    /** Soma dos serviços contratados (entrega, montagem...). */
    public double totalServicos() {
        double t = 0;
        for (ServicoContratado s : servicos) t += s.valor;
        return t;
    }

    /** Soma dos produtos/pacotes, já com os descontos manuais. */
    public double totalProdutos() { return Catalogo.total(itens); }

    /** Descontos manuais aplicados (em R$). */
    public double totalDescontos() {
        double t = 0;
        for (Catalogo.Linha l : itens) t += l.bruto() - l.subtotal();
        return t;
    }

    /** Valor da locação (sem multas): produtos com desconto + serviços. É sobre ele que valem sinal e saldo. */
    public double totalItens() { return totalProdutos() + totalServicos(); }

    /** Multas e indenizações lançadas na devolução. */
    public double totalOcorrencias() {
        double t = 0;
        for (Ocorrencia o : ocorrencias) t += o.valor;
        return t;
    }

    /** Valor total = itens + ocorrências. */
    public double total() { return totalItens() + totalOcorrencias(); }

    private double pago(boolean cobrancas) {
        double t = 0;
        for (Pagamento p : pagamentos) if ((p.tipo() == Pagamento.Tipo.COBRANCA) == cobrancas) t += p.valor();
        return t;
    }

    /** Entrada + saldo pagos (parte dos itens). */
    public double pagoItens() { return pago(false); }

    /** Multas e indenizações pagas. */
    public double pagoCobrancas() { return pago(true); }

    public double pago() { return pagoItens() + pagoCobrancas(); }

    public double saldoItens() { return Math.max(0, totalItens() - pagoItens()); }

    public double saldoCobrancas() { return Math.max(0, totalOcorrencias() - pagoCobrancas()); }

    public double saldo() { return saldoItens() + saldoCobrancas(); }

    public boolean temSaldo() { return saldo() > 0.005; }

    /** A entrada paga é o que reserva a data (regra 4 do minimundo). */
    public boolean temEntrada() { return pagoItens() > 0.005; }

    public boolean temOcorrencia() { return !ocorrencias.isEmpty(); }

    /** Finalizada = devolvida, conferida e sem valores pendentes (regra 11). */
    public boolean finalizada() { return etapa == Etapa.DEVOLVIDO && !temSaldo(); }
}
