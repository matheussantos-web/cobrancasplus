package com.emprestimos.dto.response;

/**
 * Resposta de GET /api/auth/me: usuario autenticado pela sessao (cookie httpOnly).
 */
public record AuthMeResponse(
        Long id,
        String nome,
        String email,
        String chavePix) {
}