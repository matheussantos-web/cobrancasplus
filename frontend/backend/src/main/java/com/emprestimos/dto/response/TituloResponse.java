package com.emprestimos.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.emprestimos.enums.StatusTitulo;

/**
 * Resposta de titulo com os valores financeiros calculados pelo back-end
 * (fonte unica de verdade): valor no vencimento, juros de atraso acumulados,
 * total atualizado e categorizacao.
 *
 * Regra:
 *   valorVencimento = valorOriginal + (valorOriginal * taxaJurosFixa / 100)
 *   jurosAcumulados = valorVencimento * (taxaJurosAtrasoDiaria / 100) * diasAtraso
 *   valorTotal       = valorVencimento + jurosAcumulados
 */
public record TituloResponse(
        Long id,
        String clienteNome,
        String descricao,
        BigDecimal valorOriginal,
        LocalDate dataVencimento,
        BigDecimal taxaJurosFixa,
        BigDecimal taxaJurosAtrasoDiaria,
        BigDecimal valorVencimento,
        StatusTitulo status,
        LocalDate dataPagamento,
        BigDecimal valorPago,
        long diasAtraso,
        BigDecimal jurosAcumulados,
        BigDecimal valorTotal,
        String categoria) {
}