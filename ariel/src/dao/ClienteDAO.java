package dao;

import model.Cliente;

import java.util.List;

/** Acesso a `Cliente`. As regras (CPF único, inadimplência manual, inativação) continuam no ClienteController. */
public interface ClienteDAO {
    List<Cliente> listar();
    /** Insere (idCliente == null) ou atualiza. */
    void salvar(Cliente cliente);
    /** Exclusão definitiva (só chamada quando o controller já confirmou que não há vínculos). */
    void excluir(Cliente cliente);
}
