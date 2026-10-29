package service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import funcional.Consultas;
import funcional.Texto;
import model.Categoria;
import model.EstadoObjecto;
import model.Objecto;
import model.Procura;
import model.RegraNegocioException;
import model.Sector;
import model.Utilizador;
import model.Zona;
import repository.BaseDados;

/** P01 (registar objecto encontrado e confirmar a entrada) e P02 (registar objecto perdido). */
public class RegistoService {

    private final BaseDados base;
    private final Relogio relogio;

    public RegistoService(BaseDados base, Relogio relogio) {
        this.base = base;
        this.relogio = relogio;
    }

    /** P01: quem encontrou regista o objecto e recebe um código (RN01, RN02). */
    public Objecto registarEncontrado(Utilizador achador, Categoria categoria, String descricao, String cor,
                                      String marca, Zona local, LocalDateTime encontradoEm, String foto,
                                      String detalheOculto, Sector sector) {
        if (categoria == null || local == null || encontradoEm == null || sector == null) {
            throw new RegraNegocioException("Categoria, local, data e sector são obrigatórios.");
        }
        if (Texto.vazio(descricao) || descricao.trim().length() < 5) {
            throw new RegraNegocioException("A descrição deve ter pelo menos 5 caracteres (RN02).");
        }
        if (encontradoEm.isAfter(relogio.agora())) {
            throw new RegraNegocioException("A data em que o objecto foi encontrado não pode estar no futuro.");
        }
        if (categoria.isFotoObrigatoria() && Texto.vazio(foto)) {
            throw new RegraNegocioException("A foto é obrigatória para a categoria " + categoria + " (RN02).");
        }
        synchronized (base) {
            boolean duplicado = base.objectos.stream().anyMatch(o ->
                    (o.getEstado() == EstadoObjecto.PRE_REGISTADO || o.getEstado() == EstadoObjecto.DISPONIVEL)
                            && o.getAchador().equals(achador) && o.getCategoria() == categoria
                            && o.getLocal() == local
                            && Texto.normalizar(o.getDescricao()).equals(Texto.normalizar(descricao)));
            if (duplicado) {
                throw new RegraNegocioException("Possível duplicado: já registou este objecto.");
            }
            String codigo = base.proximoCodigoObjecto(relogio.agora().getYear());
            Objecto o = new Objecto(codigo, categoria, descricao.trim(), cor, marca, local, encontradoEm, foto,
                    detalheOculto, achador, sector, relogio.agora());
            base.objectos.add(o);
            base.avisar(achador, "Pré-registo criado",
                    "Código " + codigo + ". Entregue o objecto na " + sector + " em " + PrazoService.HORAS_PRE_REGISTO
                            + " horas (RN05).");
            return o;
        }
    }

    /**
     * P01: o funcionário confere o objecto. Se estiver conforme, fica DISPONIVEL e o sistema
     * avisa os donos prováveis (RN03). Se não estiver, a entrada é recusada.
     */
    public Objecto confirmarEntrada(Utilizador funcionario, String codigo, boolean conforme,
                                    String localizacaoFisica, String nota) {
        synchronized (base) {
            Objecto o = base.objecto(codigo);
            if (o == null) {
                throw new RegraNegocioException("Não existe nenhum objecto com o código " + codigo + ".");
            }
            if (!funcionario.ehFuncionarioDe(o.getSector())) {
                throw new RegraNegocioException("Este objecto pertence à " + o.getSector() + ".");
            }
            if (o.getEstado() != EstadoObjecto.PRE_REGISTADO) {
                throw new RegraNegocioException("O objecto " + codigo + " não está à espera de entrega (estado "
                        + o.getEstado() + ").");
            }
            LocalDateTime agora = relogio.agora();
            if (!conforme) {
                o.mudarEstado(EstadoObjecto.CANCELADO, funcionario.getNome(), "Entrada recusada", agora);
                base.avisar(o.getAchador(), "Entrada recusada",
                        "O objecto " + codigo + " não estava conforme o registo. " + (nota == null ? "" : nota));
                return o;
            }
            if (Texto.vazio(localizacaoFisica)) {
                throw new RegraNegocioException("Indique onde o objecto fica guardado (prateleira, armário) (RN03).");
            }
            o.registarEntrada(localizacaoFisica.trim(), agora);
            o.mudarEstado(EstadoObjecto.DISPONIVEL, funcionario.getNome(), "Entrada confirmada", agora);
            base.avisar(o.getAchador(), "Objecto recebido", "O objecto " + codigo + " está no catálogo. Obrigado!");
            avisarTitular(o);
            Consultas.procurasCompativeis(o, base.procuras).forEach(p -> base.avisar(p.getDono(),
                    "Possível correspondência", "Apareceu o objecto " + codigo + " (" + o.getCategoria()
                            + ") compatível com a sua procura #" + p.getId() + "."));
            return o;
        }
    }

    /** RN06: se um documento traz o número do titular no detalhe oculto, o titular é avisado. */
    private void avisarTitular(Objecto o) {
        if (!Categoria.DOCUMENTO.equals(o.getCategoria()) && !Categoria.CARTAO.equals(o.getCategoria())) {
            return;
        }
        String detalhe = Texto.normalizar(o.getDetalheOculto());
        base.utilizadores.stream()
                .filter(u -> !detalhe.isEmpty()
                        && (Texto.normalizar(u.getNumero()).equals(detalhe)
                        || Texto.normalizar(u.getDocumento()).equals(detalhe)))
                .forEach(u -> base.avisar(u, "Documento encontrado",
                        "Foi entregue na " + o.getSector() + " um documento seu (" + o.getCodigo() + ")."));
    }

    /** P02: regista a perda e avisa logo se já existir algum objecto compatível (RN07, RN10). */
    public Procura registarPerda(Utilizador dono, Categoria categoria, String descricao, String cor, String marca,
                                 Zona localProvavel, LocalDate perdidoEm) {
        if (categoria == null || localProvavel == null || perdidoEm == null) {
            throw new RegraNegocioException("Categoria, local provável e data são obrigatórios.");
        }
        if (Texto.vazio(descricao) || descricao.trim().length() < 5) {
            throw new RegraNegocioException("A descrição deve ter pelo menos 5 caracteres.");
        }
        if (perdidoEm.isAfter(relogio.agora().toLocalDate())) {
            throw new RegraNegocioException("A data da perda não pode estar no futuro.");
        }
        synchronized (base) {
            Procura p = new Procura(base.proximoIdProcura(), dono, categoria, descricao.trim(), cor, marca,
                    localProvavel, perdidoEm, relogio.agora());
            base.procuras.add(p);
            base.avisar(dono, "Procura registada", "Procura #" + p.getId() + " activa até " + p.getValidaAte() + ".");
            Consultas.candidatos(p, base.objectos).forEach(o -> base.avisar(dono, "Possível correspondência",
                    "O objecto " + o.getCodigo() + " pode ser o seu. Veja o catálogo e faça a reclamação."));
            return p;
        }
    }
}
