import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import app.Demonstracao;
import funcional.Consultas;
import funcional.Texto;
import logico.MotorLogico;
import logico.Regras;
import model.Categoria;
import model.EstadoObjecto;
import model.EstadoReclamacao;
import model.Objecto;
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

/** Testes simples, sem bibliotecas. Execute a partir da pasta do projecto (scripts/testes.sh). */
public class TestesSistema {

    private static int certos = 0;
    private static int errados = 0;

    // cenário partilhado, recriado por cada teste
    private static BaseDados base;
    private static Relogio relogio;
    private static RegistoService registo;
    private static ReclamacaoService reclamacoes;
    private static PrazoService prazos;
    private static Utilizador ana, bruno, carla, secretaria, biblioteca, gestor;

    public static void main(String[] args) throws Exception {
        System.out.println("== Paradigma lógico ==");
        motorLogico();
        regrasDeNegocio();
        System.out.println("== Paradigma funcional ==");
        funcoes();
        System.out.println("== Paradigma orientado a objectos ==");
        maquinaDeEstados();
        fluxoDevolucao();
        regrasDeRegisto();
        recusaERecurso();
        prazosDoSistema();
        persistencia();
        System.out.println("== Concorrência (exclusão mútua mínima) ==");
        doisReclamantesAoMesmoTempo();

        System.out.println("\nResultado: " + certos + " certos, " + errados + " errados.");
        if (errados > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------ lógico

    private static void motorLogico() {
        MotorLogico m = new MotorLogico();
        m.carregar(List.of("pai(ze, ana). pai(ana, rui).",
                "avo(X, Z) :- pai(X, Y), pai(Y, Z).",
                "sem_filhos(X) :- pessoa(X), \\+ pai(X, Y).",
                "pessoa(ze). pessoa(ana). pessoa(rui)."));
        verificar("facto simples", m.prova("pai", "ze", "ana"));
        verificar("facto falso", !m.prova("pai", "ana", "ze"));
        verificar("regra com duas condições", m.prova("avo", "ze", "rui"));
        verificar("variável na pergunta", m.consultar("avo", "ze", "Quem").get(0).get("Quem").equals("rui"));
        verificar("negação (\\+)", m.prova("sem_filhos", "rui") && !m.prova("sem_filhos", "ana"));
    }

    private static void regrasDeNegocio() {
        verificar("telemóvel é sensível", Categoria.TELEMOVEL.isSensivel());
        verificar("chaves não são sensíveis", !Categoria.CHAVES.isSensivel());
        verificar("documento não exige foto", !Categoria.DOCUMENTO.isFotoObrigatoria());
        verificar("chaves exigem foto", Categoria.CHAVES.isFotoObrigatoria());
        verificar("prazo documento = 30 dias", Categoria.DOCUMENTO.getPrazoGuardaDias() == 30);
        verificar("prazo mochila = 90 dias", Categoria.MOCHILA.getPrazoGuardaDias() == 90);
        verificar("destino documento", Categoria.DOCUMENTO.destinoSugerido().equals("entregar_a_entidade_emissora"));
        verificar("destino carteira", Categoria.CARTEIRA.destinoSugerido().equals("destruicao_segura"));
        verificar("destino electrónico", Categoria.ELECTRONICO.destinoSugerido().equals("reciclagem"));
        verificar("destino vestuário", Categoria.VESTUARIO.destinoSugerido().equals("doacao"));
        verificar("correspondência: categoria + cor + local",
                Regras.correspondencia(List.of("mesma_categoria", "mesma_cor", "local_proximo")));
        verificar("só a categoria não chega", !Regras.correspondencia(List.of("mesma_categoria")));
        verificar("só a cor não chega (sem categoria)", !Regras.correspondencia(List.of("mesma_cor", "local_proximo")));
        verificar("prova: detalhe correcto", Regras.verificacaoSuficiente(List.of("detalhe_correcto")));
        verificar("prova: parcial sem provas falha", !Regras.verificacaoSuficiente(List.of("detalhe_parcial")));
        verificar("prova: parcial com provas passa",
                Regras.verificacaoSuficiente(List.of("detalhe_parcial", "tem_provas")));
    }

    // ------------------------------------------------------------ funcional

    private static void funcoes() {
        verificar("normalizar", Texto.normalizar("  Telemóvel AZUL ").equals("telemovel azul"));
        verificar("cobertura total", Texto.cobertura("leão atrás capa", "tem um leão atrás da capa") == 1.0);
        verificar("cobertura zero", Texto.cobertura("leão atrás capa", "é azul") == 0.0);
        verificar("dias úteis saltam o fim de semana",
                Consultas.somarDiasUteis(LocalDate.of(2026, 10, 30), 1).equals(LocalDate.of(2026, 11, 2)));
        preparar();
        Objecto o = encontrar(ana, Categoria.CHAVES, "Chaves com porta-chaves vermelho", "vermelho", Zona.BLOCO_A,
                secretaria, Sector.SECRETARIA);
        Objecto t = encontrar(ana, Categoria.TELEMOVEL, "Telemóvel Samsung azul", "azul", Zona.BIBLIOTECA,
                biblioteca, Sector.BIBLIOTECA);
        verificar("catálogo mostra os disponíveis", Consultas.catalogo(base.objectos).size() == 2);
        verificar("pesquisa por texto", Consultas.pesquisar(base.objectos, null, "vermelho").size() == 1);
        verificar("pesquisa não vê detalhe oculto", Consultas.pesquisar(base.objectos, null, "leao").isEmpty());
        verificar("pesquisa não vê descrição de sensível", Consultas.pesquisar(base.objectos, null, "samsung").isEmpty());
        verificar("contagem por estado", Consultas.contarPorEstado(base.objectos).get(EstadoObjecto.DISPONIVEL) == 2L);
        verificar("resumo público esconde dados sensíveis", t.resumoPublico().contains("dados reservados")
                && !t.resumoPublico().contains("Samsung") && !o.resumoPublico().contains("reservados"));
    }

    // ------------------------------------------------------------ OO

    private static void maquinaDeEstados() {
        verificar("PRE_REGISTADO -> DISPONIVEL", EstadoObjecto.PRE_REGISTADO.podePassarPara(EstadoObjecto.DISPONIVEL));
        verificar("PRE_REGISTADO -> DEVOLVIDO é proibido",
                !EstadoObjecto.PRE_REGISTADO.podePassarPara(EstadoObjecto.DEVOLVIDO));
        verificar("DEVOLVIDO é final", !EstadoObjecto.DEVOLVIDO.podePassarPara(EstadoObjecto.DISPONIVEL));
        verificar("NAO_RECLAMADO só vai para DESTINO_APROVADO",
                EstadoObjecto.NAO_RECLAMADO.podePassarPara(EstadoObjecto.DESTINO_APROVADO)
                        && !EstadoObjecto.NAO_RECLAMADO.podePassarPara(EstadoObjecto.DESTINO_CUMPRIDO));
        preparar();
        Objecto o = encontrar(ana, Categoria.CHAVES, "Chaves com porta-chaves vermelho", "vermelho", Zona.BLOCO_A,
                secretaria, Sector.SECRETARIA);
        try {
            o.mudarEstado(EstadoObjecto.DEVOLVIDO, "teste", "salto inválido", relogio.agora());
            verificar("objecto recusa mudança inválida", false);
        } catch (IllegalArgumentException e) {
            verificar("objecto recusa mudança inválida", true);
        }
    }

    private static void fluxoDevolucao() {
        preparar();
        Procura perda = registo.registarPerda(bruno, Categoria.TELEMOVEL, "Telemóvel Samsung azul com capa", "azul",
                "Samsung", Zona.BIBLIOTECA, relogio.agora().toLocalDate());
        Objecto o = registo.registarEncontrado(ana, Categoria.TELEMOVEL, "Telemóvel Samsung azul com capa", "azul",
                "Samsung", Zona.BIBLIOTECA, relogio.agora().minusHours(1), "foto.jpg",
                "autocolante de um leão atrás da capa", Sector.BIBLIOTECA);
        verificar("pré-registo tem código AP-", o.getCodigo().startsWith("AP-"));
        registo.confirmarEntrada(biblioteca, o.getCodigo(), true, "Armário 1", "");
        verificar("entrada confirmada", o.getEstado() == EstadoObjecto.DISPONIVEL);
        verificar("dono avisado da correspondência", bruno.getNotificacoes().stream()
                .anyMatch(n -> n.contains("Possível correspondência")));

        Reclamacao falsa = reclamacoes.submeter(carla, o.getCodigo(), "é um telemóvel azul", "", null);
        verificar("impostora precisa de prova adicional", falsa.getEstado() == EstadoReclamacao.PROVA_ADICIONAL);
        Reclamacao certa = reclamacoes.submeter(bruno, o.getCodigo(), "autocolante de um leão atrás da capa",
                "captura.png", perda);
        verificar("dono vai para verificação", certa.getEstado() == EstadoReclamacao.EM_VERIFICACAO);
        verificar("objecto reservado", o.getEstado() == EstadoObjecto.RESERVADO);
        verificar("segundo reclamante é recusado", falha(() ->
                reclamacoes.submeter(ana, o.getCodigo(), "autocolante leão", "x", null)));
        verificar("não se aprova sem conferir identidade", falha(() ->
                reclamacoes.revisar(biblioteca, certa.getId(), true, false, null)));
        verificar("funcionário de outro sector não revê", falha(() ->
                reclamacoes.revisar(secretaria, certa.getId(), true, true, null)));
        reclamacoes.revisar(biblioteca, certa.getId(), true, true, null);
        verificar("aprovada", certa.getEstado() == EstadoReclamacao.APROVADA);
        verificar("não se entrega sem conferir documento", falha(() ->
                reclamacoes.entregar(biblioteca, certa.getId(), false)));
        reclamacoes.entregar(biblioteca, certa.getId(), true);
        verificar("objecto devolvido", o.getEstado() == EstadoObjecto.DEVOLVIDO);
        verificar("procura encerrada", !perda.isActiva());
        verificar("histórico tem 4 linhas", o.getHistorico().size() == 4);
    }

    private static void regrasDeRegisto() {
        preparar();
        verificar("foto obrigatória (chaves)", falha(() -> registo.registarEncontrado(ana, Categoria.CHAVES,
                "Chaves com porta-chaves", "preto", "", Zona.BLOCO_A, relogio.agora().minusHours(1), "", "",
                Sector.SECRETARIA)));
        verificar("documento aceita sem foto", !falha(() -> registo.registarEncontrado(ana, Categoria.DOCUMENTO,
                "Bilhete de identidade", "", "", Zona.BLOCO_A, relogio.agora().minusHours(1), "", "110100200301B",
                Sector.SECRETARIA)));
        verificar("descrição curta recusada", falha(() -> registo.registarEncontrado(ana, Categoria.CHAVES, "ab",
                "preto", "", Zona.BLOCO_A, relogio.agora().minusHours(1), "f.jpg", "", Sector.SECRETARIA)));
        verificar("data no futuro recusada", falha(() -> registo.registarEncontrado(ana, Categoria.CHAVES,
                "Chaves com porta-chaves", "preto", "", Zona.BLOCO_A, relogio.agora().plusDays(1), "f.jpg", "",
                Sector.SECRETARIA)));
        Objecto doc = base.objectos.get(0);
        registo.confirmarEntrada(secretaria, doc.getCodigo(), true, "Cofre", "");
        verificar("titular do documento é avisado", bruno.getNotificacoes().stream()
                .anyMatch(n -> n.contains("Documento encontrado")));
        verificar("documento tem prazo de 30 dias", doc.getPrazoGuarda().equals(relogio.agora().toLocalDate().plusDays(30)));
    }

    private static void recusaERecurso() {
        preparar();
        Objecto o = encontrar(ana, Categoria.TELEMOVEL, "Telemóvel Samsung azul", "azul", Zona.BIBLIOTECA,
                biblioteca, Sector.BIBLIOTECA);
        Reclamacao r = reclamacoes.submeter(bruno, o.getCodigo(), "tem um autocolante de leão atrás da capa",
                "captura.png", null);
        verificar("recusa exige motivo", falha(() -> reclamacoes.revisar(biblioteca, r.getId(), false, false, "")));
        reclamacoes.revisar(biblioteca, r.getId(), false, false, "Dados não conferem.");
        verificar("recusada e objecto livre", r.getEstado() == EstadoReclamacao.RECUSADA
                && o.getEstado() == EstadoObjecto.DISPONIVEL);
        reclamacoes.pedirRecurso(bruno, r.getId(), "Tenho o recibo de compra.");
        verificar("em recurso", r.getEstado() == EstadoReclamacao.EM_RECURSO);
        verificar("estudante não decide recurso", falha(() -> reclamacoes.decidirRecurso(ana, r.getId(), true, "ok")));
        reclamacoes.decidirRecurso(gestor, r.getId(), true, "Recibo confere.");
        verificar("recurso aprovado", r.getEstado() == EstadoReclamacao.APROVADA);
        reclamacoes.entregar(biblioteca, r.getId(), true);
        verificar("entregue após recurso", o.getEstado() == EstadoObjecto.DEVOLVIDO);
    }

    private static void prazosDoSistema() {
        preparar();
        Objecto pre = registo.registarEncontrado(ana, Categoria.CHAVES, "Chaves com porta-chaves", "preto", "",
                Zona.BLOCO_A, relogio.agora().minusHours(1), "f.jpg", "", Sector.SECRETARIA);
        relogio.avancarHoras(49);
        prazos.executarRotina();
        verificar("pré-registo expira às 48 h", pre.getEstado() == EstadoObjecto.CANCELADO);

        Objecto carteira = encontrar(ana, Categoria.CARTEIRA, "Carteira castanha de couro", "castanho", Zona.CANTINA,
                secretaria, Sector.SECRETARIA);
        relogio.avancarDias(80);
        prazos.executarRotina();
        verificar("aviso de fim de prazo enviado", carteira.isAvisoPrazoEnviado());
        relogio.avancarDias(12);
        prazos.executarRotina();
        verificar("passou a NAO_RECLAMADO", carteira.getEstado() == EstadoObjecto.NAO_RECLAMADO);
        verificar("destino sugerido pelas regras", "destruicao_segura".equals(carteira.getDestino()));
        verificar("não se executa sem aprovação", falha(() -> prazos.executarDestino(secretaria, carteira.getCodigo())));
        verificar("destino inválido recusado", falha(() -> prazos.aprovarDestino(gestor, carteira.getCodigo(), "queimar")));
        prazos.aprovarDestino(gestor, carteira.getCodigo(), "destruicao_segura");
        prazos.executarDestino(secretaria, carteira.getCodigo());
        verificar("destino cumprido", carteira.getEstado() == EstadoObjecto.DESTINO_CUMPRIDO);
    }

    private static void persistencia() throws Exception {
        preparar();
        encontrar(ana, Categoria.CHAVES, "Chaves com porta-chaves vermelho", "vermelho", Zona.BLOCO_A, secretaria,
                Sector.SECRETARIA);
        Path f = Files.createTempFile("base", ".dat");
        base.guardar(f);
        BaseDados lida = BaseDados.carregar(f);
        Files.delete(f);
        verificar("dados guardados e lidos", lida.objectos.size() == 1 && lida.utilizador("20230001") != null);
        verificar("palavra-passe continua a funcionar", lida.entrar("20230001", "1234") != null
                && lida.entrar("20230001", "errada") == null);
    }

    // ------------------------------------------------------------ concorrência

    /** 50 vezes, dois reclamantes tentam reservar o mesmo objecto ao mesmo tempo. Só um pode conseguir. */
    private static void doisReclamantesAoMesmoTempo() throws Exception {
        boolean sempreUm = true;
        for (int i = 0; i < 50; i++) {
            preparar();
            Objecto o = encontrar(ana, Categoria.CHAVES, "Chaves com porta-chaves vermelho", "vermelho",
                    Zona.BLOCO_A, secretaria, Sector.SECRETARIA);
            AtomicInteger reservas = new AtomicInteger();
            Runnable tentativa1 = () -> tentar(bruno, o, reservas);
            Runnable tentativa2 = () -> tentar(carla, o, reservas);
            Thread t1 = new Thread(tentativa1);
            Thread t2 = new Thread(tentativa2);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            long emVerificacao = base.reclamacoes.stream()
                    .filter(r -> r.getEstado() == EstadoReclamacao.EM_VERIFICACAO).count();
            if (reservas.get() != 1 || emVerificacao != 1 || o.getEstado() != EstadoObjecto.RESERVADO) {
                sempreUm = false;
            }
        }
        verificar("só um dos dois fica com a reserva (50 repetições)", sempreUm);
    }

    private static void tentar(Utilizador u, Objecto o, AtomicInteger reservas) {
        try {
            reclamacoes.submeter(u, o.getCodigo(), "autocolante de um leão atrás da capa", "foto.jpg", null);
            reservas.incrementAndGet();
        } catch (RegraNegocioException e) {
            // o outro chegou primeiro: é o comportamento esperado
        }
    }

    // ------------------------------------------------------------ ajudas

    private static void preparar() {
        base = new BaseDados();
        relogio = new Relogio();
        registo = new RegistoService(base, relogio);
        reclamacoes = new ReclamacaoService(base, relogio);
        prazos = new PrazoService(base, relogio);
        Demonstracao.criarContas(base);
        ana = base.utilizador("20230001");
        bruno = base.utilizador("20230002");
        carla = base.utilizador("20230003");
        secretaria = base.utilizador("F001");
        biblioteca = base.utilizador("F002");
        gestor = base.utilizador("G001");
    }

    /** Regista um objecto e já o deixa DISPONIVEL no catálogo. */
    private static Objecto encontrar(Utilizador achador, Categoria c, String descricao, String cor, Zona zona,
                                     Utilizador funcionario, Sector sector) {
        Objecto o = registo.registarEncontrado(achador, c, descricao, cor, "", zona, relogio.agora().minusHours(1),
                "foto.jpg", "autocolante de um leão atrás da capa", sector);
        registo.confirmarEntrada(funcionario, o.getCodigo(), true, "Prateleira 1", "");
        return o;
    }

    private static boolean falha(Runnable accao) {
        try {
            accao.run();
            return false;
        } catch (RegraNegocioException e) {
            return true;
        }
    }

    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            certos++;
            System.out.println("  OK    " + descricao);
        } else {
            errados++;
            System.out.println("  ERRO  " + descricao);
        }
    }
}
