package model;

/** Erro quando o utilizador tenta fazer algo que as regras do sistema não permitem. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
