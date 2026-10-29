package app;

import java.nio.file.Path;

import repository.BaseDados;
import service.PrazoService;
import service.ReclamacaoService;
import service.RegistoService;
import service.Relogio;

/**
 * Ponto de entrada do programa.
 *   sem argumentos -> abre o menu interactivo
 *   demo           -> mostra uma demonstração automática dos 4 processos
 */
public class Main {

    private static final Path FICHEIRO = Path.of("data", "base.dat");

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("demo")) {
            new Demonstracao().executar();
            return;
        }
        BaseDados base = BaseDados.carregar(FICHEIRO);
        if (base == null) {
            base = new BaseDados();
            Demonstracao.criarContas(base);
            System.out.println("Primeira execução: criadas contas de teste (palavra-passe 1234).");
        }
        Relogio relogio = new Relogio();
        new Menus(base, relogio, new RegistoService(base, relogio), new ReclamacaoService(base, relogio),
                new PrazoService(base, relogio), FICHEIRO).iniciar();
    }
}
