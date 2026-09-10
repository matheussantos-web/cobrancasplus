package com.emprestimos.security;

/**
 * Principal autenticado injetado no SecurityContext a partir das claims do JWT.
 */
public record UsuarioPrincipal(Long id, String nome, String email) {
}