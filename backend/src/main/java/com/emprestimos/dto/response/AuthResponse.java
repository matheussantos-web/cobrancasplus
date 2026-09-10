package com.emprestimos.dto.response;

public record AuthResponse(
        String token,
        String tipo,
        Long usuarioId,
        String nome,
        String email,
        String chavePix) {

    public static AuthResponse of(String token, Long usuarioId, String nome, String email, String chavePix) {
        return new AuthResponse(token, "Bearer", usuarioId, nome, email, chavePix);
    }
}