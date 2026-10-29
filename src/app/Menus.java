package app;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import funcional.Consultas;
import model.Categoria;
import model.EstadoObjecto;
import model.EstadoReclamacao;
import model.Objecto;
import model.Perfil;
import model.Procura;
import model.Reclamacao;
import model.RegraNegocioException;
import model.Sector;
import model.Utilizador;
import model.Zona;
import repository.BaseDados;
import service.PrazoService;
import service.ReclamacaoService;
import service.RegistoService;
import service.Relogio;

/** Menus da consola. Cada perfil vê só o que pode fazer. Depois de cada acção os dados são guardados. */
public class Menus {

    private final BaseDados base;
    private final Relogio relogio;
    private final RegistoService registo;
    private final ReclamacaoService reclamacoes;
    private final PrazoService prazos;
    private final Path ficheiro;
    private final Scanner teclado = new Scanner(System.in);

    public Menus(BaseDados base, Relogio relogio, RegistoService registo, ReclamacaoService reclamacoes,
                 PrazoService prazos, Path ficheiro) {
        this.base = base;
        this.relogio = relogio;
        this.registo = registo;
        this.reclamacoes = reclamacoes;
        this.prazos = prazos;
        this.ficheiro = ficheiro;
    }

    // ============================================================ menu inicial

    public void iniciar() {
        while (true) {
            System.out.println("\n=== Achados e Perdidos - UJC ===");
            System.out.println("1) Entrar   2) Criar conta   3) Ver catálogo   0) Sair");
            String opcao = ler("Opção");
            if (opcao.equals("0")) {
                return;
            } else if (opcao.equals("1")) {
                Utilizador u = base.entrar(ler("Número"), ler("Palavra-passe"));
                if (u == null) {
                    System.out.println("Número ou palavra-passe errados.");
                } else {
                    menuDoPerfil(u);
                }
            } else if (opcao.equals("2")) {
                tentar(this::criarConta);
            } else if (opcao.equals("3")) {
                mostrarCatalogo();
            }
        }
    }

    private void criarConta() {
        Perfil perfil = ler("Perfil (1 = estudante, 2 = docente)").equals("2") ? Perfil.DOCENTE : Perfil.ESTUDANTE;
        base.criarUtilizador(ler("Número"), ler("Nome"), ler("Email"), ler("Nº do documento de identificação"),
                ler("Palavra-passe"), perfil, null);
        System.out.println("Conta criada.");
        base.guardar(ficheiro);
    }

    private void menuDoPerfil(Utilizador u) {
        while (true) {
            System.out.println("\n--- " + u + " | avisos novos: " + u.naoLidas() + " ---");
            List<String> opcoes = switch (u.getPerfil()) {
                case ESTUDANTE, DOCENTE -> List.of("Registar objecto encontrado", "Registar objecto perdido",
                        "Ver / pesquisar catálogo", "As minhas procuras", "Reclamar um objecto",
                        "As minhas reclamações", "Avisos");
                case FUNCIONARIO -> List.of("Confirmar entrada de objecto", "Rever reclamações",
                        "Entregar objecto aprovado", "Executar destino aprovado", "Objectos do meu sector", "Avisos");
                case GESTOR -> List.of("Executar rotina de prazos", "Decidir recursos", "Aprovar destino final",
                        "Relatório", "Avançar o relógio (simulação)", "Avisos");
            };
            for (int i = 0; i < opcoes.size(); i++) {
                System.out.println((i + 1) + ") " + opcoes.get(i));
            }
            System.out.println("0) Sair da conta");
            String opcao = ler("Opção");
            if (opcao.equals("0")) {
                return;
            }
            String escolhida = opcao.matches("\\d+") && Integer.parseInt(opcao) <= opcoes.size()
                    ? opcoes.get(Integer.parseInt(opcao) - 1) : "";
            tentar(() -> executar(u, escolhida));
            base.guardar(ficheiro);
        }
    }

    private void executar(Utilizador u, String opcao) {
        switch (opcao) {
            case "Registar objecto encontrado" -> registarEncontrado(u);
            case "Registar objecto perdido" -> registarPerda(u);
            case "Ver / pesquisar catálogo" -> mostrarCatalogo();
            case "As minhas procuras" -> minhasProcuras(u);
            case "Reclamar um objecto" -> reclamar(u);
            case "As minhas reclamações" -> minhasReclamacoes(u);
            case "Confirmar entrada de objecto" -> confirmarEntrada(u);
            case "Rever reclamações" -> reverReclamacoes(u);
            case "Entregar objecto aprovado" -> entregar(u);
            case "Executar destino aprovado" -> executarDestino(u);
            case "Objectos do meu sector" -> base.objectos.stream().filter(o -> o.getSector() == u.getSector())
                    .forEach(o -> System.out.println("  " + o + " | " + o.getLocalizacaoFisica()));
            case "Executar rotina de prazos" -> prazos.executarRotina().forEach(l -> System.out.println("  " + l));
            case "Decidir recursos" -> decidirRecursos(u);
            case "Aprovar destino final" -> aprovarDestino(u);
            case "Relatório" -> relatorio();
            case "Avançar o relógio (simulação)" -> {
                relogio.avancarDias(Long.parseLong(ler("Quantos dias")));
                System.out.println("Data actual do sistema: " + relogio.agora());
            }
            case "Avisos" -> {
                u.getNotificacoes().forEach(n -> System.out.println("  - " + n));
                u.marcarComoLidas();
            }
            default -> System.out.println("Opção inválida.");
        }
    }

    // ============================================================ estudante / docente

    private void registarEncontrado(Utilizador u) {
        Categoria c = escolher("Categoria", Categoria.values());
        String descricao = ler("Descrição");
        String cor = ler("Cor");
        String marca = ler("Marca (pode ficar vazio)");
        Zona zona = escolher("Onde encontrou", Zona.values());
        String foto = c.isFotoObrigatoria() ? ler("Nome do ficheiro da foto") : "";
        String detalhe = ler("Detalhe oculto (só o dono saberia; fica escondido)");
        Sector sector = escolher("Onde vai entregar", Sector.values());
        Objecto o = registo.registarEncontrado(u, c, descricao, cor, marca, zona, relogio.agora(), foto, detalhe,
                sector);
        System.out.println("Registado. Código: " + o.getCodigo() + ". Entregue-o na " + sector + ".");
    }

    private void registarPerda(Utilizador u) {
        Procura p = registo.registarPerda(u, escolher("Categoria", Categoria.values()), ler("Descrição"),
                ler("Cor"), ler("Marca (pode ficar vazio)"), escolher("Onde perdeu", Zona.values()),
                LocalDate.parse(ler("Data da perda (AAAA-MM-DD)")));
        System.out.println("Procura #" + p.getId() + " registada.");
    }

    private void mostrarCatalogo() {
        Categoria c = ler("Filtrar por categoria? (s/n)").equalsIgnoreCase("s")
                ? escolher("Categoria", Categoria.values()) : null;
        List<Objecto> lista = Consultas.pesquisar(base.objectos, c, ler("Texto a pesquisar (pode ficar vazio)"));
        if (lista.isEmpty()) {
            System.out.println("Nenhum objecto encontrado.");
        }
        lista.forEach(o -> System.out.println("  " + o.resumoPublico()));
    }

    private void minhasProcuras(Utilizador u) {
        base.procuras.stream().filter(p -> p.getDono().equals(u)).forEach(p -> {
            System.out.println("  " + p);
            Consultas.candidatos(p, base.objectos).forEach(o -> System.out.println("     possível: " + o.resumoPublico()));
        });
    }

    private void reclamar(Utilizador u) {
        String codigo = ler("Código do objecto");
        Objecto o = base.objecto(codigo);
        if (o != null && o.getCategoria().isSensivel()) {
            System.out.println("Este objecto tem dados reservados. Descreva um detalhe que só o dono saberia.");
        }
        Procura procura = null;
        String idProcura = ler("Nº da sua procura (Enter se não tiver)");
        if (!idProcura.isBlank()) {
            procura = base.procura(Integer.parseInt(idProcura));
        }
        Reclamacao r = reclamacoes.submeter(u, codigo, ler("Detalhe que só o dono saberia"),
                ler("Provas (ficheiro, foto, recibo; pode ficar vazio)"), procura);
        System.out.println("Reclamação #" + r.getId() + ": " + r.getEstado());
    }

    private void minhasReclamacoes(Utilizador u) {
        base.reclamacoes.stream().filter(r -> r.getReclamante().equals(u)).forEach(r -> System.out.println("  " + r));
        String id = ler("Nº da reclamação a tratar (Enter para voltar)");
        if (id.isBlank()) {
            return;
        }
        Reclamacao r = reclamacoes.obter(Integer.parseInt(id));
        if (r.getEstado() == EstadoReclamacao.PROVA_ADICIONAL) {
            reclamacoes.completarProva(u, r.getId(), ler("Mais informação"), ler("Mais provas"));
        } else if (r.getEstado() == EstadoReclamacao.RECUSADA) {
            reclamacoes.pedirRecurso(u, r.getId(), ler("Justificação do recurso"));
        } else {
            System.out.println("Nada a fazer nesta reclamação (" + r.getEstado() + ").");
            return;
        }
        System.out.println("Reclamação #" + r.getId() + ": " + r.getEstado());
    }

    // ============================================================ funcionário

    private void confirmarEntrada(Utilizador f) {
        base.objectos.stream().filter(o -> o.getEstado() == EstadoObjecto.PRE_REGISTADO
                && f.ehFuncionarioDe(o.getSector())).forEach(o -> System.out.println("  " + o));
        String codigo = ler("Código do objecto");
        boolean conforme = ler("O objecto está conforme o registo? (s/n)").equalsIgnoreCase("s");
        String local = conforme ? ler("Onde fica guardado") : "";
        String nota = conforme ? "" : ler("Motivo");
        Objecto o = registo.confirmarEntrada(f, codigo, conforme, local, nota);
        System.out.println("Objecto " + o.getCodigo() + ": " + o.getEstado());
    }

    private void reverReclamacoes(Utilizador f) {
        List<Reclamacao> lista = base.reclamacoes.stream()
                .filter(r -> r.getEstado() == EstadoReclamacao.EM_VERIFICACAO
                        && f.ehFuncionarioDe(r.getObjecto().getSector())).toList();
        if (lista.isEmpty()) {
            System.out.println("Nada para rever.");
            return;
        }
        for (Reclamacao r : lista) {
            System.out.println("\n  " + r);
            System.out.println("  Detalhe oculto guardado: " + r.getObjecto().getDetalheOculto());
            System.out.println("  Resposta do reclamante:  " + r.getResposta());
            System.out.println("  Provas: " + r.getProvas());
            if (ler("Aprovar? (s/n)").equalsIgnoreCase("s")) {
                boolean identidade = ler("Conferiu a identidade do reclamante? (s/n)").equalsIgnoreCase("s");
                reclamacoes.revisar(f, r.getId(), true, identidade, null);
            } else {
                reclamacoes.revisar(f, r.getId(), false, false, ler("Motivo da recusa"));
            }
            System.out.println("  -> " + r.getEstado());
        }
    }

    private void entregar(Utilizador f) {
        base.reclamacoes.stream().filter(r -> r.getEstado() == EstadoReclamacao.APROVADA
                && f.ehFuncionarioDe(r.getObjecto().getSector())).forEach(r -> System.out.println("  " + r
                + " | levantar até " + r.getPrazoLevantamento()));
        int id = Integer.parseInt(ler("Nº da reclamação"));
        boolean documento = ler("Conferiu o documento de identificação? (s/n)").equalsIgnoreCase("s");
        System.out.println("-> " + reclamacoes.entregar(f, id, documento).getEstado());
    }

    private void executarDestino(Utilizador f) {
        base.objectos.stream().filter(o -> o.getEstado() == EstadoObjecto.DESTINO_APROVADO
                && f.ehFuncionarioDe(o.getSector())).forEach(o -> System.out.println("  " + o + " | " + o.getDestino()));
        System.out.println("-> " + prazos.executarDestino(f, ler("Código do objecto")).getEstado());
    }

    // ============================================================ gestor

    private void decidirRecursos(Utilizador g) {
        List<Reclamacao> lista = base.reclamacoes.stream()
                .filter(r -> r.getEstado() == EstadoReclamacao.EM_RECURSO).toList();
        if (lista.isEmpty()) {
            System.out.println("Nenhum recurso pendente.");
        }
        for (Reclamacao r : lista) {
            System.out.println("\n  " + r + "\n  Resposta e recurso: " + r.getResposta());
            boolean aprovar = ler("Aprovar o recurso? (s/n)").equalsIgnoreCase("s");
            System.out.println("  -> " + reclamacoes.decidirRecurso(g, r.getId(), aprovar, ler("Motivo")).getEstado());
        }
    }

    private void aprovarDestino(Utilizador g) {
        base.objectos.stream().filter(o -> o.getEstado() == EstadoObjecto.NAO_RECLAMADO)
                .forEach(o -> System.out.println("  " + o + " | sugerido: " + o.getDestino()));
        String codigo = ler("Código do objecto");
        System.out.println("Opções: " + PrazoService.DESTINOS);
        System.out.println("-> " + prazos.aprovarDestino(g, codigo, ler("Destino")).getEstado());
    }

    private void relatorio() {
        System.out.println("  Objectos por estado:    " + Consultas.contarPorEstado(base.objectos));
        System.out.println("  Objectos por categoria: " + Consultas.contarPorCategoria(base.objectos));
        System.out.printf("  Taxa de devolução: %.0f%%%n", Consultas.taxaDevolucao(base.objectos));
    }

    // ============================================================ ajudas de consola

    /** Executa uma acção e mostra a mensagem de erro (em vez de fechar o programa) se algo falhar. */
    private void tentar(Runnable accao) {
        try {
            accao.run();
        } catch (RegraNegocioException | IllegalArgumentException | java.time.DateTimeException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private String ler(String pergunta) {
        System.out.print(pergunta + ": ");
        return teclado.hasNextLine() ? teclado.nextLine().trim() : "0";
    }

    /** Mostra uma lista numerada e devolve a opção escolhida. */
    private <T> T escolher(String pergunta, T[] opcoes) {
        for (int i = 0; i < opcoes.length; i++) {
            System.out.println("   " + (i + 1) + ") " + opcoes[i]);
        }
        int n = Integer.parseInt(ler(pergunta));
        if (n < 1 || n > opcoes.length) {
            throw new IllegalArgumentException("Opção fora da lista.");
        }
        return opcoes[n - 1];
    }
}
