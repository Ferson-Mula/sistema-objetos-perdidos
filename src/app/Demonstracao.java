package app;

import java.time.LocalDate;
import java.util.List;

import model.Categoria;
import model.Destino;
import model.FuncionarioSector;
import model.Gestor;
import model.Objecto;
import model.Procura;
import model.Reclamacao;
import model.RegraNegocioException;
import model.Sector;
import model.Utilizador;
import model.Zona;
import repository.BaseDados;
import util.Relogio;

/** Percorre os quatro processos (P01 a P04) com dados fictícios e mostra o que acontece. */
public class Demonstracao {

    public void executar() {
        Relogio relogio = new Relogio();
        Sistema s = new Sistema(new BaseDados(), relogio, true);
        DadosIniciais.popular(s);

        Utilizador ana = s.base.utilizadorPorNumero("20230001");
        Utilizador bruno = s.base.utilizadorPorNumero("20230002");
        Utilizador carla = s.base.utilizadorPorNumero("20230003");
        FuncionarioSector secretaria = (FuncionarioSector) s.base.utilizadorPorNumero("F001");
        FuncionarioSector biblioteca = (FuncionarioSector) s.base.utilizadorPorNumero("F002");
        Gestor gestor = (Gestor) s.base.utilizadorPorNumero("G001");

        titulo("P02 - Bruno regista a perda de um telemóvel (ainda não há nada no sistema)");
        Procura perdaBruno = s.registo.registarPerda(bruno, Categoria.TELEMOVEL, "Telemóvel Samsung azul com capa",
                "azul", "Samsung", Zona.BIBLIOTECA, relogio.agora().toLocalDate(),
                "autocolante de um leão atrás da capa");

        titulo("P01 - Ana encontra um telemóvel na biblioteca e regista-o");
        Objecto telemovel = s.registo.registarEncontrado(ana, Categoria.TELEMOVEL, "Telemóvel Samsung azul com capa",
                "azul", "Samsung", Zona.BIBLIOTECA, relogio.agora().minusHours(2), "foto_telemovel.jpg",
                "autocolante de um leão atrás da capa", Sector.BIBLIOTECA);
        passo("Código gerado: " + telemovel.getCodigo() + " | estado: " + telemovel.getEstado());

        titulo("P01 - Ana entrega na biblioteca; o funcionário confere e guarda");
        s.registo.confirmarEntrada(biblioteca, telemovel.getCodigo(), true, "Armário 1, prateleira A", "");
        passo("Estado: " + telemovel.getEstado() + " | prazo de guarda: " + telemovel.getPrazoGuarda());
        passo("Catálogo público: " + s.catalogo.resumoPublico(telemovel));

        titulo("P03 - Carla tenta reclamar o telemóvel mas não sabe o detalhe (impostora)");
        Reclamacao impostora = s.reclamacoes.submeter(carla, telemovel.getCodigo(), "é um telemóvel azul", "", null);
        passo(impostora.toString());

        titulo("P03 - Bruno reclama, responde ao desafio e junta prova");
        Reclamacao rBruno = s.reclamacoes.submeter(bruno, telemovel.getCodigo(),
                "Tem um autocolante de um leão atrás da capa", "captura_do_ecra_antigo.png", perdaBruno);
        passo(rBruno.toString());

        titulo("P03 - Carla (ou outra pessoa) tenta reclamar o mesmo objecto enquanto está reservado");
        try {
            s.reclamacoes.submeter(carla, telemovel.getCodigo(), "autocolante de leão", "foto", null);
        } catch (RegraNegocioException e) {
            passo("Recusado: " + e.getMessage());
        }

        titulo("P03 - O funcionário confere a identidade e aprova; depois entrega");
        s.reclamacoes.revisar(biblioteca, rBruno.getId(), true, true, null);
        s.reclamacoes.entregar(biblioteca, rBruno.getId(), true);
        passo("Estado do objecto: " + telemovel.getEstado() + " | procura do Bruno: " + perdaBruno.getEstado());
        for (int i = 0; i < telemovel.getHistorico().size(); i++) {
            passo("   " + telemovel.getHistorico().get(i));
        }

        titulo("P01/P02 - Objectos que ninguém vem buscar (carteira) e uma procura sem resultado");
        Objecto carteira = s.registo.registarEncontrado(bruno, Categoria.CARTEIRA, "Carteira castanha de couro",
                "castanho", "", Zona.CANTINA, relogio.agora().minusDays(1), "foto_carteira.jpg", "", Sector.SECRETARIA);
        s.registo.confirmarEntrada(secretaria, carteira.getCodigo(), true, "Cofre, gaveta 2", "");
        Objecto chaves = s.registo.registarEncontrado(carla, Categoria.CHAVES, "Chaves com porta-chaves vermelho",
                "vermelho", "", Zona.BLOCO_A, relogio.agora().minusDays(1), "foto_chaves.jpg", "", Sector.SECRETARIA);
        s.registo.confirmarEntrada(secretaria, chaves.getCodigo(), false, "", "As chaves não foram entregues.");
        passo("Chaves: " + chaves.getEstado());
        s.registo.registarPerda(ana, Categoria.MOCHILA, "Mochila preta com fecho azul", "preta", "Nike",
                Zona.BLOCO_B, LocalDate.now(), "");

        titulo("P04 - Passam 80 dias e a rotina de prazos corre (a mão, pelo Gestor)");
        relogio.avancarDias(80);
        mostrar(s.prazos.executarRotina());

        titulo("P04 - Passam mais 12 dias: a carteira passa a NÃO_RECLAMADO");
        relogio.avancarDias(12);
        mostrar(s.prazos.executarRotina());
        passo("Carteira: " + carteira.getEstado() + " | destino sugerido: " + carteira.getDestino());
        s.prazos.aprovarDestino(gestor, carteira.getCodigo(), Destino.DESTRUICAO_SEGURA);
        s.prazos.executarDestino(secretaria, carteira.getCodigo());
        passo("Carteira: " + carteira.getEstado());

        titulo("Relatório do Gestor");
        mostrar(s.relatorios.gerar());
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
        for (int i = 0; i < linhas.size(); i++) {
            System.out.println("   " + linhas.get(i));
        }
    }
}
