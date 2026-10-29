package model;

/** Fases de uma reclamação. */
public enum EstadoReclamacao {
    PROVA_ADICIONAL,   // a prova não chegou; o reclamante tem 48 h para juntar mais
    EM_VERIFICACAO,    // à espera do funcionário
    APROVADA,          // pode levantar o objecto
    RECUSADA,
    EM_RECURSO,        // à espera do Gestor
    CONCLUIDA,         // objecto entregue
    EXPIRADA           // prazo não cumprido
}
