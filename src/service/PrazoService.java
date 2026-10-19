package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import model.Destino;
import model.EstadoObjecto;
import model.EstadoProcura;
import model.EstadoReclamacao;
import model.FuncionarioSector;
import model.Gestor;
import model.Objecto;
import model.Procura;
import model.Reclamacao;
import model.RegraNegocioException;
import model.Utilizador;
import repository.BaseDados;
import util.Relogio;

/**
 * P04: prazos e destino final (RN05, RN07, RN13, RN17 a RN19).
 * Na fase 1 a rotina não corre sozinha: alguém tem de a executar (menu do Gestor).
 */
public class PrazoService {

    public static final int HORAS_PRE_REGISTO = 48;
    public static final int DIAS_AVISO_PRAZO = 15;
    public static final int DIAS_AVISO_PROCURA = 7;

    private final BaseDados base;
    private final Relogio relogio;
    private final NotificacaoService notificacoes;
    private final CorrespondenciaService correspondencia;

    public PrazoService(BaseDados base, Relogio relogio, NotificacaoService notificacoes,
                        CorrespondenciaService correspondencia) {
        this.base = base;
        this.relogio = relogio;
        this.notificacoes = notificacoes;
        this.correspondencia = correspondencia;
    }

    /** Percorre todos os registos e aplica todos os prazos. Devolve o resumo do que fez. */
    public List<String> executarRotina() {
        List<String> resumo = new ArrayList<String>();
        LocalDateTime agora = relogio.agora();
        LocalDate hoje = agora.toLocalDate();

        int preRegistos = 0;
        int reservas = 0;
        int objectosAvisados = 0;
        int naoReclamados = 0;
        int procurasAvisadas = 0;
        int procurasExpiradas = 0;

        for (int i = 0; i < base.getObjectos().size(); i++) {
            Objecto o = base.getObjectos().get(i);
            if (o.getEstado() == EstadoObjecto.PRE_REGISTADO
                    && agora.isAfter(o.getCriadoEm().plusHours(HORAS_PRE_REGISTO))) {
                o.mudarEstado(EstadoObjecto.CANCELADO, "Sistema", "Pré-registo expirado",
                        "Sem entrega em " + HORAS_PRE_REGISTO + " h (RN05)", agora);
                notificacoes.enviar(o.getAchador(), "Pré-registo cancelado",
                        "O objecto " + o.getCodigo() + " não foi entregue em 48 horas.");
                preRegistos++;
            } else if (o.getEstado() == EstadoObjecto.DISPONIVEL) {
                long diasRestantes = ChronoUnit.DAYS.between(hoje, o.getPrazoGuarda());
                if (diasRestantes < 0) {
                    o.mudarEstado(EstadoObjecto.NAO_RECLAMADO, "Sistema", "Prazo de guarda expirado",
                            "Prazo era " + o.getPrazoGuarda(), agora);
                    o.setDestino(Destino.sugerir(o.getCategoria()));
                    notificarGestores("Objecto não reclamado", o.getCodigo() + ": destino sugerido "
                            + o.getDestino() + ". Aprove o destino final.");
                    naoReclamados++;
                } else if (diasRestantes <= DIAS_AVISO_PRAZO && !o.isAvisoPrazoEnviado()) {
                    avisarPrazo(o, diasRestantes);
                    o.setAvisoPrazoEnviado(true);
                    objectosAvisados++;
                }
            }
        }

        for (int i = 0; i < base.getReclamacoes().size(); i++) {
            Reclamacao r = base.getReclamacoes().get(i);
            if (r.getEstado() == EstadoReclamacao.APROVADA && hoje.isAfter(r.getPrazoLevantamento())) {
                r.setEstado(EstadoReclamacao.EXPIRADA);
                r.getObjecto().mudarEstado(EstadoObjecto.DISPONIVEL, "Sistema", "Reserva expirada",
                        "Levantamento não feito (RN13)", agora);
                notificacoes.enviar(r.getReclamante(), "Reserva expirada",
                        "Não levantou o objecto " + r.getObjecto().getCodigo() + " a tempo.");
                reservas++;
            } else if (r.getEstado() == EstadoReclamacao.PROVA_ADICIONAL && agora.isAfter(r.getPrazoResposta())) {
                r.setEstado(EstadoReclamacao.EXPIRADA);
                notificacoes.enviar(r.getReclamante(), "Reclamação expirada",
                        "A reclamação #" + r.getId() + " expirou por falta de provas (RN13).");
                reservas++;
            }
        }

        for (int i = 0; i < base.getProcuras().size(); i++) {
            Procura p = base.getProcuras().get(i);
            if (p.getEstado() != EstadoProcura.ACTIVA) {
                continue;
            }
            long diasRestantes = ChronoUnit.DAYS.between(hoje, p.getValidaAte());
            if (diasRestantes < 0) {
                p.setEstado(EstadoProcura.EXPIRADA);
                notificacoes.enviar(p.getDono(), "Procura expirada",
                        "A procura #" + p.getId() + " expirou. Registe uma nova se ainda procura o objecto.");
                procurasExpiradas++;
            } else if (diasRestantes <= DIAS_AVISO_PROCURA && !p.isAvisoExpiracaoEnviado()) {
                notificacoes.enviar(p.getDono(), "Procura a expirar",
                        "A procura #" + p.getId() + " expira em " + diasRestantes + " dia(s).");
                p.setAvisoExpiracaoEnviado(true);
                procurasAvisadas++;
            }
        }

        resumo.add("Pré-registos cancelados: " + preRegistos);
        resumo.add("Reservas/reclamações expiradas: " + reservas);
        resumo.add("Objectos com aviso de prazo: " + objectosAvisados);
        resumo.add("Objectos passados a NÃO_RECLAMADO: " + naoReclamados);
        resumo.add("Procuras com aviso de expiração: " + procurasAvisadas);
        resumo.add("Procuras expiradas: " + procurasExpiradas);
        return resumo;
    }

    private void avisarPrazo(Objecto o, long diasRestantes) {
        String msg = "O objecto " + o.getCodigo() + " termina o prazo de guarda em " + diasRestantes + " dia(s).";
        notificacoes.enviar(o.getAchador(), "Prazo de guarda a terminar", msg);
        for (int i = 0; i < base.getUtilizadores().size(); i++) {
            Utilizador u = base.getUtilizadores().get(i);
            if (u instanceof FuncionarioSector && ((FuncionarioSector) u).getSector() == o.getSector()) {
                notificacoes.enviar(u, "Prazo de guarda a terminar", msg);
            }
        }
        List<model.Correspondencia> provaveis = correspondencia.procurasCompativeis(o);
        for (int i = 0; i < provaveis.size(); i++) {
            notificacoes.enviar(provaveis.get(i).getProcura().getDono(), "Última oportunidade", msg
                    + " Pode ser o seu objecto.");
        }
    }

    /** RN19: o destino final só pode ser executado depois de aprovado pelo Gestor. */
    public Objecto aprovarDestino(Gestor g, String codigo, Destino destino) {
        Objecto o = base.objectoPorCodigo(codigo);
        if (o == null || o.getEstado() != EstadoObjecto.NAO_RECLAMADO) {
            throw new RegraNegocioException("O objecto " + codigo + " não está à espera de destino final.");
        }
        if (destino == null) {
            throw new RegraNegocioException("Escolha o destino final.");
        }
        o.setDestino(destino);
        o.mudarEstado(EstadoObjecto.DESTINO_APROVADO, g.getNome(), "Destino aprovado", destino.toString(),
                relogio.agora());
        return o;
    }

    public Objecto executarDestino(FuncionarioSector f, String codigo) {
        Objecto o = base.objectoPorCodigo(codigo);
        if (o == null || o.getEstado() != EstadoObjecto.DESTINO_APROVADO) {
            throw new RegraNegocioException("O objecto " + codigo + " não tem destino aprovado (RN19).");
        }
        if (o.getSector() != f.getSector()) {
            throw new RegraNegocioException("O objecto está na " + o.getSector() + ".");
        }
        o.mudarEstado(EstadoObjecto.DESTINO_CUMPRIDO, f.getNome(), "Destino executado", o.getDestino().toString(),
                relogio.agora());
        return o;
    }

    public List<Objecto> noEstado(EstadoObjecto estado) {
        List<Objecto> lista = new ArrayList<Objecto>();
        for (int i = 0; i < base.getObjectos().size(); i++) {
            if (base.getObjectos().get(i).getEstado() == estado) {
                lista.add(base.getObjectos().get(i));
            }
        }
        return lista;
    }

    private void notificarGestores(String assunto, String mensagem) {
        for (int i = 0; i < base.getUtilizadores().size(); i++) {
            Utilizador u = base.getUtilizadores().get(i);
            if (u instanceof Gestor) {
                notificacoes.enviar(u, assunto, mensagem);
            }
        }
    }
}
