package funcional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Funções puras para tratar texto. "Pura" quer dizer: dá sempre o mesmo resultado
 * para os mesmos argumentos e não altera nada fora da função.
 */
public final class Texto {

    private Texto() {
    }

    public static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    /** "Telemóvel AZUL" -> "telemovel azul" */
    public static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    /** Palavras com mais de 2 letras, sem acentos e sem repetições. */
    public static List<String> palavras(String s) {
        return Arrays.stream(normalizar(s).split("[^a-z0-9]+"))
                .filter(p -> p.length() > 2)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Fracção (0 a 1) das palavras de "referencia" que também aparecem em "resposta". */
    public static double cobertura(String referencia, String resposta) {
        List<String> ref = palavras(referencia);
        List<String> resp = palavras(resposta);
        if (ref.isEmpty()) {
            return 0;
        }
        return (double) ref.stream().filter(resp::contains).count() / ref.size();
    }

    /** Palavras em comum a dividir pelo menor dos dois textos (0 a 1). */
    public static double sobreposicao(String a, String b) {
        List<String> pa = palavras(a);
        List<String> pb = palavras(b);
        if (pa.isEmpty() || pb.isEmpty()) {
            return 0;
        }
        return (double) pa.stream().filter(pb::contains).count() / Math.min(pa.size(), pb.size());
    }
}
