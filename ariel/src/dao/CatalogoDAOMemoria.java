package dao;

import model.Catalogo;

import java.util.ArrayList;
import java.util.List;

/**
 * Guarda o catálogo nas próprias listas estáticas de Catalogo (ITENS, CATEGORIAS, TEMAS, SERVICOS)
 * — comportamento idêntico ao de hoje, incluindo os dados de exemplo do bloco static de Catalogo.
 */
public class CatalogoDAOMemoria implements CatalogoDAO {

    @Override public List<Catalogo.Item> listarItens() { return new ArrayList<>(Catalogo.ITENS); }
    @Override public List<Catalogo.Categoria> listarCategorias() { return new ArrayList<>(Catalogo.CATEGORIAS); }
    @Override public List<Catalogo.Tema> listarTemas() { return new ArrayList<>(Catalogo.TEMAS); }
    @Override public List<Catalogo.Servico> listarServicos() { return new ArrayList<>(Catalogo.SERVICOS); }

    @Override public void salvarItem(Catalogo.Item item) {
        if (!Catalogo.ITENS.contains(item)) Catalogo.ITENS.add(item);
    }

    @Override public void excluirItem(Catalogo.Item item) { Catalogo.ITENS.remove(item); }

    @Override public void salvarCategoria(Catalogo.Categoria categoria) {
        if (!Catalogo.CATEGORIAS.contains(categoria)) Catalogo.CATEGORIAS.add(categoria);
    }

    @Override public void excluirCategoria(Catalogo.Categoria categoria) { Catalogo.CATEGORIAS.remove(categoria); }

    @Override public void salvarTema(Catalogo.Tema tema) {
        if (!Catalogo.TEMAS.contains(tema)) Catalogo.TEMAS.add(tema);
    }

    @Override public void excluirTema(Catalogo.Tema tema) { Catalogo.TEMAS.remove(tema); }

    @Override public void salvarServico(Catalogo.Servico servico) {
        if (!Catalogo.SERVICOS.contains(servico)) Catalogo.SERVICOS.add(servico);
    }

    @Override public void excluirServico(Catalogo.Servico servico) { Catalogo.SERVICOS.remove(servico); }
}
