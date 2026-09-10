package com.emprestimos.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.JwtException;

/**
 * Tratamento global de excecoes: converte as excecoes do dominio e de
 * validacao em respostas HTTP padronizadas (ProblemDetail para 401/429 e
 * JSON {mensagem, erros} para os demais).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsuarioNaoAutenticadoException.class)
    public ResponseEntity<ProblemDetail> usuarioNaoAutenticado(UsuarioNaoAutenticadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problema.setTitle("Nao autenticado");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ProblemDetail> credenciaisInvalidas(CredenciaisInvalidasException ex) {
        log.warn("Tentativa de login com credenciais invalidas");
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problema.setTitle("Nao autenticado");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ProblemDetail> regraNegocio(RegraNegocioException ex) {
        log.warn("Regra de negocio violada: {}", ex.getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Regra de negocio violada");
        problema.setProperty("mensagem", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problema);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> camposInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) {
            erros.put(e.getField(), e.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(Map.of("mensagem", "Dados invalidos", "erros", erros));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(IllegalArgumentException ex) {
        log.warn("Argumento invalido: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> estadoInvalido(IllegalStateException ex) {
        log.warn("Estado invalido: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(java.util.NoSuchElementException ex) {
        log.warn("Recurso nao encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("mensagem", ex.getMessage()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> tokenInvalido(JwtException ex) {
        log.warn("Token invalido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("mensagem", "Token invalido ou expirado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erroGenerico(Exception ex) {
        log.error("Erro interno", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensagem", "Erro interno do servidor"));
    }
}