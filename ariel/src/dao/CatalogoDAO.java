package dao;

import model.Catalogo;

import java.util.List;

/**
 * Acesso ao catálogo: produtos, pacotes, categorias, temas e serviços — o mesmo escopo do
 * CatalogoController. As regras (estoque, vínculos, nome único) continuam lá; o DAO só guarda e devolve.
 */
public interface CatalogoDAO {
    List<Catalogo.Item> listarItens();
    List<Catalogo.Categoria> listarCategorias();
    List<Catalogo.Tema> listarTemas();
    List<Catalogo.Servico> listarServicos();

    /** Insere (idProduto/idPacote == null, conforme o tipo) ou atualiza. */
    void salvarItem(Catalogo.Item item);
    void excluirItem(Catalogo.Item item);

    void salvarCategoria(Catalogo.Categoria categoria);
    void excluirCategoria(Catalogo.Categoria categoria);

    void salvarTema(Catalogo.Tema tema);
    void excluirTema(Catalogo.Tema tema);

    void salvarServico(Catalogo.Servico servico);
    void excluirServico(Catalogo.Servico servico);
}
