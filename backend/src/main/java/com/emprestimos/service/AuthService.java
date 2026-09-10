package com.emprestimos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emprestimos.dto.request.LoginRequest;
import com.emprestimos.dto.request.RegisterRequest;
import com.emprestimos.dto.response.AuthMeResponse;
import com.emprestimos.dto.response.AuthResponse;
import com.emprestimos.entity.Usuario;
import com.emprestimos.exception.CredenciaisInvalidasException;
import com.emprestimos.exception.UsuarioNaoAutenticadoException;
import com.emprestimos.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ValidacaoEmailService validacaoEmailService;
    private final boolean validarMx;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       ValidacaoEmailService validacaoEmailService,
                       @Value("${app.email.validar-mx:true}") boolean validarMx) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.validacaoEmailService = validacaoEmailService;
        this.validarMx = validarMx;
    }

    /**
     * Cria o usuario e devolve apenas a confirmacao (201). NAO autologa:
     * o cliente precisa passar por /api/auth/login para obter o cookie.
     * Antes de salvar, valida o dominio do e-mail (registros MX via DNS).
     */
    @Transactional
    public void registrar(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (validarMx) {
            validacaoEmailService.validarDominio(email);
        }

        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ja existe um usuario com este e-mail");
        }

        Usuario usuario = new Usuario(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()));

        if (request.chavePix() != null && !request.chavePix().isBlank()) {
            usuario.setChavePix(request.chavePix().trim());
        }

        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Ja existe um usuario com este e-mail");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse autenticar(LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException();
        }

        return emitirToken(usuario);
    }

    private AuthResponse emitirToken(Usuario usuario) {
        String token = jwtService.gerarToken(usuario);
        return AuthResponse.of(token, usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getChavePix());
    }

    /**
     * Dados do usuario autenticado pela cookie de sessao (GET /api/auth/me).
     */
    @Transactional(readOnly = true)
    public AuthMeResponse quemSouEu(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(u -> new AuthMeResponse(u.getId(), u.getNome(), u.getEmail(), u.getChavePix()))
                .orElseThrow(() -> new UsuarioNaoAutenticadoException("Sessao invalida"));
    }
}