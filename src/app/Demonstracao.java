package app;

import java.time.LocalDate;
import java.util.List;

import funcional.Consultas;
import model.Categoria;
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

/** Percorre os quatro processos (P01 a P04) com dados fictícios e mostra o que acontece. */
public class Demonstracao {

    public static final String SENHA = "1234";

    /** Cria as contas de teste. A palavra-passe de todas é 1234. */
    public static void criarContas(BaseDados base) {
        base.criarUtilizador("20230001", "Ana Macie", "ana.macie@ujc.ac.mz", "110100200300A", SENHA,
                Perfil.ESTUDANTE, null);
        base.criarUtilizador("20230002", "Bruno Tembe", "bruno.tembe@ujc.ac.mz", "110100200301B", SENHA,
                Perfil.ESTUDANTE, null);
        base.criarUtilizador("20230003", "Carla Nhaca", "carla.nhaca@ujc.ac.mz", "110100200302C", SENHA,
                Perfil.ESTUDANTE, null);
        base.criarUtilizador("D001", "Dr. Daniel Cuamba", "daniel.cuamba@ujc.ac.mz", "110100200303D", SENHA,
                Perfil.DOCENTE, null);
        base.criarUtilizador("F001", "Marta Sitoe", "marta.sitoe@ujc.ac.mz", "110100200304E", SENHA,
                Perfil.FUNCIONARIO, Sector.SECRETARIA);
        base.criarUtilizador("F002", "Carlos Mondlane", "carlos.mondlane@ujc.ac.mz", "110100200305F", SENHA,
                Perfil.FUNCIONARIO, Sector.BIBLIOTECA);
        base.criarUtilizador("G001", "Direcção de Serviços", "servicos@ujc.ac.mz", "110100200306G", SENHA,
                Perfil.GESTOR, null);
    }

    public void executar() {
        BaseDados base = new BaseDados();
        base.setMostrarNotificacoes(true);
        Relogio relogio = new Relogio();
        RegistoService registo = new RegistoService(base, relogio);
        ReclamacaoService reclamacoes = new ReclamacaoService(base, relogio);
        PrazoService prazos = new PrazoService(base, relogio);
        criarContas(base);

        Utilizador ana = base.utilizador("20230001");
        Utilizador bruno = base.utilizador("20230002");
        Utilizador carla = base.utilizador("20230003");
        Utilizador secretaria = base.utilizador("F001");
        Utilizador biblioteca = base.utilizador("F002");
        Utilizador gestor = base.utilizador("G001");

        titulo("P02 - Bruno regista a perda de um telemóvel (ainda não há nada no sistema)");
        Procura perdaBruno = registo.registarPerda(bruno, Categoria.TELEMOVEL, "Telemóvel Samsung azul com capa",
                "azul", "Samsung", Zona.BIBLIOTECA, relogio.agora().toLocalDate());

        titulo("P01 - Ana encontra um telemóvel na biblioteca e regista-o");
        Objecto telemovel = registo.registarEncontrado(ana, Categoria.TELEMOVEL, "Telemóvel Samsung azul com capa",
                "azul", "Samsung", Zona.BIBLIOTECA, relogio.agora().minusHours(2), "foto_telemovel.jpg",
                "autocolante de um leão atrás da capa", Sector.BIBLIOTECA);
        passo("Código: " + telemovel.getCodigo() + " | estado: " + telemovel.getEstado());

        titulo("P01 - Ana entrega na biblioteca; o funcionário confere e guarda (o Bruno é avisado)");
        registo.confirmarEntrada(biblioteca, telemovel.getCodigo(), true, "Armário 1, prateleira A", "");
        passo("Estado: " + telemovel.getEstado() + " | prazo de guarda: " + telemovel.getPrazoGuarda());
        passo("Catálogo público: " + telemovel.resumoPublico());

        titulo("Lógico - perguntas às regras (regras/regras.pl)");
        passo("Telemóvel é sensível? " + Categoria.TELEMOVEL.isSensivel() + " | prazo de guarda: "
                + Categoria.TELEMOVEL.getPrazoGuardaDias() + " dias | destino sugerido: "
                + Categoria.TELEMOVEL.destinoSugerido());
        passo("Documento é sensível? " + Categoria.DOCUMENTO.isSensivel() + " | prazo de guarda: "
                + Categoria.DOCUMENTO.getPrazoGuardaDias() + " dias | foto obrigatória? "
                + Categoria.DOCUMENTO.isFotoObrigatoria());
        passo("Factos entre a procura do Bruno e o telemóvel: " + Consultas.caracteristicas(perdaBruno, telemovel));

        titulo("P03 - Carla tenta reclamar o telemóvel mas não sabe o detalhe (impostora)");
        Reclamacao impostora = reclamacoes.submeter(carla, telemovel.getCodigo(), "é um telemóvel azul", "", null);
        passo(impostora.toString());

        titulo("P03 - Bruno reclama, responde ao desafio e junta prova");
        Reclamacao rBruno = reclamacoes.submeter(bruno, telemovel.getCodigo(),
                "Tem um autocolante de um leão atrás da capa", "captura_do_ecra_antigo.png", perdaBruno);
        passo(rBruno.toString());

        titulo("P03 - Outra pessoa tenta reclamar o mesmo objecto enquanto está reservado");
        try {
            reclamacoes.submeter(ana, telemovel.getCodigo(), "autocolante de leão", "foto", null);
        } catch (RegraNegocioException e) {
            passo("Recusado: " + e.getMessage());
        }

        titulo("P03 - O funcionário confere a identidade, aprova e entrega");
        reclamacoes.revisar(biblioteca, rBruno.getId(), true, true, null);
        reclamacoes.entregar(biblioteca, rBruno.getId(), true);
        passo("Estado do objecto: " + telemovel.getEstado() + " | procura do Bruno activa? " + perdaBruno.isActiva());
        telemovel.getHistorico().forEach(h -> passo("   " + h));

        titulo("P01/P02 - Objectos que ninguém vem buscar e uma procura sem resultado");
        Objecto carteira = registo.registarEncontrado(bruno, Categoria.CARTEIRA, "Carteira castanha de couro",
                "castanho", "", Zona.CANTINA, relogio.agora().minusDays(1), "foto_carteira.jpg", "",
                Sector.SECRETARIA);
        registo.confirmarEntrada(secretaria, carteira.getCodigo(), true, "Cofre, gaveta 2", "");
        Objecto chaves = registo.registarEncontrado(carla, Categoria.CHAVES, "Chaves com porta-chaves vermelho",
                "vermelho", "", Zona.BLOCO_A, relogio.agora().minusDays(1), "foto_chaves.jpg", "",
                Sector.SECRETARIA);
        registo.confirmarEntrada(secretaria, chaves.getCodigo(), false, "", "As chaves não foram entregues.");
        passo("Chaves: " + chaves.getEstado());
        registo.registarPerda(ana, Categoria.MOCHILA, "Mochila preta com fecho azul", "preta", "Nike",
                Zona.BLOCO_B, LocalDate.now());

        titulo("P04 - Passam 80 dias e a rotina de prazos corre (a pedido do Gestor)");
        relogio.avancarDias(80);
        mostrar(prazos.executarRotina());

        titulo("P04 - Passam mais 12 dias: a carteira passa a NAO_RECLAMADO");
        relogio.avancarDias(12);
        mostrar(prazos.executarRotina());
        passo("Carteira: " + carteira.getEstado() + " | destino sugerido: " + carteira.getDestino());
        prazos.aprovarDestino(gestor, carteira.getCodigo(), "destruicao_segura");
        prazos.executarDestino(secretaria, carteira.getCodigo());
        passo("Carteira: " + carteira.getEstado());

        titulo("Funcional - estatísticas calculadas com streams");
        passo("Objectos por estado: " + Consultas.contarPorEstado(base.objectos));
        passo("Objectos por categoria: " + Consultas.contarPorCategoria(base.objectos));
        passo(String.format("Taxa de devolução: %.0f%%", Consultas.taxaDevolucao(base.objectos)));

        System.out.println();
        System.out.println("Demonstração concluída.");
    }

    private void titulo(String t) {
        System.out.println();
        System.out.println(">>> " + t);
    }

    private void passo(String t) {
        System.out.println("   " + t);
    }

    private void mostrar(List<String> linhas) {
        linhas.forEach(this::passo);
    }
}
