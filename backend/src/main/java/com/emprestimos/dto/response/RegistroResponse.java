package com.emprestimos.dto.response;

/**
 * Corpo da resposta de registro (201 Created). O registro NAO autologa:
 * o usuario precisa fazer login manualmente para receber o cookie de sessao.
 */
public record RegistroResponse(String mensagem) {
}