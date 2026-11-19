package dao;

import model.Locacao;

import java.util.List;

/**
 * Acesso a `Locacao` (que nasce como orçamento). As regras de mudança de etapa, multa, estoque etc.
 * continuam 100% no PainelController; o DAO só guarda e devolve o que já foi decidido.
 */
public interface LocacaoDAO {
    List<Locacao> listar();
    /** Insere (nova) ou atualiza (reaproveitando o Locacao.id já existente). */
    void salvar(Locacao locacao);
}
