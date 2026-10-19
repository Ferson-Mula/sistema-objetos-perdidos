package model;

/** Lançada quando uma operação viola uma regra de negócio (RN01 a RN20). */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
