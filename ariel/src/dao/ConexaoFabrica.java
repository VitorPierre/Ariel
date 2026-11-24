package dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Lê `ariel.properties` (na raiz do projeto, ao lado da pasta `src`) e decide:
 *  - modo=memoria (padrão, se o arquivo não existir): nada muda, os DAOs usam as listas em memória.
 *  - modo=mysql: abre conexão JDBC com host/porta/banco/usuario/senha do arquivo.
 *
 * Importante: o driver do MySQL (mysql-connector-j, ex.: mysql-connector-j-9.x.jar) NÃO vem com este
 * projeto nem pode ser baixado automaticamente aqui — baixe o .jar e coloque numa pasta `lib/` na raiz
 * do projeto, e adicione essa pasta ao classpath/módulos do seu IntelliJ. Sem o driver, o modo "mysql"
 * lança RepositorioException na primeira tentativa de conexão (ClassNotFoundException), nunca antes disso.
 */
public final class ConexaoFabrica {

    private static final String ARQUIVO = "ariel.properties";
    private static Properties props;

    private ConexaoFabrica() { }

    private static synchronized Properties props() {
        if (props == null) {
            props = new Properties();
            try (InputStream in = ConexaoFabrica.class.getClassLoader().getResourceAsStream(ARQUIVO)) {
                if (in != null) {
                    props.load(in);
                    return props;
                }
            } catch (IOException e) {
                throw new RepositorioException("Não consegui ler " + ARQUIVO, e);
            }
            java.nio.file.Path caminho = java.nio.file.Path.of(ARQUIVO);
            if (java.nio.file.Files.exists(caminho)) {
                try (InputStream in = java.nio.file.Files.newInputStream(caminho)) {
                    props.load(in);
                } catch (IOException e) {
                    throw new RepositorioException("Não consegui ler " + ARQUIVO, e);
                }
            }
        }
        return props;
    }

    public static ModoPersistencia modo() {
        String m = props().getProperty("modo", "memoria").trim().toLowerCase();
        return m.equals("mysql") ? ModoPersistencia.MYSQL : ModoPersistencia.MEMORIA;
    }

    /** Nova conexão JDBC (feche com try-with-resources). Só chame quando modo() == MYSQL. */
    public static Connection abrir() {
        Properties p = props();
        String host = p.getProperty("mysql.host", "localhost");
        String porta = p.getProperty("mysql.porta", "3306");
        String banco = p.getProperty("mysql.banco", "ariel");
        String usuario = p.getProperty("mysql.usuario", "root");
        String senha = p.getProperty("mysql.senha", "123456");
        String url = "jdbc:mysql://" + host + ":" + porta + "/" + banco
                + "?characterEncoding=UTF-8&serverTimezone=America/Sao_Paulo";
        try {
            // Carrega o driver explicitamente: evita um erro confuso se o .jar não estiver no classpath.
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, usuario, senha);
        } catch (ClassNotFoundException e) {
            throw new RepositorioException(
                "Driver do MySQL (mysql-connector-j) não encontrado no classpath. "
                + "Baixe o .jar em dev.mysql.com/downloads/connector/j e coloque na pasta lib/ do projeto.", e);
        } catch (SQLException e) {
            throw new RepositorioException("Não consegui conectar ao MySQL em " + host + ":" + porta + "/" + banco, e);
        }
    }
}
