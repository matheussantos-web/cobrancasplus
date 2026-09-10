package com.emprestimos.security;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limita as tentativas de login a {@link #MAX_TENTATIVAS} por minuto por IP
 * (janela deslizante). Acima do limite responde HTTP 429 com ProblemDetail,
 * servindo como protecao basica contra forca bruta em /api/auth/login.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final int MAX_TENTATIVAS = 5;
    static final Duration JANELA = Duration.ofMinutes(1);
    private static final String CAMINHO_LOGIN = "/api/auth/login";

    private final ConcurrentHashMap<String, Deque<Long>> registros = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && CAMINHO_LOGIN.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = resolverIp(request);
        long agora = System.currentTimeMillis();
        Deque<Long> fila = registros.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (fila) {
            expurgarExpirados(fila, agora - JANELA.toMillis());
            if (fila.size() >= MAX_TENTATIVAS) {
                ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas tentativas de login. Aguarde 1 minuto e tente novamente.");
                problema.setTitle("Limite de tentativas excedido");
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(), problema);
                return;
            }
            fila.addLast(agora);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Rotina horaria (cron {@code 0 0 * * * *}) que remove do mapa os
     * registros de IPs sem tentativas dentro da janela, evitando
     * crescimento indefinido do {@link ConcurrentHashMap}.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void expurgarIpsInativos() {
        long limite = System.currentTimeMillis() - JANELA.toMillis();
        registros.entrySet().removeIf(entrada -> {
            synchronized (entrada.getValue()) {
                expurgarExpirados(entrada.getValue(), limite);
                return entrada.getValue().isEmpty();
            }
        });
    }

    private void expurgarExpirados(Deque<Long> fila, long limiteAntesDe) {
        while (!fila.isEmpty() && fila.peekFirst() < limiteAntesDe) {
            fila.pollFirst();
        }
    }

    private String resolverIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}