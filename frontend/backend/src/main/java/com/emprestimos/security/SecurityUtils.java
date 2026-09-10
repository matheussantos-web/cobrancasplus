package com.emprestimos.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.emprestimos.exception.UsuarioNaoAutenticadoException;

/**
 * Utilitario para recuperar os dados do usuario autenticado a partir do
 * SecurityContext populado pelo JwtFilter.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UsuarioPrincipal usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new UsuarioNaoAutenticadoException("Sessao nao autenticada");
        }
        return principal;
    }

    /**
     * Id do usuario autenticado extraido do token JWT.
     * Todos os acessos a dados DEVEM usar este filtro (isolamento multi-tenant).
     */
    public static Long usuarioIdAtual() {
        return usuarioAtual().id();
    }
}