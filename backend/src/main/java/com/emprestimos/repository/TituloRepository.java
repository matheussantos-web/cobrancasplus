package com.emprestimos.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emprestimos.entity.Titulo;
import com.emprestimos.enums.StatusTitulo;

@Repository
public interface TituloRepository extends JpaRepository<Titulo, Long> {

    // Todos os metodos de consulta filtram por usuarioId (isolamento multi-tenant).
    List<Titulo> findByUsuarioIdOrderByDataVencimentoAsc(Long usuarioId);

    List<Titulo> findByUsuarioIdAndStatus(Long usuarioId, StatusTitulo status);

    List<Titulo> findByUsuarioIdAndStatusAndDataVencimento(Long usuarioId, StatusTitulo status, LocalDate dataVencimento);

    List<Titulo> findByUsuarioIdAndStatusAndDataVencimentoLessThan(Long usuarioId, StatusTitulo status, LocalDate data);
}