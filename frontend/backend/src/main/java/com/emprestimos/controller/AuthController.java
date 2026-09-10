package com.emprestimos.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emprestimos.dto.request.LoginRequest;
import com.emprestimos.dto.request.RegisterRequest;
import com.emprestimos.dto.response.AuthMeResponse;
import com.emprestimos.dto.response.AuthResponse;
import com.emprestimos.dto.response.RegistroResponse;
import com.emprestimos.security.JwtFilter;
import com.emprestimos.security.SecurityUtils;
import com.emprestimos.service.AuthService;

import jakarta.validation.Valid;

/**
 * Autenticacao: login seta o JWT em cookie httpOnly (e devolve o corpo para
 * compatibilidade); registro apenas cria a conta (201, sem cookie — login
 * manual obrigatorio); /me valida a sessao; /logout expira o cookie.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final long expiracaoMs;

    public AuthController(AuthService authService,
                          @Value("${app.cookie.secure}") boolean cookieSecure,
                          @Value("${app.cookie.same-site}") String cookieSameSite,
                          @Value("${app.jwt.expiration-ms}") long expiracaoMs) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.expiracaoMs = expiracaoMs;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistroResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistroResponse("Conta criada com sucesso! Faca login para continuar."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> autenticar(@Valid @RequestBody LoginRequest request) {
        return comCookie(ResponseEntity.ok(), authService.autenticar(request));
    }

    @GetMapping("/me")
    public AuthMeResponse eu() {
        return authService.quemSouEu(SecurityUtils.usuarioIdAtual());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> sair() {
        ResponseCookie cookie = ResponseCookie.from(JwtFilter.COOKIE_TOKEN, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseEntity<AuthResponse> comCookie(ResponseEntity.BodyBuilder base, AuthResponse resposta) {
        return base.header(HttpHeaders.SET_COOKIE, cookieToken(resposta.token()).toString())
                .body(resposta);
    }

    private ResponseCookie cookieToken(String token) {
        return ResponseCookie.from(JwtFilter.COOKIE_TOKEN, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofMillis(expiracaoMs).getSeconds())
                .build();
    }
}