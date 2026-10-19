package tests;

import java.time.LocalDate;

import app.DadosIniciais;
import app.Sistema;
import model.Categoria;
import model.Correspondencia;
import model.EstadoObjecto;
import model.EstadoProcura;
import model.EstadoReclamacao;
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

/**
 * Testes simples, sem bibliotecas externas. Execute com scripts/testar.sh (ou testar.bat).
 * Termina com código 1 se algum teste falhar.
 */
public class TestesSistema {

    private static int passaram = 0;
    private static int falharam = 0;

    private Sistema s;
    private Relogio relogio;
    private Utilizador ana;
    private Utilizador bruno;
    private Utilizador carla;
    private FuncionarioSector secretaria;
    private FuncionarioSector biblioteca;
    private Gestor gestor;

    public static void main(String[] args) {
        TestesSistema t = new TestesSistema();
        t.registoDevolveCodigoEEstadoPreRegistado();
        t.fotoObrigatoriaParaTelemovel();
        t.duplicadoERecusado();
        t.confirmarEntradaTornaDisponivelEAvisaProcura();
        t.categoriaDiferenteNaoCorresponde();
        t.catalogoPublicoEscondeObjectosSensiveis();
        t.reclamacaoSemDetalheExigeProvaAdicional();
        t.reclamacaoComDetalheReservaObjecto();
        t.segundoReclamanteERecusado();
        t.aprovarExigeIdentidade();
        t.entregaFechaProcuraERegistaMovimento();
        t.recusaLibertaObjectoERecursoFunciona();
        t.transicaoInvalidaELancaExcecao();
        t.preRegistoExpiraEm48Horas();
        t.prazoDeGuardaLevaANaoReclamadoEDestino();
        t.destinoSoComAprovacao();
        t.relatorioContaDevolucoes();

        System.out.println();
        System.out.println("Passaram: " + passaram + " | Falharam: " + falharam);
        if (falharam > 0) {
            System.exit(1);
        }
    }

    // ---- preparação

    private void novo() {
        relogio = new Relogio();
        s = new Sistema(new BaseDados(), relogio, false);
        DadosIniciais.popular(s);
        ana = s.base.utilizadorPorNumero("20230001");
        bruno = s.base.utilizadorPorNumero("20230002");
        carla = s.base.utilizadorPorNumero("20230003");
        secretaria = (FuncionarioSector) s.base.utilizadorPorNumero("F001");
        biblioteca = (FuncionarioSector) s.base.utilizadorPorNumero("F002");
        gestor = (Gestor) s.base.utilizadorPorNumero("G001");
    }

    private Objecto telemovelDisponivel() {
        Objecto o = s.registo.registarEncontrado(ana, Categoria.TELEMOVEL, "Telemóvel Samsung azul com capa", "azul",
                "Samsung", Zona.BIBLIOTECA, relogio.agora().minusHours(1), "foto.jpg",
                "autocolante de um leão atrás da capa", Sector.BIBLIOTECA);
        s.registo.confirmarEntrada(biblioteca, o.getCodigo(), true, "Armário 1", "");
        return o;
    }

    private Reclamacao reclamacaoValida(Objecto o) {
        return s.reclamacoes.submeter(bruno, o.getCodigo(), "autocolante de um leão atrás da capa",
                "captura.png", null);
    }

    // ---- testes

    void registoDevolveCodigoEEstadoPreRegistado() {
        novo();
        Objecto o = s.registo.registarEncontrado(ana, Categoria.MOCHILA, "Mochila preta grande", "preta", "Nike",
                Zona.BLOCO_A, relogio.agora().minusHours(1), "foto.jpg", "", Sector.SECRETARIA);
        verificar("P01: código no formato OBJ-ano-0001", o.getCodigo().matches("OBJ-\\d{4}-0001"));
        verificar("P01: estado inicial PRÉ-REGISTADO", o.getEstado() == EstadoObjecto.PRE_REGISTADO);
    }

    void fotoObrigatoriaParaTelemovel() {
        novo();
        verificarErro("RN02: telemóvel sem foto é recusado", new Runnable() {
            public void run() {
                s.registo.registarEncontrado(ana, Categoria.TELEMOVEL, "Telemóvel preto", "preto", "", Zona.BLOCO_A,
                        relogio.agora().minusHours(1), "", "", Sector.SECRETARIA);
            }
        });
        Objecto doc = s.registo.registarEncontrado(ana, Categoria.DOCUMENTO, "Bilhete de identidade", "", "",
                Zona.BLOCO_A, relogio.agora().minusHours(1), "", "", Sector.SECRETARIA);
        verificar("RN02: documento pode ser registado sem foto", doc != null);
    }

    void duplicadoERecusado() {
        novo();
        s.registo.registarEncontrado(ana, Categoria.MOCHILA, "Mochila preta grande", "preta", "", Zona.BLOCO_A,
                relogio.agora().minusHours(1), "f.jpg", "", Sector.SECRETARIA);
        verificarErro("P01: mesmo objecto registado duas vezes é recusado", new Runnable() {
            public void run() {
                s.registo.registarEncontrado(ana, Categoria.MOCHILA, "mochila PRETA grande", "preta", "",
                        Zona.BLOCO_A, relogio.agora().minusHours(1), "f.jpg", "", Sector.SECRETARIA);
            }
        });
    }

    void confirmarEntradaTornaDisponivelEAvisaProcura() {
        novo();
        s.registo.registarPerda(bruno, Categoria.TELEMOVEL, "Telemóvel Samsung azul com capa", "azul", "Samsung",
                Zona.BIBLIOTECA, LocalDate.now(), "");
        int antes = s.notificacoes.doUtilizador(bruno).size();
        Objecto o = telemovelDisponivel();
        verificar("P01: após confirmar, objecto DISPONÍVEL", o.getEstado() == EstadoObjecto.DISPONIVEL);
        verificar("P01: prazo de guarda de 90 dias", o.getPrazoGuarda().equals(LocalDate.now().plusDays(90)));
        verificar("P02: dono da procura é avisado do novo objecto",
                s.notificacoes.doUtilizador(bruno).size() == antes + 1);
    }

    void categoriaDiferenteNaoCorresponde() {
        novo();
        Procura p = s.registo.registarPerda(bruno, Categoria.CHAVES, "Chaves azuis", "azul", "", Zona.BIBLIOTECA,
                LocalDate.now(), "");
        Objecto o = telemovelDisponivel();
        verificar("P02: categoria diferente tem pontuação 0", s.correspondencia.pontuar(p, o) == 0);
        verificar("P02: sem candidatos", s.correspondencia.candidatosPara(p).isEmpty());
    }

    void catalogoPublicoEscondeObjectosSensiveis() {
        novo();
        Objecto o = telemovelDisponivel();
        String resumo = s.catalogo.resumoPublico(o);
        verificar("RN06: catálogo não mostra a descrição do telemóvel", !resumo.contains("Samsung azul com capa"));
        verificar("RN04: catálogo não mostra o detalhe oculto", !resumo.contains("leão"));
        verificar("RN06: catálogo não mostra a foto", !resumo.contains("foto.jpg"));
    }

    void reclamacaoSemDetalheExigeProvaAdicional() {
        novo();
        Objecto o = telemovelDisponivel();
        Reclamacao r = s.reclamacoes.submeter(carla, o.getCodigo(), "é um telemóvel azul", "", null);
        verificar("P03: sem o detalhe, fica à espera de prova adicional", r.getEstado() == EstadoReclamacao.PROVA_ADICIONAL);
        verificar("P03: objecto não é reservado", o.getEstado() == EstadoObjecto.DISPONIVEL);
    }

    void reclamacaoComDetalheReservaObjecto() {
        novo();
        Objecto o = telemovelDisponivel();
        Reclamacao r = reclamacaoValida(o);
        verificar("P03: com detalhe correcto segue para verificação", r.getEstado() == EstadoReclamacao.EM_VERIFICACAO);
        verificar("P03: objecto RESERVADO", o.getEstado() == EstadoObjecto.RESERVADO);
    }

    void segundoReclamanteERecusado() {
        novo();
        Objecto o = telemovelDisponivel();
        reclamacaoValida(o);
        verificarErro("PR10: segundo reclamante é recusado enquanto o objecto está reservado", new Runnable() {
            public void run() {
                s.reclamacoes.submeter(carla, s.base.getObjectos().get(0).getCodigo(), "leão atrás da capa", "x", null);
            }
        });
    }

    void aprovarExigeIdentidade() {
        novo();
        Objecto o = telemovelDisponivel();
        final Reclamacao r = reclamacaoValida(o);
        verificarErro("RN11: aprovar sem conferir identidade é recusado", new Runnable() {
            public void run() {
                s.reclamacoes.revisar(biblioteca, r.getId(), true, false, null);
            }
        });
        verificarErro("P03: funcionário de outro sector não pode rever", new Runnable() {
            public void run() {
                s.reclamacoes.revisar(secretaria, r.getId(), true, true, null);
            }
        });
    }

    void entregaFechaProcuraERegistaMovimento() {
        novo();
        Procura p = s.registo.registarPerda(bruno, Categoria.TELEMOVEL, "Telemóvel Samsung azul", "azul", "Samsung",
                Zona.BIBLIOTECA, LocalDate.now(), "");
        Objecto o = telemovelDisponivel();
        Reclamacao r = s.reclamacoes.submeter(bruno, o.getCodigo(), "autocolante de um leão atrás da capa", "p.png", p);
        s.reclamacoes.revisar(biblioteca, r.getId(), true, true, null);
        s.reclamacoes.entregar(biblioteca, r.getId(), true);
        verificar("P03: objecto DEVOLVIDO", o.getEstado() == EstadoObjecto.DEVOLVIDO);
        verificar("P03: reclamação CONCLUÍDA", r.getEstado() == EstadoReclamacao.CONCLUIDA);
        verificar("P03: procura associada ENCERRADA", p.getEstado() == EstadoProcura.ENCERRADA);
        Object ultimo = o.getHistorico().get(o.getHistorico().size() - 1);
        verificar("RN15: a entrega fica registada com o nome do reclamante", ultimo.toString().contains("Bruno Tembe"));
    }

    void recusaLibertaObjectoERecursoFunciona() {
        novo();
        Objecto o = telemovelDisponivel();
        Reclamacao r = reclamacaoValida(o);
        s.reclamacoes.revisar(biblioteca, r.getId(), false, false, "Documento não corresponde.");
        verificar("P03: recusa liberta o objecto", o.getEstado() == EstadoObjecto.DISPONIVEL);
        s.reclamacoes.pedirRecurso(bruno, r.getId(), "Tenho o documento original.");
        verificar("P03: recurso volta a reservar o objecto", o.getEstado() == EstadoObjecto.RESERVADO);
        s.reclamacoes.decidirRecurso(gestor, r.getId(), true, "Documento verificado.");
        verificar("RN14: recurso aprovado pelo Gestor", r.getEstado() == EstadoReclamacao.APROVADA);
    }

    void transicaoInvalidaELancaExcecao() {
        novo();
        final Objecto o = s.registo.registarEncontrado(ana, Categoria.MOCHILA, "Mochila preta grande", "preta", "",
                Zona.BLOCO_A, relogio.agora().minusHours(1), "f.jpg", "", Sector.SECRETARIA);
        verificarErro("RN16: PRÉ-REGISTADO não pode passar a DEVOLVIDO", new Runnable() {
            public void run() {
                o.mudarEstado(EstadoObjecto.DEVOLVIDO, "teste", "teste", "", relogio.agora());
            }
        });
    }

    void preRegistoExpiraEm48Horas() {
        novo();
        Objecto o = s.registo.registarEncontrado(ana, Categoria.MOCHILA, "Mochila preta grande", "preta", "",
                Zona.BLOCO_A, relogio.agora().minusHours(1), "f.jpg", "", Sector.SECRETARIA);
        relogio.avancarHoras(49);
        s.prazos.executarRotina();
        verificar("RN05: pré-registo sem entrega é cancelado", o.getEstado() == EstadoObjecto.CANCELADO);
    }

    void prazoDeGuardaLevaANaoReclamadoEDestino() {
        novo();
        Objecto o = telemovelDisponivel();
        relogio.avancarDias(80);
        s.prazos.executarRotina();
        verificar("RN18: aviso de prazo enviado", o.isAvisoPrazoEnviado());
        verificar("RN18: continua DISPONÍVEL antes do fim do prazo", o.getEstado() == EstadoObjecto.DISPONIVEL);
        relogio.avancarDias(12);
        s.prazos.executarRotina();
        verificar("RN17: passado o prazo, objecto NÃO_RECLAMADO", o.getEstado() == EstadoObjecto.NAO_RECLAMADO);
    }

    void destinoSoComAprovacao() {
        novo();
        final Objecto o = telemovelDisponivel();
        relogio.avancarDias(95);
        s.prazos.executarRotina();
        verificarErro("RN19: executar destino sem aprovação é recusado", new Runnable() {
            public void run() {
                s.prazos.executarDestino(biblioteca, o.getCodigo());
            }
        });
        s.prazos.aprovarDestino(gestor, o.getCodigo(), model.Destino.DESTRUICAO_SEGURA);
        s.prazos.executarDestino(biblioteca, o.getCodigo());
        verificar("RN19: destino cumprido depois de aprovado", o.getEstado() == EstadoObjecto.DESTINO_CUMPRIDO);
    }

    void relatorioContaDevolucoes() {
        novo();
        Objecto o = telemovelDisponivel();
        Reclamacao r = reclamacaoValida(o);
        s.reclamacoes.revisar(biblioteca, r.getId(), true, true, null);
        s.reclamacoes.entregar(biblioteca, r.getId(), true);
        boolean achou = false;
        java.util.List<String> linhas = s.relatorios.gerar();
        for (int i = 0; i < linhas.size(); i++) {
            if (linhas.get(i).startsWith("Taxa de devolução: 100.0")) {
                achou = true;
            }
        }
        verificar("RN20: relatório mostra taxa de devolução de 100 %", achou);
    }

    // ---- apoio

    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            passaram++;
            System.out.println("  OK    " + descricao);
        } else {
            falharam++;
            System.out.println("  FALHA " + descricao);
        }
    }

    private static void verificarErro(String descricao, Runnable accao) {
        try {
            accao.run();
            falharam++;
            System.out.println("  FALHA " + descricao + " (não lançou erro)");
        } catch (RegraNegocioException e) {
            passaram++;
            System.out.println("  OK    " + descricao);
        }
    }
}
