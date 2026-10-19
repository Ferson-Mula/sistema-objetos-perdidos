package service;

import java.util.ArrayList;
import java.util.List;

import model.Categoria;
import model.EstadoObjecto;
import model.Objecto;
import model.Zona;
import repository.BaseDados;

/** RN20: indicadores para o Gestor (volume, tipos, taxa de devolução, tempo médio, zonas críticas). */
public class RelatorioService {

    private final BaseDados base;

    public RelatorioService(BaseDados base) {
        this.base = base;
    }

    public List<String> gerar() {
        List<String> linhas = new ArrayList<String>();
        int total = 0;
        int devolvidos = 0;
        int naoReclamados = 0;
        double somaDias = 0;
        int comTempo = 0;

        EstadoObjecto[] estados = EstadoObjecto.values();
        int[] porEstado = new int[estados.length];
        Categoria[] categorias = Categoria.values();
        int[] porCategoria = new int[categorias.length];
        Zona[] zonas = Zona.values();
        int[] porZona = new int[zonas.length];

        for (int i = 0; i < base.getObjectos().size(); i++) {
            Objecto o = base.getObjectos().get(i);
            porEstado[o.getEstado().ordinal()]++;
            if (o.getEstado() == EstadoObjecto.PRE_REGISTADO || o.getEstado() == EstadoObjecto.CANCELADO) {
                continue;
            }
            total++;
            porCategoria[o.getCategoria().ordinal()]++;
            porZona[o.getLocal().ordinal()]++;
            if (o.getEstado() == EstadoObjecto.DEVOLVIDO) {
                devolvidos++;
                if (o.getEntradaEm() != null && o.getDevolvidoEm() != null) {
                    somaDias += java.time.Duration.between(o.getEntradaEm(), o.getDevolvidoEm()).toHours() / 24.0;
                    comTempo++;
                }
            }
            if (o.getEstado() == EstadoObjecto.NAO_RECLAMADO || o.getEstado() == EstadoObjecto.DESTINO_APROVADO
                    || o.getEstado() == EstadoObjecto.DESTINO_CUMPRIDO) {
                naoReclamados++;
            }
        }

        linhas.add("Objectos recebidos (confirmados): " + total);
        linhas.add("Devolvidos: " + devolvidos + " | Não reclamados: " + naoReclamados);
        double taxa = total == 0 ? 0 : 100.0 * devolvidos / total;
        linhas.add("Taxa de devolução: " + String.format("%.1f", taxa) + " %");
        linhas.add("Tempo médio até à devolução: "
                + (comTempo == 0 ? "sem dados" : String.format("%.1f dia(s)", somaDias / comTempo)));
        linhas.add("Por estado:");
        for (int i = 0; i < estados.length; i++) {
            if (porEstado[i] > 0) {
                linhas.add("   " + estados[i] + ": " + porEstado[i]);
            }
        }
        linhas.add("Por categoria:");
        for (int i = 0; i < categorias.length; i++) {
            if (porCategoria[i] > 0) {
                linhas.add("   " + categorias[i] + ": " + porCategoria[i]);
            }
        }
        linhas.add("Zonas onde mais se encontram objectos:");
        for (int i = 0; i < zonas.length; i++) {
            if (porZona[i] > 0) {
                linhas.add("   " + zonas[i] + ": " + porZona[i]);
            }
        }
        linhas.add("Procuras registadas: " + base.getProcuras().size() + " | Reclamações: "
                + base.getReclamacoes().size());
        return linhas;
    }
}
