package util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/** Funções auxiliares para tratar texto (sem acentos, minúsculas, palavras). */
public final class Texto {

    private Texto() {
    }

    public static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase().trim();
    }

    /** Palavras com mais de 2 letras, já normalizadas e sem repetições. */
    public static List<String> palavras(String s) {
        List<String> resultado = new ArrayList<String>();
        String[] partes = normalizar(s).split("[^a-z0-9]+");
        for (int i = 0; i < partes.length; i++) {
            if (partes[i].length() > 2 && !resultado.contains(partes[i])) {
                resultado.add(partes[i]);
            }
        }
        return resultado;
    }

    /** Fracção (0 a 1) das palavras de "referencia" que aparecem em "resposta". */
    public static double cobertura(String referencia, String resposta) {
        List<String> ref = palavras(referencia);
        if (ref.isEmpty()) {
            return 0;
        }
        List<String> resp = palavras(resposta);
        int encontradas = 0;
        for (int i = 0; i < ref.size(); i++) {
            if (resp.contains(ref.get(i))) {
                encontradas++;
            }
        }
        return (double) encontradas / ref.size();
    }

    /** Palavras em comum a dividir pelo menor dos dois conjuntos (0 a 1). */
    public static double sobreposicao(String a, String b) {
        List<String> pa = palavras(a);
        List<String> pb = palavras(b);
        if (pa.isEmpty() || pb.isEmpty()) {
            return 0;
        }
        int comuns = 0;
        for (int i = 0; i < pa.size(); i++) {
            if (pb.contains(pa.get(i))) {
                comuns++;
            }
        }
        return (double) comuns / Math.min(pa.size(), pb.size());
    }
}
