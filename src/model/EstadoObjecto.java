package model;

/** Fases da vida de um objecto. Só são permitidas as mudanças listadas em podePassarPara. */
public enum EstadoObjecto {
    PRE_REGISTADO,      // registado no portal, ainda não entregue no sector
    DISPONIVEL,         // guardado no sector e visível no catálogo
    RESERVADO,          // alguém reclamou e está a ser verificado
    DEVOLVIDO,          // entregue ao dono
    NAO_RECLAMADO,      // passou o prazo de guarda
    DESTINO_APROVADO,   // o Gestor escolheu o destino final
    DESTINO_CUMPRIDO,   // destino executado
    CANCELADO;          // pré-registo expirado ou entrada recusada

    public boolean podePassarPara(EstadoObjecto novo) {
        switch (this) {
            case PRE_REGISTADO:
                return novo == DISPONIVEL || novo == CANCELADO;
            case DISPONIVEL:
                return novo == RESERVADO || novo == NAO_RECLAMADO;
            case RESERVADO:
                return novo == DISPONIVEL || novo == DEVOLVIDO;
            case NAO_RECLAMADO:
                return novo == DESTINO_APROVADO;
            case DESTINO_APROVADO:
                return novo == DESTINO_CUMPRIDO;
            default:
                return false;   // DEVOLVIDO, DESTINO_CUMPRIDO e CANCELADO são finais
        }
    }
}
