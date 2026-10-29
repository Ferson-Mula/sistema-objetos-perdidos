package logico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor lógico muito pequeno (estilo Prolog).
 *
 * Um "termo" é uma lista de textos: o primeiro é o nome e os outros são os argumentos.
 * Exemplo: sensivel(telemovel) fica ["sensivel", "telemovel"].
 * Argumentos que começam com letra MAIÚSCULA são variáveis.
 *
 * Para provar um objectivo, o motor tenta (por esta ordem):
 *   1. factos que combinem com o objectivo;
 *   2. regras cuja cabeça combine com o objectivo (e depois prova o corpo da regra).
 */
public class MotorLogico {

    /** Uma regra: "cabeca :- corpo". Um facto é uma regra sem corpo. */
    private record Regra(List<String> cabeca, List<List<String>> corpo) {
    }

    private final List<List<String>> factos = new ArrayList<>();
    private final List<Regra> regras = new ArrayList<>();
    private int contador = 0;

    // ------------------------------------------------------------ preparar o motor

    /** Acrescenta um facto, por exemplo facto("mesma_cor", "p1", "o1"). */
    public void facto(String... termo) {
        factos.add(List.of(termo));
    }

    /** Lê regras escritas em texto (formato do ficheiro regras.pl). */
    public void carregar(List<String> linhas) {
        StringBuilder texto = new StringBuilder();
        for (String linha : linhas) {
            int comentario = linha.indexOf('%');
            texto.append(comentario >= 0 ? linha.substring(0, comentario) : linha).append(' ');
        }
        for (String clausula : texto.toString().split("\\.\\s+")) {
            if (clausula.isBlank()) {
                continue;
            }
            String[] partes = clausula.split(":-");
            List<List<String>> corpo = new ArrayList<>();
            if (partes.length > 1) {
                for (String objectivo : dividirPorVirgulas(partes[1])) {
                    corpo.add(lerTermo(objectivo));
                }
            }
            regras.add(new Regra(lerTermo(partes[0]), corpo));
        }
    }

    /** Cria uma cópia (com as mesmas regras e factos) para acrescentar factos só desta pergunta. */
    public MotorLogico copia() {
        MotorLogico m = new MotorLogico();
        m.factos.addAll(factos);
        m.regras.addAll(regras);
        return m;
    }

    // ------------------------------------------------------------ perguntar

    /** Responde "sim" ou "não". Exemplo: prova("sensivel", "telemovel"). */
    public synchronized boolean prova(String... objectivo) {
        return !resolver(List.of(List.of(objectivo)), new HashMap<>(), 1).isEmpty();
    }

    /**
     * Devolve todas as respostas. Cada resposta diz o valor de cada variável.
     * Exemplo: consultar("destino_sugerido", "telemovel", "D") devolve [{D=destruicao_segura}].
     */
    public synchronized List<Map<String, String>> consultar(String... objectivo) {
        List<Map<String, String>> respostas = new ArrayList<>();
        for (Map<String, String> valores : resolver(List.of(List.of(objectivo)), new HashMap<>(), 1000)) {
            Map<String, String> resposta = new HashMap<>();
            for (String termo : objectivo) {
                if (eVariavel(termo)) {
                    resposta.put(termo, valorDe(termo, valores));
                }
            }
            respostas.add(resposta);
        }
        return respostas;
    }

    // ------------------------------------------------------------ o raciocínio

    private List<Map<String, String>> resolver(List<List<String>> objectivos, Map<String, String> valores,
                                               int maximo) {
        List<Map<String, String>> respostas = new ArrayList<>();
        resolver(objectivos, valores, respostas, maximo);
        return respostas;
    }

    private void resolver(List<List<String>> objectivos, Map<String, String> valores,
                          List<Map<String, String>> respostas, int maximo) {
        if (respostas.size() >= maximo) {
            return;
        }
        if (objectivos.isEmpty()) {
            respostas.add(valores);   // todos os objectivos foram provados
            return;
        }
        List<String> primeiro = objectivos.get(0);
        List<List<String>> resto = objectivos.subList(1, objectivos.size());

        // "\+" = não: é verdade se NÃO conseguirmos provar o que vem a seguir.
        if (primeiro.get(0).equals("\\+")) {
            List<List<String>> negado = List.of(primeiro.subList(1, primeiro.size()));
            if (resolver(negado, new HashMap<>(valores), 1).isEmpty()) {
                resolver(resto, valores, respostas, maximo);
            }
            return;
        }

        for (List<String> facto : factos) {
            Map<String, String> novos = new HashMap<>(valores);
            if (combinar(primeiro, facto, novos)) {
                resolver(resto, novos, respostas, maximo);
            }
        }
        for (Regra regra : regras) {
            Regra r = comVariaveisNovas(regra);
            Map<String, String> novos = new HashMap<>(valores);
            if (combinar(primeiro, r.cabeca(), novos)) {
                List<List<String>> proximos = new ArrayList<>(r.corpo());
                proximos.addAll(resto);
                resolver(proximos, novos, respostas, maximo);
            }
        }
    }

    /** Dois termos combinam se tiverem o mesmo tamanho e cada par de elementos combinar. */
    private boolean combinar(List<String> a, List<String> b, Map<String, String> valores) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            String x = valorDe(a.get(i), valores);
            String y = valorDe(b.get(i), valores);
            if (x.equals(y)) {
                continue;
            }
            if (eVariavel(x)) {
                valores.put(x, y);
            } else if (eVariavel(y)) {
                valores.put(y, x);
            } else {
                return false;
            }
        }
        return true;
    }

    /** Se o texto for uma variável que já tem valor, devolve o valor. */
    private String valorDe(String texto, Map<String, String> valores) {
        while (eVariavel(texto) && valores.containsKey(texto)) {
            texto = valores.get(texto);
        }
        return texto;
    }

    private boolean eVariavel(String texto) {
        return Character.isUpperCase(texto.charAt(0));
    }

    /** Cada vez que usamos uma regra, as variáveis ganham um número novo para não se misturarem. */
    private Regra comVariaveisNovas(Regra regra) {
        contador++;
        List<List<String>> corpo = new ArrayList<>();
        for (List<String> termo : regra.corpo()) {
            corpo.add(renomear(termo));
        }
        return new Regra(renomear(regra.cabeca()), corpo);
    }

    private List<String> renomear(List<String> termo) {
        List<String> novo = new ArrayList<>();
        for (String t : termo) {
            novo.add(eVariavel(t) ? t + "_" + contador : t);
        }
        return novo;
    }

    // ------------------------------------------------------------ ler texto

    private List<String> dividirPorVirgulas(String texto) {
        List<String> partes = new ArrayList<>();
        int nivel = 0;
        StringBuilder atual = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c == '(') {
                nivel++;
            } else if (c == ')') {
                nivel--;
            }
            if (c == ',' && nivel == 0) {
                partes.add(atual.toString());
                atual = new StringBuilder();
            } else {
                atual.append(c);
            }
        }
        partes.add(atual.toString());
        return partes;
    }

    /** Transforma "nome(a, B)" em ["nome", "a", "B"]. Aceita "\+ nome(a)". */
    private List<String> lerTermo(String texto) {
        String t = texto.trim();
        List<String> termo = new ArrayList<>();
        if (t.startsWith("\\+")) {
            termo.add("\\+");
            t = t.substring(2).trim();
        }
        int abre = t.indexOf('(');
        if (abre < 0) {
            termo.add(t);
            return termo;
        }
        termo.add(t.substring(0, abre).trim());
        for (String argumento : t.substring(abre + 1, t.lastIndexOf(')')).split(",")) {
            termo.add(argumento.trim());
        }
        return termo;
    }
}
