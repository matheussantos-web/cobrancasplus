package com.emprestimos.exception;

/**
 * Lancada quando o e-mail ou a senha informados no login sao invalidos.
 * Tratada no GlobalExceptionHandler como HTTP 401 (nao autenticado),
 * mantendo a mensagem generica para nao vazar a existencia de contas.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais invalidas");
    }
}