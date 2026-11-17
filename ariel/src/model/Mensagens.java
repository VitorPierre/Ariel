package model;

/** Dicionário de mensagens do sistema (seção 13 do Documento de Requisitos). */
public final class Mensagens {
    private Mensagens() { }

    public static final String MSG001 = "Operação realizada com sucesso.";
    public static final String MSG002 = "Atenção: preencha todos os campos obrigatórios para prosseguir.";
    public static final String MSG003 = "Erro: cadastro não realizado. O CPF, código ou login informado já existe no sistema.";
    public static final String MSG004 = "Alteração salva com sucesso.";
    public static final String MSG005 = "Operação confirmada com sucesso (exclusão, devolução ou finalização).";
    public static final String MSG006 = "Não é possível excluir o registro pois ele possui vínculos (orçamentos, locações, pagamentos). O sistema realizou a desativação para manter o histórico.";
    public static final String MSG007 = "Nenhum registro encontrado para os filtros ou termos informados.";
    public static final String MSG008 = "Acesso negado: login ou senha incorretos.";
    public static final String MSG009 = "Erro interno de comunicação com a base de dados.";
    public static final String MSG010 = "Indisponibilidade: um ou mais produtos/pacotes solicitados estão em conflito de agenda no período selecionado.";
    public static final String MSG011 = "Dados inválidos: verifique se as datas, valores numéricos ou formatações inseridas estão corretas.";
    public static final String MSG012 = "Bloqueio de confirmação: o cliente selecionado possui débitos anteriores impeditivos. Para prosseguir é necessária autorização manual registrada.";
    public static final String MSG013 = "Bloqueio de confirmação: o cadastro do cliente está incompleto (CPF e/ou endereço ausentes).";
    public static final String MSG014 = "Confirmação pendente: é obrigatório o registro do pagamento da entrada no valor de 10%, 20% ou 50% para efetivar a reserva.";
    public static final String MSG015 = "A locação não pode ser finalizada, pois existem pendências financeiras, de devolução ou de ocorrências não resolvidas.";
    public static final String MSG016 = "Falha ao gerar o documento PDF. Verifique os dados da locação e tente novamente.";
    public static final String MSG017 = "Aviso: ação não permitida para orçamentos expirados ou cancelados.";
    public static final String MSG018 = "Documento (contrato/recibo) gerado e disponibilizado para visualização com sucesso.";
    public static final String MSG019 = "Autenticação realizada com sucesso. Bem-vindo(a) ao sistema Ariel!";
    public static final String MSG020 = "Produtos e pacotes disponíveis para o período desejado!";
}
