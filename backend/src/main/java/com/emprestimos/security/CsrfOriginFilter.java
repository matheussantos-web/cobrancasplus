package com.emprestimos.security;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Camada extra de protecao CSRF para rotas de mutacao (POST/PUT/PATCH/DELETE):
 * valida o cabecalho Origin (ou Referer) — aceita mesma origem da requisicao ou
 * origens da lista configurada em CORS_ALLOWED_ORIGINS. Pedidos sem Origin e sem
 * Referer (clientes nao navegador, ex.: curl) sao permitidos. A principal
 * protecao continua sendo o cookie sameSite=Strict.
 */
@Component
public class CsrfOriginFilter extends OncePerRequestFilter {

    private final String[] origensPermitidas;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CsrfOriginFilter(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.origensPermitidas = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!isMutacao(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        if (origin != null) {
            if (isOrigemPermitida(origin, request)) {
                filterChain.doFilter(request, response);
            } else {
                negar(response);
            }
            return;
        }

        // Sem Origin (NAO e navegador) -> permitir. Com Referer, valida a origem dele.
        String referer = request.getHeader("Referer");
        if (referer != null && !isOrigemPermitida(extrairOrigem(referer), request)) {
            negar(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isMutacao(String metodo) {
        return "POST".equals(metodo) || "PUT".equals(metodo)
                || "PATCH".equals(metodo) || "DELETE".equals(metodo);
    }

    private boolean isOrigemPermitida(String origem, HttpServletRequest request) {
        if (origem.equalsIgnoreCase(mesmaOrigem(request))) {
            return true;
        }
        for (String permitida : origensPermitidas) {
            if (origem.equalsIgnoreCase(permitida)) {
                return true;
            }
        }
        return false;
    }

    private String mesmaOrigem(HttpServletRequest request) {
        String scheme = request.isSecure() ? "https" : "http";
        int porta = request.getServerPort();
        boolean portaPadrao = (porta == 80 && "http".equals(scheme))
                || (porta == 443 && "https".equals(scheme));
        return portaPadrao
                ? scheme + "://" + request.getServerName()
                : scheme + "://" + request.getServerName() + ":" + porta;
    }

    private String extrairOrigem(String url) {
        try {
            URI uri = URI.create(url);
            int porta = uri.getPort();
            return porta == -1
                    ? uri.getScheme() + "://" + uri.getHost()
                    : uri.getScheme() + "://" + uri.getHost() + ":" + porta;
        } catch (Exception e) {
            return url;
        }
    }

    private void negar(HttpServletResponse response) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Origem de requisicao nao permitida");
        problema.setTitle("Origem bloqueada");

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), problema);
    }
}