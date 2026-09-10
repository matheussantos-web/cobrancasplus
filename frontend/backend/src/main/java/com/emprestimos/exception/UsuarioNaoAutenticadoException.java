package com.emprestimos.exception;

/**
 * Lancada quando uma requisicao protegida chega sem um usuario autenticado
 * no SecurityContext (token ausente, invalido ou expirado).
 */
public class UsuarioNaoAutenticadoException extends RuntimeException {

    public UsuarioNaoAutenticadoException(String message) {
        super(message);
    }
}