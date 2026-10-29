package funcional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import logico.Regras;
import model.Categoria;
import model.EstadoObjecto;
import model.Objecto;
import model.Procura;
import model.Reclamacao;

/**
 * Funções puras sobre listas (paradigma funcional): filtram, transformam e contam
 * usando streams e lambdas, sem alterar nada.
 *
 * Divisão de trabalho com o paradigma lógico:
 *   - aqui (funcional) EXTRAÍMOS os factos: "mesma cor", "local próximo", ...
 *   - nas regras (lógico) DECIDIMOS: "com estes factos, há correspondência?"
 */
public final class Consultas {

    private Consultas() {
    }

    // ------------------------------------------------------------ catálogo

    /** Objectos que qualquer pessoa pode ver (RN06). */
    public static List<Objecto> catalogo(List<Objecto> objectos) {
        return objectos.stream()
                .filter(o -> o.getEstado() == EstadoObjecto.DISPONIVEL)
                .collect(Collectors.toList());
    }

    /** Pesquisa no catálogo. Procura só nos dados públicos: nunca no detalhe oculto. */
    public static List<Objecto> pesquisar(List<Objecto> objectos, Categoria categoria, String texto) {
        return catalogo(objectos).stream()
                .filter(o -> categoria == null || o.getCategoria() == categoria)
                .filter(o -> Texto.vazio(texto) || textoPublico(o).contains(Texto.normalizar(texto)))
                .collect(Collectors.toList());
    }

    private static String textoPublico(Objecto o) {
        String descricao = o.getCategoria().isSensivel() ? "" : o.getDescricao();
        return Texto.normalizar(descricao + " " + o.getCor() + " " + o.getMarca());
    }

    // ------------------------------------------------------------ correspondência (facto + lógico)

    /** Lista os factos verdadeiros para uma procura e um objecto. Não decide nada. */
    public static List<String> caracteristicas(Procura p, Objecto o) {
        List<String> factos = new ArrayList<>();
        if (p.getCategoria() == o.getCategoria()) {
            factos.add("mesma_categoria");
        }
        if (!Texto.vazio(p.getCor()) && Texto.normalizar(p.getCor()).equals(Texto.normalizar(o.getCor()))) {
            factos.add("mesma_cor");
        }
        if (!Texto.vazio(p.getMarca()) && Texto.normalizar(p.getMarca()).equals(Texto.normalizar(o.getMarca()))) {
            factos.add("mesma_marca");
        }
        if (p.getLocalProvavel().estaPerto(o.getLocal())) {
            factos.add("local_proximo");
        }
        long dias = ChronoUnit.DAYS.between(p.getPerdidoEm(), o.getEncontradoEm().toLocalDate());
        if (dias >= 0 && dias <= 30) {
            factos.add("data_compativel");
        }
        if (Texto.sobreposicao(p.getDescricao(), o.getDescricao()) >= 0.5) {
            factos.add("descricao_parecida");
        }
        return factos;
    }

    /** Objectos disponíveis que correspondem a uma procura (decisão tomada pelas regras). */
    public static List<Objecto> candidatos(Procura p, List<Objecto> objectos) {
        return catalogo(objectos).stream()
                .filter(o -> Regras.correspondencia(caracteristicas(p, o)))
                .collect(Collectors.toList());
    }

    /** Procuras activas que correspondem a um objecto. */
    public static List<Procura> procurasCompativeis(Objecto o, List<Procura> procuras) {
        return procuras.stream()
                .filter(Procura::isActiva)
                .filter(p -> Regras.correspondencia(caracteristicas(p, o)))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------ verificação (facto + lógico)

    /** Lista os factos sobre a prova apresentada numa reclamação. */
    public static List<String> caracteristicasProva(Reclamacao r) {
        Objecto o = r.getObjecto();
        List<String> factos = new ArrayList<>();
        double acerto = Texto.cobertura(o.getDetalheOculto(), r.getResposta());
        if (Texto.vazio(o.getDetalheOculto())) {
            factos.add("sem_detalhe_oculto");
        } else if (acerto >= 0.8) {
            factos.add("detalhe_correcto");
        } else if (acerto >= 0.4) {
            factos.add("detalhe_parcial");
        }
        if (!Texto.vazio(r.getProvas())) {
            factos.add("tem_provas");
        }
        if (r.getResposta() != null && r.getResposta().trim().length() >= 15) {
            factos.add("resposta_longa");
        }
        return factos;
    }

    public static boolean provaSuficiente(Reclamacao r) {
        return Regras.verificacaoSuficiente(caracteristicasProva(r));
    }

    // ------------------------------------------------------------ estatísticas

    /** Quantos objectos há em cada estado. */
    public static Map<EstadoObjecto, Long> contarPorEstado(List<Objecto> objectos) {
        return objectos.stream().collect(
                Collectors.groupingBy(Objecto::getEstado, TreeMap::new, Collectors.counting()));
    }

    /** Quantos objectos há em cada categoria. */
    public static Map<Categoria, Long> contarPorCategoria(List<Objecto> objectos) {
        return objectos.stream().collect(
                Collectors.groupingBy(Objecto::getCategoria, TreeMap::new, Collectors.counting()));
    }

    /** Percentagem de objectos entrados no sector (não cancelados) que foram devolvidos. */
    public static double taxaDevolucao(List<Objecto> objectos) {
        long entrados = objectos.stream().filter(o -> o.getEntradaEm() != null).count();
        long devolvidos = objectos.stream().filter(o -> o.getEstado() == EstadoObjecto.DEVOLVIDO).count();
        return entrados == 0 ? 0 : 100.0 * devolvidos / entrados;
    }

    // ------------------------------------------------------------ datas

    /** Soma dias úteis (segunda a sexta) a uma data. */
    public static LocalDate somarDiasUteis(LocalDate data, int dias) {
        LocalDate d = data;
        int somados = 0;
        while (somados < dias) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                somados++;
            }
        }
        return d;
    }
}
