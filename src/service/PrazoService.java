package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import funcional.Consultas;
import funcional.Texto;
import model.EstadoObjecto;
import model.EstadoReclamacao;
import model.Objecto;
import model.Perfil;
import model.Procura;
import model.Reclamacao;
import model.RegraNegocioException;
import model.Utilizador;
import repository.BaseDados;

/**
 * P04: prazos e destino final (RN05, RN13, RN17 a RN19).
 * A rotina corre quando o Gestor a pede. Se no futuro tiver de correr sozinha todas as noites,
 * basta chamar executarRotina() de um temporizador (ver docs/analise-paradigmas.md).
 */
public class PrazoService {

    public static final int HORAS_PRE_REGISTO = 48;
    public static final int DIAS_AVISO_PRAZO = 15;
    public static final int DIAS_AVISO_PROCURA = 7;

    /** Destinos que o Gestor pode aprovar. */
    public static final List<String> DESTINOS = List.of("entregar_a_entidade_emissora", "destruicao_segura",
            "reciclagem", "doacao");

    private final BaseDados base;
    private final Relogio relogio;

    public PrazoService(BaseDados base, Relogio relogio) {
        this.base = base;
        this.relogio = relogio;
    }

    /** Aplica todos os prazos e devolve o resumo do que fez. */
    public List<String> executarRotina() {
        synchronized (base) {
            LocalDateTime agora = relogio.agora();
            LocalDate hoje = agora.toLocalDate();
            int cancelados = 0;
            int avisados = 0;
            int naoReclamados = 0;
            int reservasExpiradas = 0;
            int procurasAvisadas = 0;
            int procurasExpiradas = 0;

            for (Objecto o : base.objectos) {
                if (o.getEstado() == EstadoObjecto.PRE_REGISTADO
                        && agora.isAfter(o.getCriadoEm().plusHours(HORAS_PRE_REGISTO))) {
                    o.mudarEstado(EstadoObjecto.CANCELADO, "Sistema", "Pré-registo expirado (RN05)", agora);
                    base.avisar(o.getAchador(), "Pré-registo cancelado",
                            "O objecto " + o.getCodigo() + " não foi entregue em " + HORAS_PRE_REGISTO + " horas.");
                    cancelados++;
                } else if (o.getEstado() == EstadoObjecto.DISPONIVEL) {
                    long dias = ChronoUnit.DAYS.between(hoje, o.getPrazoGuarda());
                    if (dias < 0) {
                        o.mudarEstado(EstadoObjecto.NAO_RECLAMADO, "Sistema", "Prazo de guarda expirado", agora);
                        o.setDestino(o.getCategoria().destinoSugerido());
                        base.avisarGestores("Objecto não reclamado", o.getCodigo() + ": destino sugerido "
                                + o.getDestino() + ". Aprove o destino final.");
                        naoReclamados++;
                    } else if (dias <= DIAS_AVISO_PRAZO && !o.isAvisoPrazoEnviado()) {
                        avisarPrazo(o, dias);
                        o.setAvisoPrazoEnviado(true);
                        avisados++;
                    }
                }
            }

            for (Reclamacao r : base.reclamacoes) {
                boolean levantamentoPerdido = r.getEstado() == EstadoReclamacao.APROVADA
                        && hoje.isAfter(r.getPrazoLevantamento());
                boolean provaPerdida = r.getEstado() == EstadoReclamacao.PROVA_ADICIONAL
                        && agora.isAfter(r.getPrazoResposta());
                if (levantamentoPerdido) {
                    r.getObjecto().mudarEstado(EstadoObjecto.DISPONIVEL, "Sistema", "Reserva expirada (RN13)", agora);
                }
                if (levantamentoPerdido || provaPerdida) {
                    r.setEstado(EstadoReclamacao.EXPIRADA);
                    base.avisar(r.getReclamante(), "Reclamação expirada",
                            "A reclamação #" + r.getId() + " expirou por falta de resposta no prazo (RN13).");
                    reservasExpiradas++;
                }
            }

            for (Procura p : base.procuras) {
                if (!p.isActiva()) {
                    continue;
                }
                long dias = ChronoUnit.DAYS.between(hoje, p.getValidaAte());
                if (dias < 0) {
                    p.encerrar();
                    base.avisar(p.getDono(), "Procura expirada", "A procura #" + p.getId() + " expirou.");
                    procurasExpiradas++;
                } else if (dias <= DIAS_AVISO_PROCURA && !p.isAvisoExpiracaoEnviado()) {
                    base.avisar(p.getDono(), "Procura a expirar",
                            "A procura #" + p.getId() + " expira em " + dias + " dia(s).");
                    p.setAvisoExpiracaoEnviado(true);
                    procurasAvisadas++;
                }
            }

            List<String> resumo = new ArrayList<>();
            resumo.add("Pré-registos cancelados: " + cancelados);
            resumo.add("Reservas e reclamações expiradas: " + reservasExpiradas);
            resumo.add("Objectos com aviso de fim de prazo: " + avisados);
            resumo.add("Objectos passados a NAO_RECLAMADO: " + naoReclamados);
            resumo.add("Procuras com aviso de expiração: " + procurasAvisadas);
            resumo.add("Procuras expiradas: " + procurasExpiradas);
            return resumo;
        }
    }

    private void avisarPrazo(Objecto o, long dias) {
        String mensagem = "O objecto " + o.getCodigo() + " termina o prazo de guarda em " + dias + " dia(s).";
        base.avisar(o.getAchador(), "Prazo de guarda a terminar", mensagem);
        base.avisarFuncionarios(o.getSector(), "Prazo de guarda a terminar", mensagem);
        Consultas.procurasCompativeis(o, base.procuras).forEach(p ->
                base.avisar(p.getDono(), "Última oportunidade", mensagem + " Pode ser o seu objecto."));
    }

    /** RN19: o destino final só pode ser executado depois de aprovado pelo Gestor. */
    public Objecto aprovarDestino(Utilizador gestor, String codigo, String destino) {
        synchronized (base) {
            if (gestor.getPerfil() != Perfil.GESTOR) {
                throw new RegraNegocioException("Só o Gestor pode aprovar o destino final.");
            }
            Objecto o = base.objecto(codigo);
            if (o == null || o.getEstado() != EstadoObjecto.NAO_RECLAMADO) {
                throw new RegraNegocioException("O objecto " + codigo + " não está à espera de destino final.");
            }
            if (Texto.vazio(destino) || !DESTINOS.contains(destino)) {
                throw new RegraNegocioException("Destino inválido. Opções: " + DESTINOS);
            }
            o.setDestino(destino);
            o.mudarEstado(EstadoObjecto.DESTINO_APROVADO, gestor.getNome(), "Destino aprovado: " + destino,
                    relogio.agora());
            return o;
        }
    }

    public Objecto executarDestino(Utilizador funcionario, String codigo) {
        synchronized (base) {
            Objecto o = base.objecto(codigo);
            if (o == null || o.getEstado() != EstadoObjecto.DESTINO_APROVADO) {
                throw new RegraNegocioException("O objecto " + codigo + " não tem destino aprovado (RN19).");
            }
            if (!funcionario.ehFuncionarioDe(o.getSector())) {
                throw new RegraNegocioException("O objecto está na " + o.getSector() + ".");
            }
            o.mudarEstado(EstadoObjecto.DESTINO_CUMPRIDO, funcionario.getNome(),
                    "Destino executado: " + o.getDestino(), relogio.agora());
            return o;
        }
    }
}
