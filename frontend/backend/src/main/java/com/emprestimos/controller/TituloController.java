package com.emprestimos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emprestimos.dto.request.TituloRequest;
import com.emprestimos.dto.response.TituloResponse;
import com.emprestimos.security.SecurityUtils;
import com.emprestimos.service.TituloService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/titulos")
public class TituloController {

    private final TituloService tituloService;

    public TituloController(TituloService tituloService) {
        this.tituloService = tituloService;
    }

    @GetMapping
    public List<TituloResponse> listar() {
        return tituloService.listar(SecurityUtils.usuarioIdAtual());
    }

    @PostMapping
    public ResponseEntity<TituloResponse> criar(@Valid @RequestBody TituloRequest request) {
        TituloResponse response = tituloService.criar(SecurityUtils.usuarioIdAtual(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/baixa")
    public TituloResponse darBaixa(@PathVariable Long id) {
        return tituloService.darBaixa(SecurityUtils.usuarioIdAtual(), id);
    }

    @PutMapping("/{id}")
    public TituloResponse editar(@PathVariable Long id, @Valid @RequestBody TituloRequest request) {
        return tituloService.editar(SecurityUtils.usuarioIdAtual(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        tituloService.excluir(SecurityUtils.usuarioIdAtual(), id);
        return ResponseEntity.noContent().build();
    }
}