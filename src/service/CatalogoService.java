package service;

import java.util.ArrayList;
import java.util.List;

import model.Categoria;
import model.EstadoObjecto;
import model.Objecto;
import model.Zona;
import repository.BaseDados;
import util.Texto;

/** Catálogo público: só mostra campos públicos (RN04, RN06). */
public class CatalogoService {

    private final BaseDados base;

    public CatalogoService(BaseDados base) {
        this.base = base;
    }

    /** Objectos DISPONÍVEIS que cumprem os filtros (qualquer filtro pode ser null ou vazio). */
    public List<Objecto> pesquisar(Categoria categoria, String cor, Zona zona, String texto) {
        List<Objecto> lista = new ArrayList<Objecto>();
        for (int i = 0; i < base.getObjectos().size(); i++) {
            Objecto o = base.getObjectos().get(i);
            if (o.getEstado() != EstadoObjecto.DISPONIVEL) {
                continue;
            }
            if (categoria != null && o.getCategoria() != categoria) {
                continue;
            }
            if (zona != null && o.getLocal() != zona) {
                continue;
            }
            if (!Texto.vazio(cor) && !Texto.normalizar(o.getCor()).equals(Texto.normalizar(cor))) {
                continue;
            }
            if (!Texto.vazio(texto) && !o.getCategoria().isSensivel()
                    && !Texto.normalizar(o.getDescricao()).contains(Texto.normalizar(texto))) {
                continue;
            }
            lista.add(o);
        }
        return lista;
    }

    /** Texto que qualquer pessoa pode ver. Nunca inclui o detalhe oculto; em objectos sensíveis, nem a descrição. */
    public String resumoPublico(Objecto o) {
        StringBuilder sb = new StringBuilder();
        sb.append(o.getCodigo()).append(" | ").append(o.getCategoria());
        if (!o.getCategoria().isSensivel()) {
            sb.append(" | ").append(o.getDescricao());
            if (!Texto.vazio(o.getFoto())) {
                sb.append(" | foto: ").append(o.getFoto());
            }
        } else {
            sb.append(" | (dados reservados: objecto sensível)");
        }
        if (!Texto.vazio(o.getCor())) {
            sb.append(" | cor: ").append(o.getCor());
        }
        sb.append(" | encontrado: ").append(o.getLocal()).append(", ")
                .append(o.getDataEncontrado().toLocalDate()).append(" | guardado na ").append(o.getSector());
        return sb.toString();
    }
}
