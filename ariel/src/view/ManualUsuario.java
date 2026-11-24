package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * VIEW: Manual do Usuário integrado à tela de Configurações.
 * Apresenta um índice clicável à esquerda e o guia detalhado em HTML estilizado à direita,
 * explicando como utilizar cada módulo do sistema Ariel · Conto de Fadas.
 */
final class ManualUsuario extends JPanel {

    private final DefaultListModel<String> modeloTopicos = new DefaultListModel<>();
    private final JList<String> listaTopicos = new JList<>(modeloTopicos);
    private final JEditorPane leitor = new JEditorPane("text/html", "");
    private final Map<String, String> topicos = new LinkedHashMap<>();

    ManualUsuario() {
        super(new BorderLayout());
        setOpaque(false);

        carregarConteudo();
        for (String titulo : topicos.keySet()) modeloTopicos.addElement(titulo);

        // ----- Índice à esquerda -----
        listaTopicos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaTopicos.setFont(Tema.fonte(Font.PLAIN, 13));
        listaTopicos.setBackground(Tema.SUPERFICIE);
        listaTopicos.setForeground(Tema.TEXTO);
        listaTopicos.setFixedCellHeight(38);
        listaTopicos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean foco) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, sel, false);
                l.setFont(Tema.fonte(sel ? Font.BOLD : Font.PLAIN, 13));
                l.setBackground(sel ? Tema.ROSA_CLARO : Tema.SUPERFICIE);
                l.setForeground(sel ? Tema.PRIMARIA_ESCURA : Tema.TEXTO);
                l.setBorder(new EmptyBorder(0, 12, 0, 12));
                return l;
            }
        });
        listaTopicos.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarTopico(listaTopicos.getSelectedValue());
        });

        JPanel caixaIndice = Ui.caixa("TÓPICOS DO MANUAL", Ui.rolagem(listaTopicos));
        caixaIndice.setPreferredSize(new Dimension(240, 200));
        caixaIndice.setMinimumSize(new Dimension(190, 200));

        // ----- Conteúdo à direita -----
        leitor.setEditable(false);
        leitor.setOpaque(true);
        leitor.setBackground(Tema.SUPERFICIE);
        leitor.setBorder(new EmptyBorder(8, 12, 16, 12));
        leitor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        leitor.setFont(Tema.fonte(Font.PLAIN, 13));

        JScrollPane rolLeitor = Ui.rolagem(leitor);
        rolLeitor.getVerticalScrollBar().setUnitIncrement(16);
        JPanel caixaConteudo = Ui.caixa("GUIA DE UTILIZAÇÃO", rolLeitor);
        caixaConteudo.setMinimumSize(new Dimension(320, 200));

        add(Ui.divisor(caixaIndice, caixaConteudo, 0.28), BorderLayout.CENTER);
        if (!modeloTopicos.isEmpty()) listaTopicos.setSelectedIndex(0);
    }

    private void mostrarTopico(String titulo) {
        if (titulo == null) return;
        String corpo = topicos.getOrDefault(titulo, "");
        String html = "<html><body style='font-family:Inter,Segoe UI,Sans-Serif; color:#3B1F2B; font-size:13px; line-height:1.5; margin:0; padding:0;'>"
                + "<h2 style='color:#A61E4D; margin:0 0 10px 0; font-size:18px;'>" + titulo + "</h2>"
                + corpo
                + "</body></html>";
        leitor.setText(html);
        leitor.setCaretPosition(0);
    }

    private void carregarConteudo() {
        topicos.put("1. Visão geral e atalhos",
                "<p>O sistema <b>Ariel · Conto de Fadas</b> organiza todo o ciclo de locação de decorações em três etapas simples:</p>"
                + "<p style='background:#FFF0F5; padding:10px; border:1px solid #F6DCE5;'>"
                + "<b>Orçamento</b> (proposta em negociação) &rarr; <b>Locado</b> (itens retirados pelo cliente) &rarr; <b>Devolvido</b> (itens conferidos na volta).</p>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Perfis de acesso</h3>"
                + "<ul>"
                + "<li><b>Administrador:</b> acesso completo, incluindo registrar pagamentos (sinal, saldo e multas), emitir contratos/recibos em PDF, gerar relatórios/gráficos e criar usuários.</li>"
                + "<li><b>Funcionário:</b> cadastra clientes, catálogo, monta orçamentos, acompanha retiradas e devoluções e altera os próprios dados.</li>"
                + "</ul>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Atalhos úteis</h3>"
                + "<ul>"
                + "<li><b>Ctrl + B</b> (ou botão <b>&laquo; / &raquo;</b> no menu): recolhe ou expande a barra lateral esquerda para ganhar mais espaço na tela.</li>"
                + "<li><b>F11:</b> alterna o modo tela cheia.</li>"
                + "<li><b>Esc:</b> sai da tela cheia (ou fecha a janela no modo normal).</li>"
                + "</ul>");

        topicos.put("2. Painel principal",
                "<p>O <b>Painel principal</b> mostra o andamento das reservas em formato de quadro Kanban com 3 colunas: <b>ORÇAMENTO</b>, <b>LOCADO</b> e <b>DEVOLVIDO</b>.</p>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Como usar</h3>"
                + "<ul>"
                + "<li><b>Cartões de resumo (topo):</b> acompanhe rapidamente reservas na semana, pagamentos pendentes, orçamentos a vencer e devoluções previstas.</li>"
                + "<li><b>Busca rápida:</b> filtre cartões por nome do cliente, código da locação ou item.</li>"
                + "<li><b>Arrastar cartões:</b> arraste um cartão de <b>ORÇAMENTO</b> para <b>LOCADO</b> para registrar a retirada (exige sinal + saldo quitado + cadastro completo), ou de <b>LOCADO</b> para <b>DEVOLVIDO</b> para abrir a conferência de devolução.</li>"
                + "<li><b>Botão + Novo orçamento:</b> abre diretamente o formulário de nova proposta.</li>"
                + "<li><b>Clicar no cartão:</b> abre os detalhes completos e ações rápidas (pagar, gerar contrato/recibo, autorizar terceiro ou excluir do painel).</li>"
                + "</ul>");

        topicos.put("3. Clientes",
                "<p>Na tela <b>Clientes</b> você gerencia todos os cadastros, controla inadimplência manual e registra pessoas autorizadas.</p>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Passo a passo</h3>"
                + "<ul>"
                + "<li><b>Novo cadastro:</b> clique em <b>+ Novo cliente</b>, preencha CPF (validado automaticamente), Nome e Endereço completo (obrigatórios para locar) e clique em <b>Salvar</b>.</li>"
                + "<li><b>Busca e filtros:</b> pesquise por nome ou CPF e alterne entre <i>Todos</i>, <i>Em dia</i>, <i>Inadimplente</i> e <i>Inativos</i>.</li>"
                + "<li><b>Inadimplência manual:</b> selecione o cliente e clique em <b>Marcar como inadimplente</b> (ou <i>Marcar como em dia</i>). Clientes inadimplentes geram alerta e exigem autorização ao confirmar novas reservas.</li>"
                + "<li><b>Inativar / Reativar cliente:</b> clique em <b>Inativar cliente</b> ao lado do botão de inadimplência. O histórico é preservado e o cliente pode ser reativado a qualquer momento pelo filtro <i>Inativos</i>.</li>"
                + "<li><b>Histórico:</b> clique em <b>Histórico</b> para ver todas as locações e situações do cliente selecionado.</li>"
                + "<li><b>Pessoas autorizadas:</b> no bloco inferior direito, clique em <b>+ Autorizar pessoa</b> para cadastrar terceiros autorizados a retirar ou devolver os itens pelo titular.</li>"
                + "</ul>");

        topicos.put("4. Catálogo",
                "<p>O <b>Catálogo</b> concentra tudo o que a empresa aluga ou oferece, dividido em 4 abas:</p>"
                + "<ul>"
                + "<li><b>Produtos:</b> itens avulsos com categoria, cor/material, valor de locação, <b>Estoque total</b> e quantidade <b>Indisponível</b> (danificada). O sistema não permite reduzir o estoque abaixo do que já está reservado.</li>"
                + "<li><b>Pacotes:</b> conjuntos fechados com preço próprio e tema opcional. Adicione os produtos e quantidades na composição. <i>Atenção:</i> pacotes usam o mesmo estoque dos produtos avulsos.</li>"
                + "<li><b>Temas e categorias:</b> cadastre temas (ex.: Princesas, Realeza) e categorias de produtos lado a lado. Use <b>+ Novo</b>, <b>Salvar</b> e <b>Excluir / Reativar</b> em cada painel.</li>"
                + "<li><b>Serviços:</b> serviços opcionais (Entrega, Montagem, Balonismo) com valor de referência.</li>"
                + "</ul>"
                + "<p><b>Regra de exclusão:</b> cadastros sem vínculos são excluídos; cadastros já usados em orçamentos/locações são <b>desativados</b> para preservar o histórico e podem ser reativados depois.</p>");

        topicos.put("5. Orçamentos",
                "<p>Em <b>Orçamentos</b> você consulta propostas em negociação, reservas confirmadas, expiradas ou canceladas e cria novas propostas.</p>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Montando um orçamento</h3>"
                + "<ul>"
                + "<li>Clique em <b>+ Novo orçamento</b>, selecione o <b>Cliente</b>, a <b>Modalidade</b> (Retira, Entrega ou Entrega + montagem) e as datas (<b>Retirada &le; Evento &le; Devolução</b>).</li>"
                + "<li>Adicione itens com <b>+ Produto</b>, <b>+ Pacote</b> e <b>+ Serviço</b>. Você pode ajustar quantidade e desconto direto na tabela.</li>"
                + "<li><b>Validade:</b> todo orçamento sem sinal vale por <b>30 dias</b> e <b>não reserva estoque</b>.</li>"
                + "<li><b>Salvar rascunho:</b> guarda a proposta em negociação para editar depois.</li>"
                + "<li><b>Registrar entrada e confirmar (Administrador):</b> escolha o sinal (10%, 20%, 50% ou outro valor acima do mínimo de 10%) e a forma de pagamento. Ao confirmar, o estoque fica reservado e o contrato PDF é gerado.</li>"
                + "</ul>");

        topicos.put("6. Locações",
                "<p>A tela <b>Locações</b> reúne todas as reservas ativas e finalizadas com busca por cliente, código ou produto e filtro por etapa ou saldo pendente.</p>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Ações disponíveis no detalhe</h3>"
                + "<ul>"
                + "<li><b>Registrar entrada / Quitar saldo:</b> registra pagamentos (exclusivo do Administrador). O saldo dos itens deve estar <b>100% quitado</b> para liberar a retirada.</li>"
                + "<li><b>Registrar retirada:</b> passa a locação para <i>Locado</i>, registrando quem retirou (somente o titular ou pessoa previamente autorizada).</li>"
                + "<li><b>Devolução:</b> abre a tela de conferência de devolução já selecionada nesta locação.</li>"
                + "<li><b>Contrato PDF e Recibo PDF:</b> gera e abre os documentos oficiais da locação (somente Administrador).</li>"
                + "<li><b>+ Serviço e Autorizar terceiro:</b> adiciona serviços extras ou autoriza um terceiro a retirar os itens desta locação específica.</li>"
                + "<li><b>Excluir:</b> remove do painel preservando o histórico (em reservas com sinal, o sinal não é reembolsado).</li>"
                + "</ul>");

        topicos.put("7. Devoluções e ocorrências",
                "<p>Quando os itens voltam do evento, faça a conferência produto por produto em <b>Devoluções</b>.</p>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Como conferir</h3>"
                + "<ul>"
                + "<li>Escolha a locação no seletor no canto superior direito e informe <b>quem está devolvendo</b> (qualquer pessoa identificada).</li>"
                + "<li>Selecione cada produto na tabela e use os botões <b>Item OK</b>, <b>Registrar dano</b>, <b>Registrar perda</b> ou <b>Ainda não voltou</b> (devolução parcial).</li>"
                + "<li><b>Perda definitiva:</b> reduz automaticamente o estoque total do produto.</li>"
                + "<li><b>Dano:</b> você decide se deseja indisponibilizar as unidades danificadas e informa o valor da cobrança (se houver).</li>"
                + "<li><b>Atraso:</b> o sistema informa os dias de atraso, mas o valor da multa é sempre digitado manualmente por você (deixe vazio se não houver cobrança).</li>"
                + "<li>Clique em <b>Registrar devolução</b>. Se restarem multas ou indenizações, use <b>Quitar pendências</b> para registrar o pagamento.</li>"
                + "</ul>");

        topicos.put("8. Relatórios e gráficos",
                "<p>A tela <b>Relatórios</b> é exclusiva do <b>Administrador</b> e serve para análise gerencial sem alterar nenhum dado do sistema.</p>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Relatórios disponíveis</h3>"
                + "<ul>"
                + "<li><b>Faturamento:</b> pagamentos efetivamente recebidos no período (entradas, saldos e cobranças).</li>"
                + "<li><b>Locações do período:</b> reservas confirmadas com evento no intervalo escolhido.</li>"
                + "<li><b>Produtos mais locados:</b> ranking por unidades locadas (desmembrando pacotes).</li>"
                + "<li><b>Ocorrências:</b> atrasos, danos e perdas registrados no período.</li>"
                + "<li><b>Demanda por tema:</b> clientes distintos e orçamentos por tema de decoração.</li>"
                + "<li><b>Inadimplência:</b> clientes marcados como inadimplentes e valores em aberto.</li>"
                + "<li><b>Fluxo de caixa:</b> recebimentos agrupados por dia, forma de pagamento e acumulado.</li>"
                + "<li><b>Agenda do período:</b> retiradas, eventos e devoluções previstos e efetivos.</li>"
                + "</ul>"
                + "<h3 style='color:#C2255C; margin-top:14px;'>Gráficos e exportação</h3>"
                + "<ul>"
                + "<li>Alterne entre as abas <b>Tabela</b> e <b>Gráfico</b> para visualizar os dados em gráficos interativos (clique com o botão direito no gráfico para salvar em PNG, copiar ou imprimir).</li>"
                + "<li>Use <b>Exportar CSV</b> (abre direto no Excel) ou <b>Exportar PDF</b> para salvar o relatório.</li>"
                + "</ul>");

        topicos.put("9. Configurações e usuários",
                "<p>Na aba <b>Conta e usuários</b> de <b>Configurações</b>:</p>"
                + "<ul>"
                + "<li><b>Meus dados:</b> qualquer usuário pode atualizar seu nome, login e senha. Para trocar a senha, informe a senha atual, a nova senha e a confirmação, depois clique em <b>Salvar meus dados</b>.</li>"
                + "<li><b>Usuários do sistema (somente Administrador):</b> visualize todos os usuários cadastrados e crie novos acessos escolhendo o perfil (<i>Administrador</i> ou <i>Funcionário</i>).</li>"
                + "</ul>"
                + "<p><b>Dica:</b> se algum funcionário esquecer a senha, o Administrador pode criar ou orientar a redefinição de acesso.</p>");
    }
}
