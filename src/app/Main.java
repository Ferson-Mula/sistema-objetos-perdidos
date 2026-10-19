package app;

import repository.BaseDados;
import repository.Persistencia;
import util.Relogio;

/**
 * Ponto de entrada.
 *   executar sem argumentos : aplicação de consola (dados em data/achados.dat)
 *   --demo                  : demonstração automática (não usa nem altera o ficheiro de dados)
 *   --reiniciar             : apaga os dados e recomeça com as contas de demonstração
 */
public class Main {

    public static final String FICHEIRO_DADOS = "data/achados.dat";

    public static void main(String[] args) {
        boolean demo = false;
        boolean reiniciar = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--demo")) {
                demo = true;
            } else if (args[i].equals("--reiniciar")) {
                reiniciar = true;
            }
        }

        if (demo) {
            new Demonstracao().executar();
            return;
        }

        BaseDados base = reiniciar ? null : Persistencia.carregar(FICHEIRO_DADOS);
        boolean primeira = base == null;
        if (primeira) {
            base = new BaseDados();
        }
        Sistema sistema = new Sistema(base, new Relogio(), true);
        if (primeira) {
            DadosIniciais.popular(sistema);
            Persistencia.gravar(base, FICHEIRO_DADOS);
            System.out.println("Dados iniciais criados. Contas de teste (palavra-passe 1234):");
            System.out.println("  Estudantes: 20230001, 20230002, 20230003 | Docente: D001");
            System.out.println("  Funcionários: F001 (Secretaria), F002 (Biblioteca) | Gestor: G001");
        }
        new Menus(sistema, new Consola(), FICHEIRO_DADOS).executar();
    }
}
