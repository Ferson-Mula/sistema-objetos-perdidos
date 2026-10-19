package app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;

import util.Texto;

/** Leitura de dados na consola. */
public class Consola {

    private final Scanner entrada = new Scanner(System.in, "UTF-8");

    public String ler(String pergunta) {
        System.out.print(pergunta + " ");
        if (!entrada.hasNextLine()) {
            throw new IllegalStateException("Fim da entrada de dados.");
        }
        return entrada.nextLine().trim();
    }

    /** Devolve um número inteiro ou -1 se o texto não for um número. */
    public int lerInt(String pergunta) {
        String t = ler(pergunta);
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean lerSimNao(String pergunta) {
        String t = Texto.normalizar(ler(pergunta + " (s/n)"));
        return t.startsWith("s");
    }

    public LocalDate lerData(String pergunta) {
        while (true) {
            String t = ler(pergunta + " (aaaa-mm-dd, vazio = hoje):");
            if (t.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(t);
            } catch (Exception e) {
                System.out.println("Data inválida.");
            }
        }
    }

    public LocalDateTime lerDataHora(String pergunta, LocalDateTime agora) {
        while (true) {
            String t = ler(pergunta + " (aaaa-mm-dd hh:mm, vazio = agora):");
            if (t.isEmpty()) {
                return agora;
            }
            try {
                String[] partes = t.split(" ");
                LocalDate d = LocalDate.parse(partes[0]);
                LocalTime h = partes.length > 1 ? LocalTime.parse(partes[1]) : LocalTime.of(12, 0);
                return LocalDateTime.of(d, h);
            } catch (Exception e) {
                System.out.println("Data/hora inválida.");
            }
        }
    }

    /** Mostra uma lista numerada e devolve o item escolhido (ou null se escolher 0). */
    public <T> T escolher(String titulo, List<T> opcoes) {
        System.out.println(titulo);
        for (int i = 0; i < opcoes.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + opcoes.get(i));
        }
        System.out.println("  0) Cancelar");
        while (true) {
            int n = lerInt("Opção:");
            if (n == 0) {
                return null;
            }
            if (n >= 1 && n <= opcoes.size()) {
                return opcoes.get(n - 1);
            }
            System.out.println("Opção inválida.");
        }
    }

    public <T> T escolherEnum(String titulo, T[] valores) {
        List<T> lista = new java.util.ArrayList<T>();
        for (int i = 0; i < valores.length; i++) {
            lista.add(valores[i]);
        }
        return escolher(titulo, lista);
    }
}
