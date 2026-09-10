package com.emprestimos.dto.response;

import java.math.BigDecimal;

public record DashboardResponse(
        BigDecimal totalAReceber,
        long titulosPendentes,
        BigDecimal jurosAcumulados,
        long titulosVencemHoje,
        BigDecimal totalVencemHoje,
        long titulosEmAtraso,
        BigDecimal totalEmAtraso,
        BigDecimal totalBaixado) {

    public static DashboardResponse of(BigDecimal totalAReceber, long titulosPendentes,
                                       BigDecimal jurosAcumulados, long titulosVencemHoje,
                                       BigDecimal totalVencemHoje, long titulosEmAtraso,
                                       BigDecimal totalEmAtraso, BigDecimal totalBaixado) {
        return new DashboardResponse(totalAReceber, titulosPendentes, jurosAcumulados,
                titulosVencemHoje, totalVencemHoje, titulosEmAtraso, totalEmAtraso, totalBaixado);
    }
}