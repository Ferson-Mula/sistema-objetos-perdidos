package app;

import repository.BaseDados;
import service.AutenticacaoService;
import service.CatalogoService;
import service.CorrespondenciaService;
import service.NotificacaoService;
import service.PrazoService;
import service.ReclamacaoService;
import service.RegistoService;
import service.RelatorioService;
import util.Relogio;

/** Liga todos os serviços aos mesmos dados e ao mesmo relógio. */
public class Sistema {

    public final BaseDados base;
    public final Relogio relogio;
    public final NotificacaoService notificacoes;
    public final AutenticacaoService autenticacao;
    public final CorrespondenciaService correspondencia;
    public final RegistoService registo;
    public final CatalogoService catalogo;
    public final ReclamacaoService reclamacoes;
    public final PrazoService prazos;
    public final RelatorioService relatorios;

    public Sistema(BaseDados base, Relogio relogio, boolean mostrarNotificacoes) {
        this.base = base;
        this.relogio = relogio;
        this.notificacoes = new NotificacaoService(base, relogio, mostrarNotificacoes);
        this.autenticacao = new AutenticacaoService(base);
        this.correspondencia = new CorrespondenciaService(base, notificacoes);
        this.registo = new RegistoService(base, relogio, notificacoes, correspondencia);
        this.catalogo = new CatalogoService(base);
        this.reclamacoes = new ReclamacaoService(base, relogio, notificacoes);
        this.prazos = new PrazoService(base, relogio, notificacoes, correspondencia);
        this.relatorios = new RelatorioService(base);
    }
}
