package model;

/** Estados de um objecto e transições permitidas entre eles. */
public enum EstadoObjecto {
    PRE_REGISTADO,
    DISPONIVEL,
    RESERVADO,
    DEVOLVIDO,
    NAO_RECLAMADO,
    DESTINO_APROVADO,
    DESTINO_CUMPRIDO,
    CANCELADO;

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
                return false;
        }
    }
}
