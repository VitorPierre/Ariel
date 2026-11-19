package dao;

/**
 * Qualquer falha de persistência (SQLException, conexão caiu, etc.) chega aqui, sem obrigar
 * controller nem view a conhecer java.sql. O controller trata como trataria uma RegraNegocioException
 * de infraestrutura: mostra uma mensagem e deixa o usuário tentar de novo.
 */
public class RepositorioException extends RuntimeException {
    public RepositorioException(String mensagem, Throwable causa) { super(mensagem, causa); }
    public RepositorioException(String mensagem) { super(mensagem); }
}
