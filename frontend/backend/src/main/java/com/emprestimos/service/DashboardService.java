package com.emprestimos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emprestimos.dto.response.DashboardResponse;
import com.emprestimos.entity.Titulo;
import com.emprestimos.enums.StatusTitulo;
import com.emprestimos.repository.TituloRepository;

@Service
public class DashboardService {

    private final TituloRepository tituloRepository;
    private final CalculoFinanceiroService calculoService;

    public DashboardService(TituloRepository tituloRepository,
                            CalculoFinanceiroService calculoService) {
        this.tituloRepository = tituloRepository;
        this.calculoService = calculoService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo(Long usuarioId) {
        List<Titulo> pendentes = tituloRepository
                .findByUsuarioIdAndStatus(usuarioId, StatusTitulo.PENDENTE);
        List<Titulo> pagos = tituloRepository
                .findByUsuarioIdAndStatus(usuarioId, StatusTitulo.PAGO);

        LocalDate hoje = calculoService.dataAtual();

        BigDecimal totalAReceber = BigDecimal.ZERO;
        BigDecimal jurosAcumulados = BigDecimal.ZERO;
        long vencemHojeCount = 0;
        BigDecimal totalVencemHoje = BigDecimal.ZERO;
        long emAtrasoCount = 0;
        BigDecimal totalEmAtraso = BigDecimal.ZERO;

        for (Titulo t : pendentes) {
            BigDecimal total = calculoService.calcularValorTotal(t);
            BigDecimal juros = calculoService.calcularJurosAtraso(t);
            totalAReceber = totalAReceber.add(total);
            jurosAcumulados = jurosAcumulados.add(juros);

            if (t.getDataVencimento().isBefore(hoje)) {
                emAtrasoCount++;
                totalEmAtraso = totalEmAtraso.add(total);
            } else if (t.getDataVencimento().isEqual(hoje)) {
                vencemHojeCount++;
                totalVencemHoje = totalVencemHoje.add(total);
            }
        }

        BigDecimal totalBaixado = pagos.stream()
                .map(Titulo::getValorPago)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardResponse.of(
                totalAReceber.setScale(2, RoundingMode.HALF_UP),
                pendentes.size(),
                jurosAcumulados.setScale(2, RoundingMode.HALF_UP),
                vencemHojeCount,
                totalVencemHoje.setScale(2, RoundingMode.HALF_UP),
                emAtrasoCount,
                totalEmAtraso.setScale(2, RoundingMode.HALF_UP),
                totalBaixado.setScale(2, RoundingMode.HALF_UP));
    }
}
