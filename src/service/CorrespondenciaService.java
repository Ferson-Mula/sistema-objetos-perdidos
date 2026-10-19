package service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import model.Correspondencia;
import model.EstadoObjecto;
import model.EstadoProcura;
import model.Objecto;
import model.Procura;
import repository.BaseDados;
import util.Texto;

/**
 * Cruza o que foi perdido com o que foi encontrado (P02, RN08, RN10).
 * A comparação é feita por pontuação: soma de pesos por atributo igual.
 * O sistema só sugere; nunca confirma a posse (RN08).
 */
public class CorrespondenciaService {

    public static final double PESO_COR = 0.25;
    public static final double PESO_MARCA = 0.20;
    public static final double PESO_LOCAL = 0.20;
    public static final double PESO_DATA = 0.15;
    public static final double PESO_TEXTO = 0.20;
    public static final double LIMIAR = 0.50;

    private final BaseDados base;
    private final NotificacaoService notificacoes;

    public CorrespondenciaService(BaseDados base, NotificacaoService notificacoes) {
        this.base = base;
        this.notificacoes = notificacoes;
    }

    /** Pontuação (0 a 1) entre uma procura e um objecto. Categoria diferente dá sempre 0. */
    public double pontuar(Procura p, Objecto o) {
        if (p.getCategoria() != o.getCategoria()) {
            return 0;
        }
        double pontos = 0;
        if (!Texto.vazio(p.getCor()) && !Texto.vazio(o.getCor())
                && Texto.normalizar(p.getCor()).equals(Texto.normalizar(o.getCor()))) {
            pontos += PESO_COR;
        }
        if (!Texto.vazio(p.getMarca()) && !Texto.vazio(o.getMarca())
                && Texto.normalizar(p.getMarca()).equals(Texto.normalizar(o.getMarca()))) {
            pontos += PESO_MARCA;
        }
        pontos += PESO_LOCAL * p.getLocalProvavel().proximidade(o.getLocal());
        long dias = ChronoUnit.DAYS.between(p.getPerdidoEm(), o.getDataEncontrado().toLocalDate());
        if (dias >= 0 && dias <= 30) {
            pontos += PESO_DATA;
        }
        pontos += PESO_TEXTO * Texto.sobreposicao(p.getDescricao(), o.getDescricao());
        return Math.round(pontos * 100.0) / 100.0;
    }

    /** Candidatos para uma procura, do melhor para o pior. Percorre todos os objectos DISPONÍVEIS. */
    public List<Correspondencia> candidatosPara(Procura p) {
        List<Correspondencia> lista = new ArrayList<Correspondencia>();
        for (int i = 0; i < base.getObjectos().size(); i++) {
            Objecto o = base.getObjectos().get(i);
            if (o.getEstado() == EstadoObjecto.DISPONIVEL) {
                double pontos = pontuar(p, o);
                if (pontos >= LIMIAR) {
                    lista.add(new Correspondencia(p, o, pontos));
                }
            }
        }
        ordenar(lista);
        return lista;
    }

    /** Procuras activas compatíveis com um objecto, da melhor para a pior. */
    public List<Correspondencia> procurasCompativeis(Objecto o) {
        List<Correspondencia> lista = new ArrayList<Correspondencia>();
        for (int i = 0; i < base.getProcuras().size(); i++) {
            Procura p = base.getProcuras().get(i);
            if (p.getEstado() == EstadoProcura.ACTIVA) {
                double pontos = pontuar(p, o);
                if (pontos >= LIMIAR) {
                    lista.add(new Correspondencia(p, o, pontos));
                }
            }
        }
        ordenar(lista);
        return lista;
    }

    /** Chamado quando entra um objecto novo: avisa os donos das procuras compatíveis (RN09, RN10). */
    public int avisarProcurasCompativeis(Objecto o) {
        List<Correspondencia> lista = procurasCompativeis(o);
        int avisos = 0;
        for (int i = 0; i < lista.size(); i++) {
            if (avisar(lista.get(i))) {
                avisos++;
            }
        }
        return avisos;
    }

    /** Chamado quando se regista uma perda: procura logo nos objectos que já existem. */
    public List<Correspondencia> avisarCandidatos(Procura p) {
        List<Correspondencia> lista = candidatosPara(p);
        for (int i = 0; i < lista.size(); i++) {
            avisar(lista.get(i));
        }
        return lista;
    }

    private boolean avisar(Correspondencia c) {
        String chave = c.getProcura().getId() + ":" + c.getObjecto().getCodigo();
        if (base.getAvisosCorrespondencia().contains(chave)) {
            return false;
        }
        base.getAvisosCorrespondencia().add(chave);
        notificacoes.enviar(c.getProcura().getDono(), "Possível correspondência",
                "Objecto " + c.getObjecto().getCodigo() + " (" + c.getObjecto().getCategoria() + ") pode ser o seu "
                        + "(procura #" + c.getProcura().getId() + ", pontuação "
                        + String.format("%.2f", c.getPontuacao()) + "). Consulte o catálogo e reclame se reconhecer.");
        return true;
    }

    private void ordenar(List<Correspondencia> lista) {
        Collections.sort(lista, new Comparator<Correspondencia>() {
            @Override
            public int compare(Correspondencia a, Correspondencia b) {
                return Double.compare(b.getPontuacao(), a.getPontuacao());
            }
        });
    }
}
