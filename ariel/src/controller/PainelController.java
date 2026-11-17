package controller;

import model.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * CONTROLLER do painel: guarda clientes e locações e aplica TODAS as regras de negócio
 * (quem pode retirar, quando a data fica reservada, multa de atraso, pagamentos...).
 *
 * As telas (view) não decidem nada: perguntam aqui. Quando uma regra é violada o controller
 * lança RegraNegocioException e a tela só mostra a mensagem.
 * Quando algo muda, chamamos notificar() e as telas se redesenham (ouvintes).
 */
public class PainelController {

    public final List<Cliente> clientes = new ArrayList<>();
    public final List<Locacao> locacoes = new ArrayList<>();
    private final List<Runnable> ouvintes = new ArrayList<>();
    /** Geração de contrato e recibo em PDF (UC13). */
    public final DocumentoController documentos = new DocumentoController();
    private LocalDate hoje = agora();
    private int proximoId = 1;

    public PainelController() {
        popularExemplo();
        arquivarVencidos();
    }

    // ================= Data e ouvintes =================

    /** Data de hoje no fuso horário do computador. */
    public static LocalDate agora() { return LocalDate.now(ZoneId.systemDefault()); }

    public LocalDate hoje() { return hoje; }

    /** Número que o próximo orçamento vai receber (mostrado na tela Novo orçamento). */
    public int proximoNumero() { return proximoId; }

    /** Retorna true se o dia virou (e recalcula o que depende da data). */
    public boolean verificarVirada() {
        LocalDate a = agora();
        if (a.equals(hoje)) return false;
        hoje = a;
        arquivarVencidos();
        return true;
    }

    public long diasAte(LocalDate d) { return ChronoUnit.DAYS.between(hoje, d); }

    public void addOuvinte(Runnable r) { ouvintes.add(r); }

    /** Avisa as telas que os dados mudaram. */
    public void notificar() {
        arquivarVencidos();
        for (Runnable r : new ArrayList<>(ouvintes)) r.run();
    }

    /** RF18: orçamento sem entrada que passou dos 30 dias EXPIRA e é arquivado (sai do painel, fica no histórico). */
    private void arquivarVencidos() {
        for (Locacao l : locacoes) {
            if (l.etapa == Etapa.ORCAMENTO && !l.temEntrada() && l.validade != null && l.validade.isBefore(hoje))
            { l.arquivada = true; l.motivoArquivo = "Expirado"; }
        }
    }

    /** Locações que aparecem no painel (sem as arquivadas). */
    public List<Locacao> doPainel(Etapa etapa) {
        List<Locacao> l = new ArrayList<>();
        for (Locacao x : locacoes) if (!x.arquivada && !x.ocultaNoPainel && x.etapa == etapa) l.add(x);
        return l;
    }

    // ================= Consultas sobre clientes =================

    /**
     * Inadimplência é uma situação DEFINIDA MANUALMENTE (RF03): o sistema só alerta; quem decide
     * se o cliente está inadimplente é a proprietária ou um funcionário (o sistema não substitui essa decisão).
     */
    public boolean inadimplente(Cliente c) {
        return c.inadimplenteManual;
    }

    /** Clientes ativos (os que podem receber novos orçamentos). Inativos ficam só no histórico. */
    public List<Cliente> clientesAtivos() {
        List<Cliente> r = new ArrayList<>();
        for (Cliente c : clientes) if (c.ativo) r.add(c);
        return r;
    }

    /** Há orçamento ou locação ainda aberto para o cliente? */
    public boolean temReservaEmAberto(Cliente c) {
        for (Locacao k : locacoes) if (k.cliente == c && !k.arquivada && k.etapa != Etapa.DEVOLVIDO) return true;
        return false;
    }

    /** Histórico do cliente, do evento mais recente para o mais antigo. */
    public List<Locacao> historicoDe(Cliente c) {
        List<Locacao> l = new ArrayList<>();
        for (Locacao k : locacoes) if (k.cliente == c) l.add(k);
        l.sort((a, b) -> b.evento.compareTo(a.evento));
        return l;
    }

    /**
     * Quem pode RETIRAR (UC03/UC05): o titular, os terceiros autorizados PARA ESTA LOCAÇÃO e as pessoas
     * autorizadas no cadastro do cliente (tipo "Retira"). A DEVOLUÇÃO é livre: qualquer pessoa identificada.
     */
    public List<String> pessoasParaRetirar(Locacao l) {
        List<String> nomes = new ArrayList<>();
        nomes.add(l.cliente.nome + " (cliente)");
        for (Cliente.Autorizada a : l.autorizados) nomes.add(a.nome + " (autorizado)");
        for (Cliente.Autorizada a : l.cliente.autorizadas) {
            String n = a.nome + " (autorizado)";
            if (a.tipo.contains("Retira") && !nomes.contains(n)) nomes.add(n);
        }
        return nomes;
    }

    private void exigirQuemRetira(Locacao l, String quem) {
        if (quem == null || !pessoasParaRetirar(l).contains(quem))
            throw new RegraNegocioException("Só o titular ou uma pessoa previamente autorizada pode retirar os itens.\n"
                    + "Use \"Autorizar terceiro\" na locação para cadastrar quem vai retirar.");
    }

    /** Registra uma pessoa autorizada pelo titular a retirar os itens DESTA locação (pessoa autorizada, UC03/UC05). */
    public void autorizarTerceiro(Locacao l, String nome, String documento, String contato) {
        if (l.arquivada || l.etapa != Etapa.ORCAMENTO || !l.temEntrada())
            throw new RegraNegocioException("A autorização vale para a retirada: use em uma locação confirmada que ainda não foi retirada.");
        if (nome == null || nome.isBlank() || documento == null || documento.isBlank())
            throw new RegraNegocioException(Mensagens.MSG002 + "\n(nome e documento do terceiro)");
        for (Cliente.Autorizada a : l.autorizados)
            if (a.nome.equalsIgnoreCase(nome.trim())) throw new RegraNegocioException(Mensagens.MSG003);
        l.autorizados.add(new Cliente.Autorizada(nome.trim(), documento.trim(), contato == null ? "" : contato.trim(), "Retira"));
        notificar();
    }

    // ================= Estoque e agenda (UC15) =================

    /** Unidades do produto comprometidas no período (orçamentos com entrada paga e locações em aberto). */
    public int comprometido(Catalogo.Item produto, LocalDate ini, LocalDate fim) {
        return comprometido(produto, ini, fim, null);
    }

    /** Igual ao anterior, mas ignora uma locação (usada para conferir a própria reserva). */
    public int comprometido(Catalogo.Item produto, LocalDate ini, LocalDate fim, Locacao ignorar) {
        int soma = 0;
        for (Locacao k : locacoes) {
            if (k == ignorar || k.arquivada || k.etapa == Etapa.DEVOLVIDO) continue;
            if (k.etapa == Etapa.ORCAMENTO && !k.temEntrada()) continue;      // sem entrada não reserva
            if (k.retirada == null || k.devolucao == null) continue;
            LocalDate fimK = (k.etapa == Etapa.LOCADO && k.devolucao.isBefore(hoje)) ? hoje : k.devolucao; // atrasado ainda está fora
            if (k.retirada.isAfter(fim) || fimK.isBefore(ini)) continue;
            int usado = 0;
            for (Catalogo.Linha l : k.itens) usado += l.qtd * l.item.consumo(produto);
            if (k.etapa == Etapa.LOCADO) usado -= k.resolvidos.getOrDefault(produto, 0);   // devolução parcial já liberou
            soma += Math.max(0, usado);
        }
        return soma;
    }

    /** Unidades do produto ainda livres no período (estoque útil menos o que já está reservado). */
    public int livre(Catalogo.Item produto, LocalDate ini, LocalDate fim, Locacao ignorar) {
        return produto.estoqueUtil() - comprometido(produto, ini, fim, ignorar);
    }

    /** UC15: nome do primeiro produto em falta para a locação, ou null se está tudo disponível. */
    private String itemEmFalta(Locacao l) { return itemEmFalta(l, l.retirada); }

    private String itemEmFalta(Locacao l, LocalDate ini) {
        Catalogo.Item p = produtoEmFalta(l, ini);
        return p == null ? null : p.nome;
    }

    /** Primeiro produto sem estoque suficiente no período (null = tudo disponível). */
    private Catalogo.Item produtoEmFalta(Locacao l, LocalDate ini) {
        for (Catalogo.Linha linha : l.itens) {
            for (Catalogo.Item p : linha.item.produtos()) {
                int usado = 0;
                for (Catalogo.Linha o : l.itens) usado += o.qtd * o.item.consumo(p);
                if (usado + comprometido(p, ini, l.devolucao, l) > p.estoqueUtil()) return p;
            }
        }
        return null;
    }

    /** Reserva confirmada (ainda não retirada) que ficou sem estoque? Devolve o produto em falta (ou null). */
    public Catalogo.Item semEstoque(Locacao l) {
        if (l.arquivada || l.etapa != Etapa.ORCAMENTO || !l.temEntrada() || l.retirada == null || l.devolucao == null) return null;
        return produtoEmFalta(l, l.retirada);
    }

    /** Uma reserva confirmada que fica sem estoque por causa de uma devolução (dano ou perda). */
    public record ReservaAfetada(Locacao reserva, Catalogo.Item produto) {
        @Override public String toString() {
            return reserva.codigo() + " · " + reserva.cliente.nome + " (retirada "
                    + java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").format(reserva.retirada)
                    + "): fica sem estoque de \"" + produto.nome + "\"";
        }
    }

    /**
     * Prévia da devolução: quais reservas CONFIRMADAS passariam a ficar sem estoque se esta conferência
     * fosse registrada (perda reduz o estoque; dano com "Indisp.?" tira unidades do uso).
     * Não altera nada: aplica as mudanças de forma temporária, confere e desfaz.
     * (Mesmo princípio do UC08: sinalizar reservas futuras afetadas para substituição ou ajuste.)
     */
    public List<ReservaAfetada> reservasAfetadas(Locacao l, Devolucao d) {
        java.util.Map<Catalogo.Item, Integer> perde = new java.util.LinkedHashMap<>();
        for (Devolucao.ItemConferido c : d.itens) {
            if (c.qtdAfetada <= 0) continue;
            if (c.situacao == Devolucao.Situacao.PERDIDO
                    || (c.situacao == Devolucao.Situacao.DANIFICADO && c.indisponibilizar))
                perde.merge(c.produto, Math.min(c.qtdAfetada, c.qtd), Integer::sum);
        }
        List<ReservaAfetada> r = new ArrayList<>();
        if (perde.isEmpty()) return r;

        // reservas que já estavam sem estoque antes não entram (o aviso é sobre o que ESTA devolução causa)
        List<Locacao> jaEmFalta = new ArrayList<>();
        for (Locacao k : locacoes) if (semEstoque(k) != null) jaEmFalta.add(k);

        java.util.Map<Catalogo.Item, int[]> antes = new java.util.LinkedHashMap<>();
        java.util.Map<Catalogo.Item, Integer> resolvidosAntes = new java.util.LinkedHashMap<>(l.resolvidos);
        try {
            // aplica temporariamente: unidades conferidas deixam de estar com o cliente e o estoque útil diminui
            for (Devolucao.ItemConferido c : d.itens) {
                int resolvidas = c.situacao == Devolucao.Situacao.NAO_DEVOLVIDO ? c.qtd - c.qtdAfetada : c.qtd;
                if (resolvidas > 0) l.resolvidos.merge(c.produto, resolvidas, Integer::sum);
            }
            for (java.util.Map.Entry<Catalogo.Item, Integer> e : perde.entrySet()) {
                Catalogo.Item p = e.getKey();
                antes.put(p, new int[]{p.estoque, p.indisponivel});
                p.indisponivel = Math.min(p.estoque, p.indisponivel + e.getValue());
            }
            for (Locacao k : locacoes) {
                if (k == l || jaEmFalta.contains(k)) continue;
                Catalogo.Item falta = semEstoque(k);
                if (falta != null && perde.containsKey(falta)) r.add(new ReservaAfetada(k, falta));
            }
        } finally {                                                   // desfaz tudo
            for (java.util.Map.Entry<Catalogo.Item, int[]> e : antes.entrySet()) {
                e.getKey().estoque = e.getValue()[0];
                e.getKey().indisponivel = e.getValue()[1];
            }
            l.resolvidos.clear();
            l.resolvidos.putAll(resolvidosAntes);
        }
        return r;
    }

    /**
     * Opção B (decisão da equipe): ao registrar o SINAL de uma reserva futura, avisa (sem bloquear) se algum
     * produto dela está ATRASADO com outro cliente. A disponibilidade conta que ele volte a tempo.
     * A retirada continua protegida: item que não voltou conta como ocupado no dia de hoje.
     */
    public List<String> avisosDeAtraso(List<Catalogo.Linha> itens, LocalDate retirada, Locacao ignorar) {
        List<String> r = new ArrayList<>();
        if (itens == null || retirada == null || !retirada.isAfter(hoje)) return r;   // hoje/antes: o cálculo já conta o atrasado
        java.util.Set<Catalogo.Item> produtos = new java.util.LinkedHashSet<>();
        for (Catalogo.Linha li : itens) produtos.addAll(li.item.produtos());
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu");
        for (Locacao k : locacoes) {
            if (k == ignorar || k.etapa != Etapa.LOCADO || k.devolucao == null || !k.devolucao.isBefore(hoje)) continue;
            for (Catalogo.Item p : produtos) {
                int fora = k.pendentesDe(p);
                if (fora > 0) r.add(fora + "× " + p.nome + " está(ão) atrasado(s) com " + k.codigo() + " · " + k.cliente.nome
                        + " (devolução prevista " + f.format(k.devolucao) + ")");
            }
        }
        return r;
    }

    // ================= Orçamentos (UC04) =================

    /** Confere o que é comum a criar/alterar um orçamento: cliente, itens e datas. */
    private void validarOrcamento(Cliente cliente, LocalDate evento, LocalDate retirada, LocalDate devolucao,
                                  List<Catalogo.Linha> itens) {
        if (cliente == null || itens.isEmpty())
            throw new RegraNegocioException(Mensagens.MSG002 + "\n(escolha o cliente e ao menos um produto ou pacote)");
        if (!cliente.ativo)
            throw new RegraNegocioException(cliente.nome + " está com o cadastro inativo. Reative em Clientes para fazer um orçamento.");
        if (evento == null || retirada == null || devolucao == null
                || retirada.isBefore(hoje) || evento.isBefore(retirada) || devolucao.isBefore(evento))
            throw new RegraNegocioException(Mensagens.MSG011
                    + "\n(a ordem é retirada ≤ evento ≤ devolução, e a retirada não pode ser antes de hoje)");
        for (Catalogo.Linha l : itens) {
            if (l.qtd <= 0 || l.desconto < 0 || l.desconto > 100)
                throw new RegraNegocioException(Mensagens.MSG011 + "\n(quantidade e desconto de \"" + l.item.nome + "\")");
            if (!l.item.ativo)
                throw new RegraNegocioException("\"" + l.item.nome + "\" está inativo no catálogo e não pode ser orçado.");
        }
    }

    /**
     * Cria um orçamento (validade fixa de 30 dias, RF18). O orçamento NÃO reserva estoque.
     * Se "entrada" for maior que zero é o fluxo "Registrar entrada e confirmar reserva": o sinal variável é pago,
     * a disponibilidade é revalidada (UC15) e o contrato é gerado (UC13). Se algo falhar, nada é criado.
     * autorizadoPor: só preenchido quando o cliente é inadimplente e o usuário autorizou a locação (decisão da equipe: autorização registrada).
     */
    public Locacao criarOrcamento(Cliente cliente, LocalDate evento, LocalDate retirada, LocalDate devolucao,
                                  String modalidade, List<Catalogo.Linha> itens, List<ServicoContratado> servicos,
                                  double entrada, Pagamento.Forma forma, Usuario operador, Usuario autorizadoPor) {
        validarOrcamento(cliente, evento, retirada, devolucao, itens);
        if (entrada > 0.005) exigirAdmin(operador, "registrar o pagamento do sinal");
        Locacao l = new Locacao(proximoId, cliente, Etapa.ORCAMENTO, evento,
                hoje.plusDays(Regras.DIAS_VALIDADE_ORCAMENTO), retirada, devolucao, null, itens);
        l.emissao = hoje;
        l.modalidade = modalidade == null ? "Retira" : modalidade;
        l.servicos.addAll(servicos);
        boolean confirma = entrada > 0.005;
        if (confirma) validarSinal(l, entrada, forma, autorizadoPor);     // falhou? nada foi criado ainda
        proximoId++;
        locacoes.add(l);
        if (confirma) {
            l.pagamentos.add(new Pagamento(hoje, entrada, forma, Pagamento.Tipo.ENTRADA));
            gerarContratoAoConfirmar(l);
        }
        notificar();
        return l;
    }

    /**
     * UC04 FA01: altera um orçamento em negociação (SEM renovar a validade).
     * UC05 FA02: altera uma reserva CONFIRMADA (sinal pago, ainda não retirada): revalida a disponibilidade
     * desconsiderando a própria reserva; com conflito, exibe MSG010 e MANTÉM a reserva anterior; sem conflito,
     * grava e gera nova versão do contrato (UC13).
     */
    public void atualizarOrcamento(Locacao l, LocalDate evento, LocalDate retirada, LocalDate devolucao, String modalidade,
                                   List<Catalogo.Linha> itens, List<ServicoContratado> servicos) {
        if (l.arquivada) throw new RegraNegocioException(Mensagens.MSG017);
        if (l.etapa != Etapa.ORCAMENTO)
            throw new RegraNegocioException("Os itens já foram retirados: a reserva não pode mais ser alterada.");
        boolean confirmada = l.temEntrada();
        if (!confirmada && l.validade != null && l.validade.isBefore(hoje)) throw new RegraNegocioException(Mensagens.MSG017);
        validarOrcamento(l.cliente, evento, retirada, devolucao, itens);

        // guarda a versão anterior para poder voltar se a nova tiver conflito
        LocalDate ev0 = l.evento, ret0 = l.retirada, dev0 = l.devolucao;
        String mod0 = l.modalidade;
        List<Catalogo.Linha> itens0 = new ArrayList<>(l.itens);
        List<ServicoContratado> serv0 = new ArrayList<>(l.servicos);

        l.evento = evento;
        l.retirada = retirada;
        l.devolucao = devolucao;
        l.modalidade = modalidade == null ? "Retira" : modalidade;
        l.itens.clear();
        l.itens.addAll(itens);
        l.servicos.clear();
        l.servicos.addAll(servicos);

        if (confirmada) {
            String erro = null;
            String falta = itemEmFalta(l);
            if (falta != null) erro = Mensagens.MSG010 + "\n(sem estoque de \"" + falta + "\" no período)\nA reserva anterior foi mantida.";
            else if (l.pagoItens() > l.totalItens() + 0.005)
                erro = "O novo total (" + moeda(l.totalItens()) + ") fica menor que o valor já pago (" + moeda(l.pagoItens())
                        + "). A reserva anterior foi mantida.";
            if (erro != null) {                                         // volta tudo como estava
                l.evento = ev0; l.retirada = ret0; l.devolucao = dev0; l.modalidade = mod0;
                l.itens.clear(); l.itens.addAll(itens0);
                l.servicos.clear(); l.servicos.addAll(serv0);
                throw new RegraNegocioException(erro);
            }
            gerarContratoAoConfirmar(l);                                // nova versão do contrato (UC13 FA02)
        }
        notificar();
    }

    /** UC04 FA03: cancela (arquiva) um orçamento em negociação, mantendo o histórico. */
    public void arquivar(Locacao l) {
        if (l.arquivada || l.etapa != Etapa.ORCAMENTO) throw new RegraNegocioException(Mensagens.MSG017);
        if (l.temEntrada())
            throw new RegraNegocioException("Há sinal pago (não reembolsável): a reserva está confirmada.\n"
                    + "Se for cancelar mesmo assim, use \"Excluir do painel\".");
        l.arquivada = true;
        l.motivoArquivo = "Cancelado";
        notificar();
    }

    /**
     * Confirma a reserva: valida cadastro do cliente (MSG013), inadimplência (decisão da equipe: autorização registrada), valor do sinal
     * e disponibilidade (UC15). Chamado antes de gravar o sinal.
     */
    private void validarSinal(Locacao l, double valor, Pagamento.Forma forma, Usuario autorizadoPor) {
        if (l.arquivada || (l.validade != null && l.validade.isBefore(hoje))) throw new RegraNegocioException(Mensagens.MSG017);
        if (forma == null) throw new RegraNegocioException("Escolha a forma de pagamento.");
        if (l.cliente.cpf.trim().isEmpty() || l.cliente.endereco.trim().isEmpty())
            throw new RegraNegocioException(Mensagens.MSG013);
        boolean precisaAutorizacao = inadimplente(l.cliente) && l.autorizadoInadimplentePor.isEmpty();
        if (precisaAutorizacao && autorizadoPor == null) throw new ExigeAutorizacaoException(Mensagens.MSG012);
        double minimo = Math.ceil(l.totalItens() * Regras.SINAL_MINIMO * 100 - 1e-9) / 100.0;
        if (valor + 0.005 < minimo)
            throw new RegraNegocioException(Mensagens.MSG014 + "\nO sinal informado (" + moeda(valor) + ") é "
                    + percentual(valor, l.totalItens()) + " do total; o mínimo é " + moeda(minimo) + " (10%).");
        if (valor > l.totalItens() + 0.005)
            throw new RegraNegocioException("O sinal não pode passar do total (" + moeda(l.totalItens()) + ").");
        String falta = itemEmFalta(l);
        if (falta != null)
            throw new RegraNegocioException(Mensagens.MSG010 + "\n(sem estoque de \"" + falta + "\" no período)");
        // só registra quem autorizou (decisão da equipe: autorização registrada) depois que TODAS as validações passaram
        if (precisaAutorizacao) l.autorizadoInadimplentePor = autorizadoPor.nome();
    }

    /** Registra o responsável que autorizou a locação de cliente inadimplente (decisão da equipe: autorização registrada). Não quita dívida nenhuma. */
    public void autorizarInadimplente(Locacao l, Usuario responsavel) {
        l.autorizadoInadimplentePor = responsavel.nome();
        notificar();
    }

    // ================= Serviços e documentos =================

    /** UC06 FA04: associa um serviço (entrega, montagem...) à reserva, com valor individual. */
    public void adicionarServico(Locacao l, Catalogo.Servico servico, double valor) {
        if (l.arquivada || l.etapa == Etapa.DEVOLVIDO) throw new RegraNegocioException(Mensagens.MSG017);
        if (servico == null || !servico.ativo) throw new RegraNegocioException("Escolha um serviço ativo.");
        if (valor < 0) throw new RegraNegocioException(Mensagens.MSG011);
        l.servicos.add(new ServicoContratado(servico, valor));
        if (!DocumentoController.versoes(l, "Contrato").isEmpty()) gerarContratoAoConfirmar(l);   // mudou: nova versão (UC13)
        notificar();
    }

    /** Gera o contrato logo após o sinal. Se falhar (MSG016) a reserva continua e fica "pendente de documento". */
    private void gerarContratoAoConfirmar(Locacao l) {
        try {
            documentos.contrato(l, hoje);
            l.contratoPendente = false;
        } catch (java.io.IOException | RuntimeException e) {
            l.contratoPendente = true;
        }
    }

    /** UC13: contrato em PDF da locação (nova versão só se algo mudou). Acesso direto: só o Administrador. */
    public Documento gerarContrato(Locacao l, Usuario operador) {
        exigirAdmin(operador, "gerar documentos");
        if (l.arquivada || (l.etapa == Etapa.ORCAMENTO && !l.temEntrada()))
            throw new RegraNegocioException("O contrato só é gerado depois da confirmação da locação (sinal registrado).");
        try {
            Documento d = documentos.contrato(l, hoje);
            l.contratoPendente = false;
            notificar();
            return d;
        } catch (java.io.IOException | RuntimeException e) {
            l.contratoPendente = true;
            throw new RegraNegocioException(Mensagens.MSG016);
        }
    }

    /** UC13: recibo em PDF dos pagamentos recebidos. Só o Administrador. */
    public Documento gerarRecibo(Locacao l, Usuario operador) {
        exigirAdmin(operador, "gerar documentos");
        if (l.pagamentos.isEmpty()) throw new RegraNegocioException("Ainda não há pagamento registrado para emitir recibo.");
        try {
            Documento d = documentos.recibo(l, hoje);
            notificar();
            return d;
        } catch (java.io.IOException | RuntimeException e) {
            throw new RegraNegocioException(Mensagens.MSG016);
        }
    }

    // ================= Avisos dos cartões =================

    /** Aviso do cartão, calculado a partir de hoje (muda sozinho com os dias). */
    public String aviso(Locacao l) {
        switch (l.etapa) {
            case ORCAMENTO: {
                if (l.temEntrada()) {
                    Catalogo.Item falta = semEstoque(l);
                    return falta != null ? "Sem estoque: " + falta.nome : "Entrada paga";
                }
                long d = diasAte(l.validade);
                if (d < 0) return "Vencido há " + dias(-d);
                if (d == 0) return "Vence hoje";
                if (d == 1) return "Vence amanhã";
                if (d <= Regras.DIAS_ALERTA_VENCIMENTO) return "Vence em " + dias(d);
                return "Aguardando entrada";
            }
            case LOCADO: {
                long d = diasAte(l.devolucao);
                if (d < 0) return "Atrasado há " + dias(-d);
                if (!l.resolvidos.isEmpty()) return "Devolução parcial";
                if (d == 0) return "Devolução hoje";
                if (l.temSaldo()) return "Saldo pendente";
                return "Quitado";
            }
            default:
                if (l.saldoCobrancas() > 0.005) return "Cobrança pendente";
                if (l.temSaldo()) return "Saldo pendente";
                return l.temOcorrencia() ? "Finalizada · com ocorrência" : "Finalizada · sem ocorrência";
        }
    }

    /** Avisos urgentes aparecem em preto com letras brancas. */
    public boolean urgente(Locacao l) {
        switch (l.etapa) {
            case ORCAMENTO: return l.temEntrada() ? semEstoque(l) != null : diasAte(l.validade) <= 1;
            case LOCADO:    return diasAte(l.devolucao) <= 0;
            default:        return l.temSaldo();
        }
    }

    private static String dias(long n) { return n + (Math.abs(n) == 1 ? " dia" : " dias"); }

    // ================= Mudança de etapa (arrastar o cartão) =================

    public enum Acao { RETIRAR, DEVOLVER }

    /**
     * Confere se o cartão pode ir para a etapa de destino e diz qual é o próximo passo.
     * Regras:
     *  - só anda para frente: Orçamento -> Locado -> Devolvido;
     *  - retirar exige sinal pago, SALDO TOTALMENTE QUITADO (UC07), cadastro completo e itens disponíveis (UC15);
     *  - devolver passa pela conferência por produto (tela Devoluções) e não exige pagamento.
     */
    public Acao validarMovimento(Locacao l, Etapa destino) {
        if (destino.ordinal() <= l.etapa.ordinal())
            throw new RegraNegocioException("Uma reserva só avança de etapa (Orçamento → Locado → Devolvido).\n"
                    + "Para desistir, use Excluir (o sinal não é devolvido).");
        if (destino == Etapa.DEVOLVIDO && l.etapa == Etapa.ORCAMENTO)
            throw new RegraNegocioException("Antes de devolver, os itens precisam ser retirados (coluna Locado).");
        if (destino == Etapa.LOCADO) { exigirRetiravel(l); return Acao.RETIRAR; }
        return Acao.DEVOLVER;
    }

    private void exigirRetiravel(Locacao l) {
        if (l.arquivada) throw new RegraNegocioException(Mensagens.MSG017);
        if (!l.temEntrada())
            throw new PendenciaException(PendenciaException.Onde.RESERVA,
                    Mensagens.MSG014 + "\nSem sinal a data e os itens não são reservados.");
        if (l.saldoItens() > 0.005)
            throw new PendenciaException(PendenciaException.Onde.RESERVA, "Saldo pendente de " + moeda(l.saldoItens()) + ".\n"
                    + "O saldo deve ser quitado integralmente até a retirada (use Quitar saldo).");
        if (l.cliente.endereco.trim().isEmpty() || l.cliente.cpf.trim().isEmpty())
            throw new PendenciaException(PendenciaException.Onde.CLIENTE, Mensagens.MSG013);
        // retirada antecipada (UC05 FA03): o período ampliado precisa estar livre
        Catalogo.Item falta = produtoEmFalta(l, hoje.isBefore(l.retirada) ? hoje : l.retirada);
        if (falta != null)
            throw new PendenciaException(PendenciaException.Onde.PRODUTO, falta,
                    Mensagens.MSG010 + "\n(sem estoque de \"" + falta.nome + "\")");
    }

    /** Registra a retirada/entrega: a locação passa a "Locado" com a data de hoje. Registra QUEM retirou (UC05 FA03). */
    public void registrarRetirada(Locacao l, String quem) {
        validarMovimento(l, Etapa.LOCADO);
        exigirQuemRetira(l, quem);
        l.etapa = Etapa.LOCADO;
        l.retirada = hoje;
        l.retiradoPor = quem;
        if (l.devolucao == null || l.devolucao.isBefore(hoje)) l.devolucao = hoje.plusDays(1);
        notificar();
    }

    // ================= Devolução e ocorrências =================

    /** Dias de atraso na devolução (0 se no prazo). É só um FATO: o sistema não calcula multa. */
    public long diasAtraso(Locacao l) {
        return Math.max(0, ChronoUnit.DAYS.between(l.devolucao, hoje));
    }

    /**
     * Registra a devolução (UC16) com a conferência por produto.
     *  - O titular ou qualquer pessoa identificada pode devolver: basta informar o nome.
     *  - Não exige pagamento: registrar a condição não equivale a quitação (pendências ficam registradas).
     *  - Atraso, dano e perda viram OCORRÊNCIAS (UC11) com valor DIGITADO (0 = sem cobrança).
     *    Por decisão da equipe, o sistema não calcula nem sugere multa.
     *  - Perda definitiva reduz o estoque total; dano só tira do estoque se a proprietária decidir (indisponibilizar).
     *  - Devolução parcial (FA02): itens "Ainda não voltou" continuam pendentes (não viram perda) e a locação
     *    continua em Locado até tudo ser conferido. O que voltou já fica livre para novas reservas.
     */
    public void registrarDevolucao(Locacao l, Devolucao d, Usuario quem) {
        if (l.etapa != Etapa.LOCADO) throw new RegraNegocioException("Só é possível devolver uma locação em andamento (Locado).");
        if (d.devolvidoPor == null || d.devolvidoPor.isBlank())
            throw new RegraNegocioException("Identifique quem está devolvendo os itens (nome da pessoa).");
        long atraso = diasAtraso(l);
        if (d.multa < 0 || (d.multa > 0.005 && atraso == 0))
            throw new RegraNegocioException(d.multa < 0 ? Mensagens.MSG011 : "Não há atraso: não há multa a registrar.");
        int conferidas = 0, pendentes = 0;
        for (Devolucao.ItemConferido c : d.itens) {
            if (c.situacao == Devolucao.Situacao.OK) {
                if (c.valorCobrado > 0.005 || c.qtdAfetada > 0)
                    throw new RegraNegocioException("\"" + c.produto.nome + "\" está OK: sem valor cobrado nem unidades afetadas.");
                conferidas += c.qtd;
                continue;
            }
            if (c.qtdAfetada < 1 || c.qtdAfetada > c.qtd)
                throw new RegraNegocioException("Informe quantas unidades de \"" + c.produto.nome + "\" ficaram com a situação \""
                        + c.situacao + "\" (de 1 a " + c.qtd + ").");
            if (c.situacao == Devolucao.Situacao.NAO_DEVOLVIDO) {
                if (c.valorCobrado > 0.005)
                    throw new RegraNegocioException("\"" + c.produto.nome + "\" ainda não voltou: não há cobrança. "
                            + "Se for perda confirmada, registre como \"Perdido\".");
                pendentes += c.qtdAfetada;
                conferidas += c.qtd - c.qtdAfetada;
                continue;
            }
            if (c.valorCobrado < 0) throw new RegraNegocioException(Mensagens.MSG011);
            conferidas += c.qtd;
        }
        if (conferidas == 0)
            throw new RegraNegocioException("Nenhum item voltou: não há devolução a registrar.");
        boolean parcial = pendentes > 0;

        String por = quem == null ? "" : quem.nome();
        if (atraso > 0 && (!parcial || d.multa > 0.005))              // o atraso é registrado no fechamento (ou quando há multa)
            l.ocorrencias.add(new Ocorrencia(Ocorrencia.Tipo.ATRASO, atraso + (atraso == 1 ? " dia" : " dias")
                    + " de atraso" + (d.multa > 0.005 ? "" : " (sem cobrança)"), d.multa, hoje, por));
        StringBuilder faltam = new StringBuilder();
        for (Devolucao.ItemConferido c : d.itens) {
            int resolvidas = c.situacao == Devolucao.Situacao.NAO_DEVOLVIDO ? c.qtd - c.qtdAfetada : c.qtd;
            if (resolvidas > 0) l.resolvidos.merge(c.produto, resolvidas, Integer::sum);
            if (c.situacao == Devolucao.Situacao.NAO_DEVOLVIDO) {
                if (faltam.length() > 0) faltam.append(", ");
                faltam.append(c.qtdAfetada).append("× ").append(c.produto.nome);
                continue;
            }
            if (c.situacao == Devolucao.Situacao.OK) continue;
            boolean perdido = c.situacao == Devolucao.Situacao.PERDIDO;
            l.ocorrencias.add(new Ocorrencia(perdido ? Ocorrencia.Tipo.PERDA : Ocorrencia.Tipo.DANO,
                    c.qtdAfetada + "× " + c.produto.nome + (c.qtdAfetada < c.qtd ? " (de " + c.qtd + ")" : "")
                            + (c.observacao == null || c.observacao.isBlank() ? "" : " — " + c.observacao.trim()),
                    c.valorCobrado, hoje, por));
            if (perdido) c.produto.estoque = Math.max(0, c.produto.estoque - c.qtdAfetada);    // perda definitiva
            else if (c.indisponibilizar) c.produto.indisponivel += c.qtdAfetada;               // dano: decisão manual
        }
        if (parcial) {                                                // UC16 FA02: só registra o que voltou
            l.devolucoesParciais.add(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").format(hoje)
                    + " · devolvido por " + d.devolvidoPor.trim() + " · ainda faltam: " + faltam);
            notificar();
            return;
        }
        l.etapa = Etapa.DEVOLVIDO;
        l.devolvidoEm = hoje;
        l.devolvidoPor = d.devolvidoPor.trim();
        notificar();
    }

    // ================= Pagamentos =================

    /** Texto do botão de pagamento do detalhe da reserva (null = nada a pagar). */
    public String rotuloPagamento(Locacao l) {
        if (l.arquivada) return null;
        switch (l.etapa) {
            case ORCAMENTO: return !l.temEntrada() ? "Registrar entrada" : (l.saldoItens() > 0.005 ? "Quitar saldo" : null);
            case LOCADO:    return l.saldoItens() > 0.005 ? "Quitar saldo" : null;
            default:        return l.saldo() > 0.005 ? "Quitar saldo pendente" : null;
        }
    }

    /** Valor que a tela sugere ao pagar (para o sinal, 20% do total: o protótipo mostra 20%). */
    public double valorSugerido(Locacao l) {
        if (l.etapa == Etapa.ORCAMENTO && !l.temEntrada())
            return Math.ceil(l.totalItens() * Regras.SINAL_PADRAO * 100 - 1e-9) / 100.0;
        return l.etapa == Etapa.DEVOLVIDO ? l.saldo() : l.saldoItens();
    }

    /**
     * Registra um pagamento (entrada, saldo ou multa/indenização conforme a etapa).
     *  - entrada (sinal): valor VARIÁVEL, qualquer valor de zero até o total; confirma a reserva e gera o contrato;
     *  - saldo: não passa do que falta (excesso seria reembolso, nunca crédito);
     *  - cobranças: multas e indenizações lançadas na devolução.
     * autorizadoPor: só se o cliente é inadimplente e o usuário autorizou a locação (decisão da equipe: autorização registrada).
     */
    public void pagar(Locacao l, double valor, Pagamento.Forma forma, Usuario operador, Usuario autorizadoPor) {
        exigirAdmin(operador, "registrar pagamentos");
        if (l.arquivada) throw new RegraNegocioException(Mensagens.MSG017);
        if (forma == null) throw new RegraNegocioException("Escolha a forma de pagamento.");
        if (valor <= 0.005) throw new RegraNegocioException("Informe um valor maior que zero.");
        Pagamento.Tipo tipo;
        double limite;
        if (l.etapa == Etapa.ORCAMENTO && !l.temEntrada()) {
            tipo = Pagamento.Tipo.ENTRADA;
            limite = l.totalItens();
            validarSinal(l, valor, forma, autorizadoPor);
        } else if (l.etapa == Etapa.DEVOLVIDO) {
            // devolvida: quita primeiro o que faltar dos itens e depois multas/indenizações (num pagamento só)
            limite = l.saldo();
            if (limite <= 0.005) throw new RegraNegocioException("Não há saldo pendente.");
            if (valor > limite + 0.005) throw new RegraNegocioException("O valor passa do que falta pagar (" + moeda(limite) + ").");
            double paraItens = Math.min(valor, l.saldoItens());
            if (paraItens > 0.005) l.pagamentos.add(new Pagamento(hoje, paraItens, forma, Pagamento.Tipo.SALDO));
            if (valor - paraItens > 0.005) l.pagamentos.add(new Pagamento(hoje, valor - paraItens, forma, Pagamento.Tipo.COBRANCA));
            notificar();
            return;
        } else {
            tipo = Pagamento.Tipo.SALDO;
            limite = l.saldoItens();
            if (limite <= 0.005) throw new RegraNegocioException("Esta locação já está quitada.");
        }
        if (valor > limite + 0.005)
            throw new RegraNegocioException("O valor passa do que falta pagar (" + moeda(limite) + ").");
        l.pagamentos.add(new Pagamento(hoje, valor, forma, tipo));
        if (tipo == Pagamento.Tipo.ENTRADA) gerarContratoAoConfirmar(l);      // RF13: contrato após o sinal
        notificar();
    }

    // ================= Excluir =================

    /**
     * Tira o cartão do painel SEM apagar nada (o histórico é preservado).
     *  - Orçamento: é CANCELADO (arquivado). Se tinha sinal, o sinal não é devolvido (UC05 FA04), continua no
     *    faturamento e os itens voltam a ficar livres.
     *  - Devolvida e quitada: só sai do painel; continua em Locações, no histórico do cliente e nos relatórios.
     */
    public void excluir(Locacao l) {
        if (l.arquivada || l.ocultaNoPainel) return;
        if (l.etapa == Etapa.LOCADO)
            throw new RegraNegocioException("Os itens estão com o cliente. Registre a devolução antes de excluir.");
        if (l.etapa == Etapa.DEVOLVIDO) {
            if (l.temSaldo())
                throw new RegraNegocioException("Há valores pendentes (" + moeda(l.saldo()) + "). Quite antes de excluir.");
            l.ocultaNoPainel = true;
        } else {
            l.arquivada = true;
            l.motivoArquivo = l.temEntrada() ? "Cancelado (sinal retido)" : "Cancelado";
        }
        notificar();
    }

    // ================= Resumo do painel =================

    /** {reservas na semana, pagamentos pendentes, orçamentos a vencer, devoluções previstas}. */
    public int[] resumo() {
        LocalDate fimSemana = hoje.plusDays(6);          // semana = hoje + 6 dias seguintes
        LocalDate limiteVencer = hoje.plusDays(7);
        int reservas = 0, pendentes = 0, vencer = 0, previstas = 0;
        for (Locacao c : doPainel(Etapa.ORCAMENTO)) {
            if (c.temEntrada()) {
                if (!c.evento.isBefore(hoje) && !c.evento.isAfter(fimSemana)) reservas++;
            } else if (!c.validade.isBefore(hoje)) {      // sem entrada e ainda dentro da validade
                pendentes++;
                if (!c.validade.isAfter(limiteVencer)) vencer++;
            }
        }
        for (Locacao c : doPainel(Etapa.LOCADO)) {
            if (!c.retirada.isAfter(fimSemana) && !c.devolucao.isBefore(hoje)) reservas++;
            if (c.temSaldo()) pendentes++;
            if (!c.devolucao.isAfter(fimSemana)) previstas++;
        }
        for (Locacao c : doPainel(Etapa.DEVOLVIDO)) if (c.temSaldo()) pendentes++;
        return new int[]{reservas, pendentes, vencer, previstas};
    }

    /**
     * UC07/UC13: só o Administrador registra pagamentos e gera documentos. O Funcionário pode montar e salvar
     * o orçamento; a confirmação fica pendente até o Administrador registrar o sinal (UC05 FA06).
     */
    private static void exigirAdmin(Usuario operador, String acao) {
        if (operador == null || !operador.ehAdmin())
            throw new RegraNegocioException("Acesso restrito: somente o Administrador pode " + acao + ".");
    }

    public static boolean podeRegistrarPagamento(Usuario u) { return u != null && u.ehAdmin(); }

    private static String percentual(double valor, double total) {
        return total <= 0 ? "0%" : String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f%%", valor / total * 100);
    }

    public static String moeda(double v) {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR")).format(v);
    }

    // ================= Dados de exemplo (todas as datas partem de HOJE) =================

    private Cliente novoCliente(String cpf, String nome, String whatsapp, String rg,
                                String instagram, String endereco, String obs) {
        Cliente c = new Cliente(cpf, nome, whatsapp);
        c.rg = rg;
        c.instagram = instagram;
        c.endereco = endereco;
        c.observacoes = obs;
        clientes.add(c);
        return c;
    }

    private Cliente cliente(String nome) {
        for (Cliente c : clientes) if (c.nome.equals(nome)) return c;
        throw new IllegalArgumentException("Cliente não cadastrado: " + nome);
    }

    private List<Catalogo.Linha> itens(Object... pares) {
        List<Catalogo.Linha> l = new ArrayList<>();
        for (int i = 0; i < pares.length; i += 2)
            l.add(new Catalogo.Linha(Catalogo.buscar((String) pares[i]), (Integer) pares[i + 1]));
        return l;
    }

    /** pago == null significa totalmente pago. */
    private Locacao exemplo(Etapa etapa, String cliente, LocalDate evento, LocalDate validade, LocalDate retirada,
                            LocalDate devolucao, LocalDate devolvidoEm, List<Catalogo.Linha> itens, Double pago) {
        Locacao l = new Locacao(proximoId++, cliente(cliente), etapa, evento, validade, retirada, devolucao, devolvidoEm, itens);
        double p = pago == null ? l.totalItens() : pago;
        if (p > 0) l.pagamentos.add(new Pagamento(hoje, p, Pagamento.Forma.PIX,
                pago == null ? Pagamento.Tipo.SALDO : Pagamento.Tipo.ENTRADA));
        if (etapa != Etapa.ORCAMENTO) l.retiradoPor = l.cliente.nome + " (cliente)";
        if (etapa == Etapa.DEVOLVIDO) l.devolvidoPor = l.cliente.nome + " (cliente)";
        locacoes.add(l);
        return l;
    }

    private void popularExemplo() {
        Cliente maria = novoCliente("529.982.247-25", "Maria Souza", "(75) 99811-2233",
                "1234567 SSP-BA", "@maria.souza", "Rua das Flores, 120, Centro, Paulo Afonso - BA",
                "Prefere contato por WhatsApp.");
        maria.autorizadas.add(new Cliente.Autorizada("João Souza", "RG 7654321", "(75) 99888-1100", "Retira/Devolve"));
        novoCliente("111.444.777-35", "João Pereira", "(75) 99922-4455",
                "2345678 SSP-BA", "@joaopereira", "Av. Apolônio Sales, 45, Tancredo Neves, Paulo Afonso - BA", "");
        Cliente ana = novoCliente("390.533.447-05", "Ana Lima", "(75) 98801-7766",
                "3456789 SSP-BA", "@analima.eventos", "Rua da Paz, 300, BTN, Paulo Afonso - BA",
                "Casamento em família, retirada sempre com a mãe.");
        ana.autorizadas.add(new Cliente.Autorizada("Rosa Lima", "RG 3344556", "(75) 99777-3322", "Retira"));
        ana.autorizadas.add(new Cliente.Autorizada("Paulo Lima", "RG 4455667", "(75) 99666-8899", "Devolve"));
        novoCliente("123.456.789-09", "Carla Mendes", "(75) 99133-8890",
                "4567890 SSP-BA", "@carlamendes", "Travessa do Sol, 18, Centro, Paulo Afonso - BA", "");
        Cliente pedro = novoCliente("746.971.314-01", "Pedro Alves", "(75) 99244-5521",
                "5678901 SSP-BA", "", "Rua Bahia, 77, Vila Poty, Paulo Afonso - BA", "");
        pedro.autorizadas.add(new Cliente.Autorizada("Marta Alves", "RG 5566778", "(75) 99555-0042", "Retira/Devolve"));
        novoCliente("168.995.350-09", "Fernanda Costa", "(75) 98855-6612",
                "6789012 SSP-BA", "@fe.costa", "Rua Projetada, 9, Barroco, Paulo Afonso - BA", "");
        Cliente lucas = novoCliente("987.654.321-00", "Lucas Rocha", "(75) 99700-1288",
                "7890123 SSP-BA", "@lucasrocha", "Rua Sergipe, 410, Centro, Paulo Afonso - BA",
                "Dano no traje na última devolução; pendência de R$ 150,00.");
        lucas.inadimplenteManual = true;
        novoCliente("453.178.287-91", "Beatriz Nunes", "(75) 99318-4400",
                "8901234 SSP-BA", "@bia.nunes", "Rua do Comércio, 52, Centro, Paulo Afonso - BA", "");
        novoCliente("295.379.955-93", "Juliana Martins", "(75) 98744-2031",
                "9012345 SSP-BA", "@ju.martins", "Av. Getúlio Vargas, 800, Paulo Afonso - BA", "");
        novoCliente("714.602.380-01", "Roberto Dias", "(75) 99655-7710",
                "0123456 SSP-BA", "", "Rua Pernambuco, 25, Centro, Paulo Afonso - BA", "");

        LocalDate h = hoje;
        exemplo(Etapa.ORCAMENTO, "Maria Souza", h.plusDays(20), h.plusDays(2), h.plusDays(19), h.plusDays(21), null,
                itens("Pacote Festa Real", 1, "Pacote Princesa", 1), 0.0)
                .servicos.add(new ServicoContratado(Catalogo.buscarServico("Entrega"), 80.0));
        exemplo(Etapa.ORCAMENTO, "João Pereira", h.plusDays(30), h.plusDays(9), h.plusDays(29), h.plusDays(31), null,
                itens("Pacote Princesa", 1, "Cadeira Tiffany", 20, "Toalha de mesa", 2), 0.0);
        exemplo(Etapa.ORCAMENTO, "Ana Lima", h.plusDays(12), h.plusDays(5), h.plusDays(11), h.plusDays(13), null,
                itens("Pacote Festa Real", 1, "Pacote Mesa Completa", 1, "Vestido Cinderela", 1), 400.0);

        exemplo(Etapa.LOCADO, "Carla Mendes", h, null, h.minusDays(1), h.plusDays(2), null,
                itens("Pacote Mesa Completa", 1, "Painel redondo", 1, "Arco de balões", 1), null);
        exemplo(Etapa.LOCADO, "Pedro Alves", h.minusDays(1), null, h.minusDays(2), h, null,
                itens("Pacote Princesa", 1, "Vestido Cinderela", 1, "Tapete vermelho", 1), null);

        exemplo(Etapa.DEVOLVIDO, "Fernanda Costa", h.minusDays(5), null, h.minusDays(6), h.minusDays(4), h.minusDays(3),
                itens("Vestido Ariel", 1, "Painel redondo", 1, "Mesa do bolo provençal", 1), null);
        Locacao lr = exemplo(Etapa.DEVOLVIDO, "Lucas Rocha", h.minusDays(7), null, h.minusDays(8), h.minusDays(6), h.minusDays(5),
                itens("Fantasia Príncipe", 1, "Trono real", 1), null);
        lr.ocorrencias.add(new Ocorrencia(Ocorrencia.Tipo.DANO, "1× Fantasia Príncipe (rasgada)", 150.0, h.minusDays(5), "Proprietária"));
        exemplo(Etapa.DEVOLVIDO, "Beatriz Nunes", h.minusDays(11), null, h.minusDays(12), h.minusDays(10), h.minusDays(9),
                itens("Pacote Festa Real", 1, "Cadeira Tiffany", 10), null);
    }
}
