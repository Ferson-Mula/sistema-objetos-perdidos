package logico;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Faz perguntas ao ficheiro regras/regras.pl.
 * O resto do programa não precisa de saber como o motor lógico funciona: usa só estes métodos.
 */
public class Regras {

    private static final Path FICHEIRO = Path.of("regras", "regras.pl");
    private static MotorLogico motor;

    private Regras() {
    }

    /** Lê o ficheiro de regras na primeira vez que é preciso. */
    private static synchronized MotorLogico motor() {
        if (motor == null) {
            try {
                MotorLogico novo = new MotorLogico();
                novo.carregar(Files.readAllLines(FICHEIRO, StandardCharsets.UTF_8));
                motor = novo;
            } catch (IOException e) {
                throw new IllegalStateException("Não encontrei o ficheiro " + FICHEIRO
                        + ". Execute o programa a partir da pasta do projecto.");
            }
        }
        return motor;
    }

    // ---------------------------------------------------------- sobre categorias
    // A categoria é indicada pela chave em minúsculas, por exemplo "telemovel".

    public static boolean sensivel(String categoria) {
        return motor().prova("sensivel", categoria);
    }

    public static boolean fotoObrigatoria(String categoria) {
        return motor().prova("foto_obrigatoria", categoria);
    }

    public static int prazoGuardaDias(String categoria) {
        return Integer.parseInt(motor().consultar("prazo_guarda", categoria, "Dias").get(0).get("Dias"));
    }

    /** Devolve por exemplo "destruicao_segura". */
    public static String destinoSugerido(String categoria) {
        return motor().consultar("destino_sugerido", categoria, "D").get(0).get("D");
    }

    // ---------------------------------------------------------- decisões

    /**
     * Uma procura e um objecto correspondem?
     * "caracteristicas" são os nomes das coisas que são verdade nesta dupla,
     * por exemplo ["mesma_categoria", "mesma_cor", "local_proximo"].
     */
    public static boolean correspondencia(List<String> caracteristicas) {
        return decidir("correspondencia", "p", "o", caracteristicas);
    }

    /** A prova apresentada numa reclamação é suficiente? */
    public static boolean verificacaoSuficiente(List<String> caracteristicas) {
        return decidir("verificacao_suficiente", "r", null, caracteristicas);
    }

    /** Cria uma cópia do motor, acrescenta os factos desta decisão e faz a pergunta. */
    private static boolean decidir(String regra, String a, String b, List<String> caracteristicas) {
        MotorLogico m = motor().copia();
        for (String c : caracteristicas) {
            if (b == null) {
                m.facto(c, a);
            } else {
                m.facto(c, a, b);
            }
        }
        return b == null ? m.prova(regra, a) : m.prova(regra, a, b);
    }
}
