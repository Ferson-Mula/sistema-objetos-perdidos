import textwrap
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Circle, Polygon, Rectangle

BLUE = "#DCE8F8"; BLUE_E = "#2F5597"
GREEN = "#DDF1DD"; GREEN_E = "#2E7D32"
YEL = "#FFF4C2"; YEL_E = "#9A7B00"
RED_E = "#B71C1C"
PITCH = 1.2
BW, BH = 1.08, 1.0
LABEL_W = 0.55
X0 = LABEL_W + 0.75


class Diagram:
    def __init__(self, title, lanes, ncols, out):
        # lanes: list of (name, height)
        self.title, self.lanes, self.ncols, self.out = title, lanes, ncols, out
        self.W = X0 + ncols * PITCH + 0.2
        self.H = sum(h for _, h in lanes) + 0.55
        self.fig, self.ax = plt.subplots(figsize=(self.W, self.H), dpi=200)
        self.ax.set_xlim(0, self.W); self.ax.set_ylim(0, self.H)
        self.ax.axis("off")
        self.fig.subplots_adjust(0, 0, 1, 1)
        self.nodes = {}
        top = self.H - 0.35
        self.lane_y = []  # (top, bottom)
        y = top
        for i, (name, h) in enumerate(lanes):
            self.ax.add_patch(Rectangle((0.05, y - h), self.W - 0.1, h, fill=False, ec="#444", lw=1.0))
            self.ax.add_patch(Rectangle((0.05, y - h), LABEL_W, h, fc="#E9EEF6", ec="#444", lw=1.0))
            self.ax.text(0.05 + LABEL_W / 2, y - h / 2, name, rotation=90, ha="center", va="center",
                         fontsize=9.5, fontweight="bold", color="#1F2D4D", wrap=True)
            self.lane_y.append((y, y - h))
            y -= h
        self.ax.text(0.08, self.H - 0.17, title, fontsize=11.5, fontweight="bold", color="#17365D", va="center")

    def cx(self, col):
        return X0 + col * PITCH + 0.35

    def cy(self, lane, row=0.0):
        t, b = self.lane_y[lane]
        return (t + b) / 2 - row

    # --- nodes
    def task(self, key, lane, col, text, kind="user", row=0.0, wrap=14):
        x, y = self.cx(col), self.cy(lane, row)
        fc, ec = (BLUE, BLUE_E) if kind == "user" else (GREEN, GREEN_E)
        self.ax.add_patch(FancyBboxPatch((x - BW / 2, y - BH / 2), BW, BH, boxstyle="round,pad=0,rounding_size=0.09",
                                         fc=fc, ec=ec, lw=1.3, zorder=3))
        self.ax.text(x, y, textwrap.fill(text, wrap), ha="center", va="center", fontsize=8.6, zorder=4,
                     linespacing=1.12)
        if kind == "system":
            self.ax.text(x - BW / 2 + 0.07, y + BH / 2 - 0.07, "⚙", fontsize=7, ha="left", va="top",
                         color=GREEN_E, zorder=5)
        self.nodes[key] = (x, y, BW / 2, BH / 2)

    def event(self, key, lane, col, kind, label, row=0.0, below=True, wrap=18, timer=False):
        x, y = self.cx(col), self.cy(lane, row)
        r = 0.21
        if kind == "start":
            self.ax.add_patch(Circle((x, y), r, fc="#E3F4E3", ec=GREEN_E, lw=1.6, zorder=3))
        elif kind == "end":
            self.ax.add_patch(Circle((x, y), r, fc="#FBE3E3", ec=RED_E, lw=3.0, zorder=3))
        else:  # intermediate
            self.ax.add_patch(Circle((x, y), r, fc="#FFF", ec="#6A1B9A", lw=1.2, zorder=3))
            self.ax.add_patch(Circle((x, y), r - 0.045, fc="none", ec="#6A1B9A", lw=1.2, zorder=3))
        if timer:
            self.ax.plot([x,x],[y,y+0.12],color="#333",lw=1.2,zorder=5); self.ax.plot([x,x+0.09],[y,y],color="#333",lw=1.2,zorder=5)
        elif kind == "inter":
            self.ax.text(x, y, "✉", fontsize=8, ha="center", va="center", zorder=5, color="#6A1B9A")
        ty = y - r - 0.06 if below else y + r + 0.06
        self.ax.text(x, ty, textwrap.fill(label, wrap), ha="center", va="top" if below else "bottom",
                     fontsize=8, zorder=4, linespacing=1.1)
        self.nodes[key] = (x, y, r, r)

    def gate(self, key, lane, col, text, row=0.0, wrap=14, above=True):
        x, y = self.cx(col), self.cy(lane, row)
        s = 0.34
        self.ax.add_patch(Polygon([(x - s, y), (x, y + s), (x + s, y), (x, y - s)], fc=YEL, ec=YEL_E, lw=1.4,
                                  zorder=3))
        self.ax.text(x, y, "×", fontsize=11, ha="center", va="center", zorder=4, color=YEL_E)
        self.ax.text(x, (y + s + 0.05) if above else (y - s - 0.05), textwrap.fill(text, wrap), ha="center", va="bottom" if above else "top", fontsize=8,
                     zorder=4, linespacing=1.1, fontstyle="italic")
        self.nodes[key] = (x, y, s, s)

    def note(self, x, y, w, h, text, wrap=44):
        self.ax.add_patch(Rectangle((x, y), w, h, fc="#FFFDF2", ec="#8A7A30", lw=1, ls="--", zorder=2))
        self.ax.text(x + 0.08, y + h - 0.07, textwrap.fill(text, wrap), ha="left", va="top", fontsize=7.2,
                     zorder=3, linespacing=1.15)

    # --- edges
    def side(self, key, s):
        x, y, hw, hh = self.nodes[key]
        return {"r": (x + hw, y), "l": (x - hw, y), "t": (x, y + hh), "b": (x, y - hh)}[s]

    def path(self, pts, label=None, lpos=None, dashed=False, color="#222"):
        xs, ys = zip(*pts)
        ls = (0, (4, 3)) if dashed else "-"
        self.ax.plot(xs[:-1] + (xs[-1],), ys, color=color, lw=1.15, ls=ls, zorder=1, solid_capstyle="butt")
        (x1, y1), (x2, y2) = pts[-2], pts[-1]
        self.ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                         arrowprops=dict(arrowstyle="-|>", color=color, lw=1.15, shrinkA=0, shrinkB=0,
                                         mutation_scale=9), zorder=2)
        if label:
            lx, ly = lpos if lpos else ((pts[0][0] + pts[1][0]) / 2, (pts[0][1] + pts[1][1]) / 2)
            self.ax.text(lx, ly, label, fontsize=8, ha="center", va="center", zorder=6,
                         bbox=dict(fc="white", ec="none", pad=0.6), fontweight="bold", color="#333")

    def h(self, a, b, label=None, lpos=None):
        self.path([self.side(a, "r"), self.side(b, "l")], label, lpos)

    def v(self, a, b, label=None, lpos=None, up=False):
        if up:
            self.path([self.side(a, "t"), self.side(b, "b")], label, lpos)
        else:
            self.path([self.side(a, "b"), self.side(b, "t")], label, lpos)

    def hv(self, a, b, label=None, lpos=None, xoff=0.0):
        """A.right -> horizontal to x of B -> vertical to B top/bottom"""
        p = self.side(a, "r")
        bx, by, _, bh = self.nodes[b]
        if by > p[1]:
            end = (bx + xoff, by - bh)
        else:
            end = (bx + xoff, by + bh)
        self.path([p, (bx + xoff, p[1]), end], label, lpos)

    def save(self, legend=True):
        if legend:
            y = 0.12
            self.ax.add_patch(FancyBboxPatch((0.9, y - 0.07), 0.3, 0.17, boxstyle="round,pad=0,rounding_size=0.04",
                                             fc=BLUE, ec=BLUE_E, lw=1))
            self.ax.text(1.27, y + 0.015, "Actividade humana", fontsize=7, va="center")
            self.ax.add_patch(FancyBboxPatch((2.65, y - 0.07), 0.3, 0.17, boxstyle="round,pad=0,rounding_size=0.04",
                                             fc=GREEN, ec=GREEN_E, lw=1))
            self.ax.text(3.02, y + 0.015, "⚙ Actividade do sistema (automatizada)", fontsize=7, va="center")
            self.ax.add_patch(Polygon([(5.95, y + 0.015), (6.1, y + 0.1), (6.25, y + 0.015), (6.1, y - 0.07)],
                                      fc=YEL, ec=YEL_E))
            self.ax.text(6.32, y + 0.015, "Decisão (gateway)", fontsize=7, va="center")
            self.ax.add_patch(Circle((7.75, y + 0.015), 0.08, fc="#E3F4E3", ec=GREEN_E, lw=1.2))
            self.ax.text(7.88, y + 0.015, "Início", fontsize=7, va="center")
            self.ax.add_patch(Circle((8.5, y + 0.015), 0.08, fc="#FBE3E3", ec=RED_E, lw=2))
            self.ax.text(8.63, y + 0.015, "Fim", fontsize=7, va="center")
            self.ax.add_patch(Circle((9.15, y + 0.015), 0.08, fc="white", ec="#6A1B9A", lw=1.2))
            self.ax.add_patch(Circle((9.15, y + 0.015), 0.05, fc="none", ec="#6A1B9A", lw=1))
            self.ax.text(9.28, y + 0.015, "Evento intermédio (mensagem/temporizador)", fontsize=7, va="center")
        self.fig.savefig(self.out, dpi=200, facecolor="white")
        plt.close(self.fig)


# ---------------------------------------------------------------- P01
def p01(out):
    d = Diagram("P01 — Registo de objecto encontrado (TO-BE)",
                [("Achador", 1.75), ("Sistema", 1.95), ("Funcionário do sector", 1.75)], 9, out)
    d.event("S", 0, 0, "start", "Objecto encontrado")
    d.task("T1", 0, 1, "Autenticar-se e registar o objecto (com foto)")
    d.task("T3", 1, 2, "Validar campos e classificar sensibilidade", "system")
    d.gate("G1", 1, 3, "Dados válidos?")
    d.task("T4", 1, 4, "Gerar código único e criar pré-registo", "system")
    d.task("T5", 0, 5, "Entregar o objecto no sector com o código")
    d.task("T6", 2, 6, "Conferir objecto com o registo e etiquetá-lo")
    d.gate("G2", 2, 7, "Objecto conforme?", above=False)
    d.task("T7", 1, 7, "Confirmar entrada, publicar anúncio e emitir evento",
           "system", wrap=14)
    d.event("E1", 1, 8, "end", "Objecto DISPONÍVEL (segue para P02)")
    d.event("E2", 2, 8, "end", "Registo corrigido ou cancelado")
    d.h("S", "T1")
    d.hv("T1", "T3")
    d.h("T3", "G1")
    d.h("G1", "T4", "Sim", (d.cx(3) + 0.52, d.cy(1) + 0.12))
    x1, y1 = d.side("G1", "b")
    bot = d.lane_y[1][1] + 0.064
    d.path([(x1, y1), (x1, bot), (d.cx(1), bot), d.side("T1", "b")], "Não: corrigir dados", (d.cx(2) + 0.1, bot))
    d.hv("T4", "T5")
    d.hv("T5", "T6")
    d.h("T6", "G2")
    d.v("G2", "T7", "Sim", (d.cx(7) + 0.2, d.cy(2) + 0.62), up=True)
    d.h("T7", "E1")
    d.h("G2", "E2", "Não", (d.cx(7) + 0.52, d.cy(2) + 0.12))
    d.note(d.cx(0) - 0.3, d.lane_y[2][1] + 0.15, 3.6, 0.7,
           "RN05: pré-registo sem entrega em 48 h é cancelado automaticamente (temporizador do sistema).", wrap=48)
    d.save()


# ---------------------------------------------------------------- P02
def p02(out):
    d = Diagram("P02 — Registo de objecto perdido e correspondência automática (TO-BE)",
                [("Proprietário", 1.75), ("Sistema (portal e motor)", 2.1),
                 ("Notificações", 1.65)], 9, out)
    d.event("S", 0, 0, "start", "Dá pela falta do objecto")
    d.task("T1", 0, 1, "Autenticar-se e registar a perda (com detalhe oculto)", wrap=14)
    d.task("T2", 1, 2, "Validar e guardar pedido de procura activo (60 dias)", "system")
    d.task("T3", 1, 3, "Comparar com objectos DISPONÍVEIS e calcular pontuação", "system")
    d.gate("G1", 1, 4, "Pontuação ≥ limiar?")
    d.task("T4", 2, 4, "Notificar proprietário com os candidatos prováveis", "system")
    d.task("T5", 1, 5, "Manter procura em vigilância", "system")
    d.event("IE", 1, 6, "inter", "Novo OBJECTO_REGISTADO (P01)", below=True)
    d.task("T6", 0, 6, "Consultar candidatos e comparar com a foto/descrição")
    d.gate("G2", 0, 7, "Reconhece o objecto?")
    d.event("E1", 0, 8, "end", "Segue para P03 (reclamação)")
    d.event("E2", 1, 7, "end", "Procura continua em vigilância")
    d.h("S", "T1")
    d.hv("T1", "T2")
    d.h("T2", "T3")
    d.h("T3", "G1")
    d.v("G1", "T4", "Sim", (d.cx(4) + 0.17, d.cy(1) - 0.62))
    d.h("G1", "T5", "Não", (d.cx(4) + 0.52, d.cy(1) + 0.12))
    d.h("T5", "IE")
    xr, yr = d.side("T4", "r")
    xm = d.cx(5) + 0.80
    d.path([(xr, yr), (xm, yr), (xm, d.cy(0)), d.side("T6", "l")])
    xt, yt = d.side("IE", "t")
    ytop = d.lane_y[1][0] - 0.2
    d.path([(xt, yt), (xt, ytop), (d.cx(3), ytop), d.side("T3", "t")], "reavaliar", (d.cx(4.6), ytop))
    d.h("T6", "G2")
    d.h("G2", "E1", "Sim", (d.cx(7) + 0.52, d.cy(0) + 0.12))
    d.v("G2", "E2", "Não", (d.cx(7) + 0.17, d.cy(0) - 0.62))
    d.note(d.cx(0) - 0.3, d.lane_y[2][1] + 0.12, 3.6, 0.78,
           "O proprietário também pode pesquisar o catálogo público (filtros por categoria, cor, zona e data). "
           "A procura expira ao fim de 60 dias, com aviso 7 dias antes (RN07).", wrap=52)
    d.save()


# ---------------------------------------------------------------- P03
def p03(out):
    d = Diagram("P03 — Reclamação, verificação e devolução de objecto (TO-BE)",
                [("Reclamante", 1.6), ("Sistema", 2.75), ("Funcionário", 1.7), ("Gestor", 1.55)], 10, out)
    r0, r1 = -0.62, 0.62  # rows within the system lane (row offset subtracts from y)
    d.event("S", 0, 0, "start", "Reconhece o objecto candidato")
    d.task("T1", 0, 1, "Submeter reclamação com respostas de verificação e provas", wrap=14)
    d.gate("G1", 1, 2, "Reclamação em curso?", row=r0, wrap=14)
    d.task("T2", 1, 2, "Colocar em fila de espera (ordem de chegada) e notificar", "system", row=r1)
    d.event("EQ", 1, 3, "end", "Em fila de espera", row=r1, wrap=14)
    d.task("T3", 1, 3, "Reservar objecto (exclusão mútua) e pontuar a verificação", "system", row=r0, wrap=14)
    d.gate("G2", 1, 4, "Verificação suficiente?", row=r0)
    d.task("T4", 1, 4, "Pedir prova adicional (48 h) e notificar", "system", row=r1)
    d.task("T5", 2, 5, "Rever o dossier e conferir os documentos de identificação")
    d.gate("G3", 2, 6, "Decisão do funcionário")
    d.task("T6", 1, 6, "Registar aprovação, reservar e agendar levantamento (5 dias úteis)", "system", row=r0, wrap=14)
    d.task("T7", 3, 6, "Decidir a disputa ou recurso e registar o motivo", wrap=14)
    d.event("ER", 2, 7, "end", "Reclamação recusada e notificada", below=False)
    d.task("T8", 0, 7, "Comparecer no sector com documento e código de levantamento", wrap=14)
    d.task("T9", 2, 8, "Entregar o objecto, recolher assinatura e registar a entrega", "system", wrap=14)
    d.event("E", 2, 9, "end", "Objecto DEVOLVIDO; evento OBJECTO_DEVOLVIDO")
    d.h("S", "T1")
    p = d.side("T1","r"); gx, gy, gw, _ = d.nodes["G1"]
    d.path([p, (d.cx(1)+0.8, p[1]), (d.cx(1)+0.8, gy), (gx-gw, gy)])
    d.v("G1", "T2", "Sim", (d.cx(2) + 0.17, d.cy(1)))
    d.h("T2", "EQ")
    d.h("G1", "T3", "Não", (d.cx(2) + 0.53, d.cy(1, r0) + 0.12))
    d.h("T3", "G2")
    d.v("G2", "T4", "Não", (d.cx(4) + 0.17, d.cy(1)))
    xb, yb = d.side("T4", "b")
    bot = d.lane_y[1][1] + 0.06
    d.path([(xb, yb), (xb, bot), (d.cx(1), bot), d.side("T1", "b")], "nova submissão", (d.cx(2.9), bot))
    d.hv("G2", "T5", "Sim", (d.cx(4) + 0.55, d.cy(1, r0) + 0.12))
    d.h("T5", "G3")
    d.v("G3", "T6", "Aprovada", (d.cx(6) + 0.33, d.cy(2) + 0.95), up=True)
    d.h("G3", "ER", "Recusada", (d.cx(6) + 0.55, d.cy(2) + 0.12))
    d.v("G3", "T7", "Disputa", (d.cx(6) + 0.28, d.cy(2) - 0.62))
    xl, yl = d.side("T7", "l")
    d.path([(xl, yl), (d.cx(5), yl), d.side("T5", "b")], "decisão\ndevolvida", (d.cx(5.25), yl + 0.2))
    d.hv("T6", "T8")
    d.hv("T8", "T9")
    d.h("T9", "E")
    d.note(d.cx(0) - 0.3, d.lane_y[3][1] + 0.12, 3.9, 0.62,
           "RN12: só uma reclamação por objecto pode estar em verificação; as restantes ficam em fila.\n"
           "RN14: provas equivalentes escalam a decisão para o Gestor.", wrap=60)
    d.save()


# ---------------------------------------------------------------- P04
def p04(out):
    d = Diagram("P04 — Gestão de prazos e destino final de objectos não reclamados (TO-BE)",
                [("Sistema (agendador)", 1.8), ("Funcionário", 1.75), ("Gestor", 1.6)], 10, out)
    d.event("S", 0, 0, "start", "Verificação diária (agendador)", timer=True)
    d.task("T1", 0, 1, "Listar objectos DISPONÍVEIS com prazo de guarda", "system")
    d.gate("G1", 0, 2, "Prazo termina em ≤ 15 dias?")
    d.task("T2", 0, 3, "Notificar achador, proprietário provável e funcionário", "system")
    d.gate("G2", 0, 4, "Prazo expirado sem reclamação?")
    d.task("T3", 0, 5, "Marcar NÃO RECLAMADO e propor destino conforme o tipo", "system", wrap=14)
    d.task("T4", 2, 6, "Aprovar o destino final e registar a decisão")
    d.task("T5", 1, 7, "Executar o destino aprovado e registar a saída",
           wrap=14)
    d.task("T6", 0, 8, "Libertar espaço e actualizar indicadores", "system")
    d.event("E", 0, 9, "end", "Ciclo concluído")
    d.event("E0", 1, 2, "end", "Sem acções necessárias")
    d.event("E1", 1, 4, "end", "Aguarda próximo ciclo")
    d.h("S", "T1")
    d.h("T1", "G1")
    d.h("G1", "T2", "Sim", (d.cx(2) + 0.52, d.cy(0) + 0.12))
    d.v("G1", "E0", "Não", (d.cx(2) + 0.17, d.cy(0) - 0.62))
    d.h("T2", "G2")
    d.h("G2", "T3", "Sim", (d.cx(4) + 0.52, d.cy(0) + 0.12))
    d.v("G2", "E1", "Não", (d.cx(4) + 0.17, d.cy(0) - 0.62))
    d.hv("T3", "T4")
    d.hv("T4", "T5")
    d.hv("T5", "T6")
    d.h("T6", "E")
    d.note(d.cx(0) - 0.3, d.lane_y[2][1] + 0.12, 3.3, 0.78,
           "Valores de referência (a aprovar pela Direcção): 90 dias de guarda; 30 dias para documentos de "
           "identificação. O Gestor recebe um relatório mensal de indicadores.", wrap=48)
    d.save()


if __name__ == "__main__":
    import sys, os
    o = sys.argv[1]
    p01(os.path.join(o, "p01.png")); p02(os.path.join(o, "p02.png"))
    p03(os.path.join(o, "p03.png")); p04(os.path.join(o, "p04.png"))
