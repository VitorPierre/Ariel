package dao;

import model.Cliente;

import java.util.ArrayList;
import java.util.List;

/** Guarda os clientes na própria lista que o PainelController já usava — comportamento idêntico ao de hoje. */
public class ClienteDAOMemoria implements ClienteDAO {

    private final List<Cliente> clientes;

    /** @param clientes a MESMA lista que painel.clientes (compartilhada, não copiada). */
    public ClienteDAOMemoria(List<Cliente> clientes) { this.clientes = clientes; }

    @Override public List<Cliente> listar() { return new ArrayList<>(clientes); }

    @Override public void salvar(Cliente cliente) {
        if (!clientes.contains(cliente)) clientes.add(cliente);
    }

    @Override public void excluir(Cliente cliente) { clientes.remove(cliente); }
}
