package com.emprestimos.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.emprestimos.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Servico de emissao e validacao de JSON Web Tokens (JWT).
 */
@Service
public class JwtService {

    private static final String CLAIM_USUARIO_ID = "usuarioId";
    private static final String CLAIM_NOME = "nome";

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.expiration-ms}") long expiracaoMs) {
        this.chave = construirChave(segredo);
        this.expiracaoMs = expiracaoMs;
    }

    private SecretKey construirChave(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente — defina a variavel de ambiente JWT_SECRET "
                            + "(texto ou base64, minimo 48 bytes) antes de iniciar a aplicacao.");
        }

        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(segredo.trim());
        } catch (IllegalArgumentException | io.jsonwebtoken.io.DecodingException e) {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
        }

        // HS384 exige chave de pelo menos 384 bits (48 bytes).
        if (bytes.length < 48) {
            throw new IllegalStateException(
                    "JWT_SECRET fraca — o segredo precisa de pelo menos 48 bytes (384 bits) para HS384; "
                            + "o valor informado tem " + bytes.length + " byte(s).");
        }

        return Keys.hmacShaKeyFor(bytes);
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_NOME, usuario.getNome())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoMs)))
                .signWith(chave)
                .compact();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extrairEmail(String token) {
        return extrairClaims(token).getSubject();
    }

    public Long extrairUsuarioId(String token) {
        return extrairClaims(token).get(CLAIM_USUARIO_ID, Long.class);
    }

    public boolean tokenValido(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}