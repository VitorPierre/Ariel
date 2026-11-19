package dao;

import model.Locacao;

import java.util.ArrayList;
import java.util.List;

/** Guarda as locações na própria lista que o PainelController já usava — comportamento idêntico ao de hoje. */
public class LocacaoDAOMemoria implements LocacaoDAO {

    private final List<Locacao> locacoes;

    /** @param locacoes a MESMA lista que painel.locacoes (compartilhada, não copiada). */
    public LocacaoDAOMemoria(List<Locacao> locacoes) { this.locacoes = locacoes; }

    @Override public List<Locacao> listar() { return new ArrayList<>(locacoes); }

    @Override public void salvar(Locacao locacao) {
        if (!locacoes.contains(locacao)) locacoes.add(locacao);
    }
}
