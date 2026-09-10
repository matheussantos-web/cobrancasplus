package com.emprestimos.exception;

/**
 * Violacao de regra de negocio (validacoes de dominio, estado etc.).
 * Mapeada pelo GlobalExceptionHandler para 400 Bad Request em ProblemDetail.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}