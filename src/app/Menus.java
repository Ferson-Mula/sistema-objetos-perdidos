package app;

import java.util.ArrayList;
import java.util.List;

import model.Categoria;
import model.Correspondencia;
import model.Destino;
import model.EstadoObjecto;
import model.EstadoProcura;
import model.EstadoReclamacao;
import model.FuncionarioSector;
import model.Gestor;
import model.Notificacao;
import model.Objecto;
import model.Procura;
import model.Reclamacao;
import model.RegraNegocioException;
import model.Sector;
import model.Utilizador;
import model.Zona;
import repository.Persistencia;

/** Menus de consola. Cada perfil vê as suas opções. Os dados são gravados depois de cada operação. */
public class Menus {

    private final Sistema sis;
    private final Consola consola;
    private final String ficheiroDados;

    public Menus(Sistema sis, Consola consola, String ficheiroDados) {
        this.sis = sis;
        this.consola = consola;
        this.ficheiroDados = ficheiroDados;
    }

    // ------------------------------------------------------------------ menu inicial

    public void executar() {
        boolean continuar = true;
        while (continuar) {
            System.out.println();
            System.out.println("=== ACHADOS E PERDIDOS - UJC ===");
            System.out.println("1) Entrar");
            System.out.println("2) Criar conta (estudante ou docente)");
            System.out.println("3) Pesquisar catálogo público");
            System.out.println("0) Sair");
            int op = consola.lerInt("Opção:");
            try {
                if (op == 1) {
                    entrar();
                } else if (op == 2) {
                    criarConta();
                } else if (op == 3) {
                    pesquisarCatalogo();
                } else if (op == 0) {
                    continuar = false;
                } else {
                    System.out.println("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
        gravar();
        System.out.println("Até à próxima.");
    }

    private void gravar() {
        Persistencia.gravar(sis.base, ficheiroDados);
    }

    private void entrar() {
        String numero = consola.ler("Número:");
        String senha = consola.ler("Palavra-passe:");
        Utilizador u = sis.autenticacao.entrar(numero, senha);
        System.out.println("Bem-vindo, " + u.getNome() + ".");
        int novas = 0;
        List<Notificacao> ns = sis.notificacoes.doUtilizador(u);
        for (int i = 0; i < ns.size(); i++) {
            if (!ns.get(i).isLida()) {
                novas++;
            }
        }
        if (novas > 0) {
            System.out.println("Tem " + novas + " notificação(ões) por ler.");
        }
        if (u instanceof FuncionarioSector) {
            menuFuncionario((FuncionarioSector) u);
        } else if (u instanceof Gestor) {
            menuGestor((Gestor) u);
        } else {
            menuUtilizador(u);
        }
    }

    private void criarConta() {
        String tipo = consola.ler("Tipo (1 = estudante, 2 = docente):");
        String nome = consola.ler("Nome completo:");
        String numero = consola.ler("Número institucional:");
        String email = consola.ler("E-mail:");
        String doc = consola.ler("N.º do documento de identificação:");
        String senha = consola.ler("Palavra-passe (mín. 4 caracteres):");
        if (tipo.equals("2")) {
            sis.autenticacao.criarDocente(nome, numero, email, doc, senha);
        } else {
            sis.autenticacao.criarEstudante(nome, numero, email, doc, senha);
        }
        gravar();
        System.out.println("Conta criada. Já pode entrar.");
    }

    // ------------------------------------------------------------------ estudante / docente

    private void menuUtilizador(Utilizador u) {
        boolean continuar = true;
        while (continuar) {
            System.out.println();
            System.out.println("--- Menu de " + u.getNome() + " ---");
            System.out.println("1) Registar objecto ENCONTRADO");
            System.out.println("2) Registar objecto PERDIDO");
            System.out.println("3) As minhas procuras");
            System.out.println("4) Pesquisar catálogo");
            System.out.println("5) Reclamar um objecto");
            System.out.println("6) As minhas reclamações");
            System.out.println("7) Notificações");
            System.out.println("0) Sair da conta");
            int op = consola.lerInt("Opção:");
            try {
                if (op == 1) {
                    registarEncontrado(u);
                } else if (op == 2) {
                    registarPerdido(u);
                } else if (op == 3) {
                    minhasProcuras(u);
                } else if (op == 4) {
                    pesquisarCatalogo();
                } else if (op == 5) {
                    reclamar(u);
                } else if (op == 6) {
                    minhasReclamacoes(u);
                } else if (op == 7) {
                    notificacoes(u);
                } else if (op == 0) {
                    continuar = false;
                } else {
                    System.out.println("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                System.out.println("Erro: " + e.getMessage());
            }
            gravar();
        }
    }

    private void registarEncontrado(Utilizador u) {
        Categoria cat = consola.escolherEnum("Categoria:", Categoria.values());
        if (cat == null) {
            return;
        }
        String desc = consola.ler("Descrição:");
        String cor = consola.ler("Cor:");
        String marca = consola.ler("Marca:");
        Zona zona = consola.escolherEnum("Onde foi encontrado?", Zona.values());
        if (zona == null) {
            return;
        }
        java.time.LocalDateTime quando = consola.lerDataHora("Quando foi encontrado?", sis.relogio.agora());
        String foto = consola.ler("Ficheiro da foto (caminho ou nome):");
        String detalhe = consola.ler("Detalhe identificador que NÃO será publicado (ex.: autocolante, risco, conteúdo):");
        Sector sector = consola.escolherEnum("Onde vai entregar?", Sector.values());
        if (sector == null) {
            return;
        }
        Objecto o = sis.registo.registarEncontrado(u, cat, desc, cor, marca, zona, quando, foto, detalhe, sector);
        System.out.println("Pré-registo criado. Código: " + o.getCodigo() + ". Entregue o objecto na " + sector
                + " em 48 horas, indicando o código.");
    }

    private void registarPerdido(Utilizador u) {
        Categoria cat = consola.escolherEnum("Categoria:", Categoria.values());
        if (cat == null) {
            return;
        }
        String desc = consola.ler("Descrição:");
        String cor = consola.ler("Cor:");
        String marca = consola.ler("Marca:");
        Zona zona = consola.escolherEnum("Onde acha que perdeu?", Zona.values());
        if (zona == null) {
            return;
        }
        java.time.LocalDate quando = consola.lerData("Quando perdeu?");
        String detalhe = consola.ler("Detalhe que só o dono sabe (não será publicado):");
        Procura p = sis.registo.registarPerda(u, cat, desc, cor, marca, zona, quando, detalhe);
        List<Correspondencia> candidatos = sis.correspondencia.candidatosPara(p);
        if (candidatos.isEmpty()) {
            System.out.println("Procura #" + p.getId() + " registada. Ainda não há candidatos; será avisado.");
        } else {
            System.out.println("Procura #" + p.getId() + " registada. Candidatos encontrados:");
            for (int i = 0; i < candidatos.size(); i++) {
                System.out.println("  - " + String.format("%.2f", candidatos.get(i).getPontuacao()) + " | "
                        + sis.catalogo.resumoPublico(candidatos.get(i).getObjecto()));
            }
        }
    }

    private void minhasProcuras(Utilizador u) {
        boolean alguma = false;
        for (int i = 0; i < sis.base.getProcuras().size(); i++) {
            Procura p = sis.base.getProcuras().get(i);
            if (p.getDono().getId() == u.getId()) {
                alguma = true;
                System.out.println(p);
                if (p.getEstado() == EstadoProcura.ACTIVA) {
                    List<Correspondencia> c = sis.correspondencia.candidatosPara(p);
                    for (int j = 0; j < c.size(); j++) {
                        System.out.println("     candidato " + String.format("%.2f", c.get(j).getPontuacao()) + " | "
                                + sis.catalogo.resumoPublico(c.get(j).getObjecto()));
                    }
                }
            }
        }
        if (!alguma) {
            System.out.println("Não tem procuras registadas.");
        }
    }

    private void pesquisarCatalogo() {
        Categoria cat = null;
        if (consola.lerSimNao("Filtrar por categoria?")) {
            cat = consola.escolherEnum("Categoria:", Categoria.values());
        }
        Zona zona = null;
        if (consola.lerSimNao("Filtrar por zona?")) {
            zona = consola.escolherEnum("Zona:", Zona.values());
        }
        String cor = consola.ler("Cor (vazio = qualquer):");
        String texto = consola.ler("Palavra na descrição (vazio = qualquer):");
        List<Objecto> r = sis.catalogo.pesquisar(cat, cor, zona, texto);
        if (r.isEmpty()) {
            System.out.println("Nenhum objecto encontrado.");
        }
        for (int i = 0; i < r.size(); i++) {
            System.out.println("  " + sis.catalogo.resumoPublico(r.get(i)));
        }
    }

    private void reclamar(Utilizador u) {
        String codigo = consola.ler("Código do objecto (ex.: OBJ-2026-0001):");
        Objecto o = sis.base.objectoPorCodigo(codigo);
        if (o == null) {
            throw new RegraNegocioException("Não existe o objecto " + codigo + ".");
        }
        System.out.println(sis.catalogo.resumoPublico(o));
        System.out.println("Pergunta de verificação: descreva o detalhe que identifica este objecto como seu "
                + "(algo que não aparece no catálogo).");
        String resposta = consola.ler("Resposta:");
        String provas = consola.ler("Provas (foto antiga, factura, captura de ecrã; vazio = nenhuma):");
        Procura associada = null;
        List<Procura> minhas = new ArrayList<Procura>();
        for (int i = 0; i < sis.base.getProcuras().size(); i++) {
            Procura p = sis.base.getProcuras().get(i);
            if (p.getDono().getId() == u.getId() && p.getEstado() == EstadoProcura.ACTIVA) {
                minhas.add(p);
            }
        }
        if (!minhas.isEmpty() && consola.lerSimNao("Associar a uma das suas procuras activas?")) {
            associada = consola.escolher("Procura:", minhas);
        }
        Reclamacao r = sis.reclamacoes.submeter(u, codigo, resposta, provas, associada);
        System.out.println(r);
        if (r.getEstado() == EstadoReclamacao.PROVA_ADICIONAL) {
            System.out.println("Pontuação insuficiente. Acrescente provas em 48 horas (menu 'As minhas reclamações').");
        } else {
            System.out.println("Reclamação enviada para verificação.");
        }
    }

    private void minhasReclamacoes(Utilizador u) {
        List<Reclamacao> lista = sis.reclamacoes.doUtilizador(u);
        if (lista.isEmpty()) {
            System.out.println("Não tem reclamações.");
            return;
        }
        Reclamacao r = consola.escolher("As suas reclamações:", lista);
        if (r == null) {
            return;
        }
        System.out.println(r);
        if (r.getMotivoDecisao() != null) {
            System.out.println("Motivo da decisão: " + r.getMotivoDecisao());
        }
        if (r.getEstado() == EstadoReclamacao.APROVADA) {
            System.out.println("Levante o objecto na " + r.getObjecto().getSector() + " até " + r.getPrazoLevantamento()
                    + ", com o documento de identificação.");
        } else if (r.getEstado() == EstadoReclamacao.PROVA_ADICIONAL) {
            if (consola.lerSimNao("Acrescentar provas agora?")) {
                String resp = consola.ler("Mais informação sobre o detalhe do objecto:");
                String provas = consola.ler("Novas provas:");
                System.out.println(sis.reclamacoes.completarProva(u, r.getId(), resp, provas));
            }
        } else if (r.getEstado() == EstadoReclamacao.RECUSADA && !r.isRecursoUsado()) {
            if (consola.lerSimNao("Pedir recurso ao Gestor?")) {
                String just = consola.ler("Justificação:");
                System.out.println(sis.reclamacoes.pedirRecurso(u, r.getId(), just));
            }
        }
    }

    private void notificacoes(Utilizador u) {
        List<Notificacao> lista = sis.notificacoes.doUtilizador(u);
        if (lista.isEmpty()) {
            System.out.println("Sem notificações.");
        }
        for (int i = 0; i < lista.size(); i++) {
            System.out.println(lista.get(i));
        }
        sis.notificacoes.marcarTodasLidas(u);
    }

    // ------------------------------------------------------------------ funcionário

    private void menuFuncionario(FuncionarioSector f) {
        boolean continuar = true;
        while (continuar) {
            System.out.println();
            System.out.println("--- " + f.getNome() + " (" + f.getSector() + ") ---");
            System.out.println("1) Confirmar entrada de objecto");
            System.out.println("2) Rever reclamações");
            System.out.println("3) Entregar objecto (levantamento)");
            System.out.println("4) Executar destino final aprovado");
            System.out.println("5) Inventário do sector");
            System.out.println("6) Histórico de um objecto");
            System.out.println("7) Notificações");
            System.out.println("0) Sair da conta");
            int op = consola.lerInt("Opção:");
            try {
                if (op == 1) {
                    confirmarEntrada(f);
                } else if (op == 2) {
                    reverReclamacoes(f);
                } else if (op == 3) {
                    entregar(f);
                } else if (op == 4) {
                    executarDestino(f);
                } else if (op == 5) {
                    inventario(f.getSector());
                } else if (op == 6) {
                    historico();
                } else if (op == 7) {
                    notificacoes(f);
                } else if (op == 0) {
                    continuar = false;
                } else {
                    System.out.println("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                System.out.println("Erro: " + e.getMessage());
            }
            gravar();
        }
    }

    private void confirmarEntrada(FuncionarioSector f) {
        List<Objecto> pendentes = new ArrayList<Objecto>();
        for (int i = 0; i < sis.base.getObjectos().size(); i++) {
            Objecto o = sis.base.getObjectos().get(i);
            if (o.getEstado() == EstadoObjecto.PRE_REGISTADO && o.getSector() == f.getSector()) {
                pendentes.add(o);
            }
        }
        if (pendentes.isEmpty()) {
            System.out.println("Não há pré-registos à espera nesta secção.");
            return;
        }
        Objecto o = consola.escolher("Pré-registos à espera de entrega:", pendentes);
        if (o == null) {
            return;
        }
        System.out.println("Detalhe oculto registado pelo achador: " + o.getDetalheOculto());
        boolean conforme = consola.lerSimNao("O objecto entregue está conforme o registo?");
        String local = "";
        String nota = "";
        if (conforme) {
            local = consola.ler("Localização física (ex.: Armário 2, prateleira B):");
        } else {
            nota = consola.ler("Motivo:");
        }
        sis.registo.confirmarEntrada(f, o.getCodigo(), conforme, local, nota);
        System.out.println(conforme ? "Entrada confirmada. Etiquete o objecto com " + o.getCodigo() + "."
                : "Entrada recusada.");
    }

    private void reverReclamacoes(FuncionarioSector f) {
        List<Reclamacao> lista = sis.reclamacoes.doSectorNoEstado(f.getSector(), EstadoReclamacao.EM_VERIFICACAO);
        if (lista.isEmpty()) {
            System.out.println("Não há reclamações para rever.");
            return;
        }
        Reclamacao r = consola.escolher("Reclamações em verificação:", lista);
        if (r == null) {
            return;
        }
        Objecto o = r.getObjecto();
        System.out.println("Objecto: " + o);
        System.out.println("Detalhe oculto registado: " + o.getDetalheOculto());
        System.out.println("Resposta do reclamante: " + r.getRespostaDetalhe());
        System.out.println("Provas: " + r.getProvas());
        System.out.println("Reclamante: " + r.getReclamante() + " | documento: "
                + r.getReclamante().getDocumentoIdentificacao());
        boolean aprovar = consola.lerSimNao("Aprovar a reclamação?");
        if (aprovar) {
            boolean identidade = consola.lerSimNao("Conferiu a identidade do reclamante?");
            sis.reclamacoes.revisar(f, r.getId(), true, identidade, null);
            System.out.println("Reclamação aprovada.");
        } else {
            String motivo = consola.ler("Motivo da recusa:");
            sis.reclamacoes.revisar(f, r.getId(), false, false, motivo);
            System.out.println("Reclamação recusada.");
        }
    }

    private void entregar(FuncionarioSector f) {
        List<Reclamacao> lista = sis.reclamacoes.doSectorNoEstado(f.getSector(), EstadoReclamacao.APROVADA);
        if (lista.isEmpty()) {
            System.out.println("Não há levantamentos aprovados.");
            return;
        }
        Reclamacao r = consola.escolher("Levantamentos aprovados:", lista);
        if (r == null) {
            return;
        }
        System.out.println("Local de armazenamento: " + r.getObjecto().getLocalizacaoFisica());
        boolean doc = consola.lerSimNao("Conferiu o documento de identificação (n.º "
                + r.getReclamante().getDocumentoIdentificacao() + ")?");
        sis.reclamacoes.entregar(f, r.getId(), doc);
        System.out.println("Objecto entregue e registado.");
    }

    private void executarDestino(FuncionarioSector f) {
        List<Objecto> lista = new ArrayList<Objecto>();
        for (int i = 0; i < sis.base.getObjectos().size(); i++) {
            Objecto o = sis.base.getObjectos().get(i);
            if (o.getEstado() == EstadoObjecto.DESTINO_APROVADO && o.getSector() == f.getSector()) {
                lista.add(o);
            }
        }
        Objecto o = consola.escolher("Objectos com destino aprovado:", lista);
        if (o == null) {
            return;
        }
        sis.prazos.executarDestino(f, o.getCodigo());
        System.out.println("Destino executado: " + o.getDestino());
    }

    private void inventario(Sector sector) {
        boolean algum = false;
        for (int i = 0; i < sis.base.getObjectos().size(); i++) {
            Objecto o = sis.base.getObjectos().get(i);
            if (o.getSector() == sector && o.getEstado() != EstadoObjecto.CANCELADO) {
                System.out.println("  " + o + " | " + o.getLocalizacaoFisica() + " | prazo " + o.getPrazoGuarda());
                algum = true;
            }
        }
        if (!algum) {
            System.out.println("Sem objectos.");
        }
    }

    private void historico() {
        String codigo = consola.ler("Código do objecto:");
        Objecto o = sis.base.objectoPorCodigo(codigo);
        if (o == null) {
            throw new RegraNegocioException("Não existe o objecto " + codigo + ".");
        }
        System.out.println(o);
        for (int i = 0; i < o.getHistorico().size(); i++) {
            System.out.println("   " + o.getHistorico().get(i));
        }
    }

    // ------------------------------------------------------------------ gestor

    private void menuGestor(Gestor g) {
        boolean continuar = true;
        while (continuar) {
            System.out.println();
            System.out.println("--- Gestor: " + g.getNome() + " ---");
            System.out.println("1) Decidir recursos");
            System.out.println("2) Aprovar destino final");
            System.out.println("3) Relatório de indicadores");
            System.out.println("4) Executar rotina de prazos");
            System.out.println("5) Avançar o relógio (simulação)");
            System.out.println("6) Notificações");
            System.out.println("0) Sair da conta");
            int op = consola.lerInt("Opção:");
            try {
                if (op == 1) {
                    decidirRecursos(g);
                } else if (op == 2) {
                    aprovarDestino(g);
                } else if (op == 3) {
                    List<String> linhas = sis.relatorios.gerar();
                    for (int i = 0; i < linhas.size(); i++) {
                        System.out.println(linhas.get(i));
                    }
                } else if (op == 4) {
                    List<String> linhas = sis.prazos.executarRotina();
                    for (int i = 0; i < linhas.size(); i++) {
                        System.out.println(linhas.get(i));
                    }
                } else if (op == 5) {
                    int dias = consola.lerInt("Quantos dias avançar?");
                    if (dias > 0) {
                        sis.relogio.avancarDias(dias);
                        System.out.println("Data simulada: " + sis.relogio.agora());
                    }
                } else if (op == 6) {
                    notificacoes(g);
                } else if (op == 0) {
                    continuar = false;
                } else {
                    System.out.println("Opção inválida.");
                }
            } catch (RegraNegocioException e) {
                System.out.println("Erro: " + e.getMessage());
            }
            gravar();
        }
    }

    private void decidirRecursos(Gestor g) {
        List<Reclamacao> lista = sis.reclamacoes.noEstado(EstadoReclamacao.EM_RECURSO);
        if (lista.isEmpty()) {
            System.out.println("Não há recursos pendentes.");
            return;
        }
        Reclamacao r = consola.escolher("Recursos pendentes:", lista);
        if (r == null) {
            return;
        }
        System.out.println("Objecto: " + r.getObjecto());
        System.out.println("Detalhe oculto: " + r.getObjecto().getDetalheOculto());
        System.out.println("Resposta e recurso: " + r.getRespostaDetalhe());
        System.out.println("Motivo da recusa anterior: " + r.getMotivoDecisao());
        boolean aprovar = consola.lerSimNao("Aprovar o recurso?");
        String motivo = consola.ler("Motivo da decisão:");
        sis.reclamacoes.decidirRecurso(g, r.getId(), aprovar, motivo);
        System.out.println("Decisão registada.");
    }

    private void aprovarDestino(Gestor g) {
        List<Objecto> lista = sis.prazos.noEstado(EstadoObjecto.NAO_RECLAMADO);
        if (lista.isEmpty()) {
            System.out.println("Não há objectos não reclamados.");
            return;
        }
        Objecto o = consola.escolher("Objectos não reclamados:", lista);
        if (o == null) {
            return;
        }
        System.out.println("Destino sugerido: " + o.getDestino());
        Destino d = consola.escolherEnum("Destino final:", Destino.values());
        if (d == null) {
            return;
        }
        sis.prazos.aprovarDestino(g, o.getCodigo(), d);
        System.out.println("Destino aprovado.");
    }
}
