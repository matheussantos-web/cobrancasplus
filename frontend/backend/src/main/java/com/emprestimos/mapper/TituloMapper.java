package com.emprestimos.mapper;

import org.springframework.stereotype.Component;

import com.emprestimos.dto.response.TituloResponse;
import com.emprestimos.entity.Titulo;
import com.emprestimos.service.CalculoFinanceiroService;

/**
 * Converte a entidade {@link Titulo} para o DTO de resposta
 * {@link TituloResponse}, centralizando a montagem com os valores
 * financeiros calculados pelo {@link CalculoFinanceiroService}
 * (fonte unica de verdade).
 */
@Component
public class TituloMapper {

    private final CalculoFinanceiroService calculoService;

    public TituloMapper(CalculoFinanceiroService calculoService) {
        this.calculoService = calculoService;
    }

    public TituloResponse paraResposta(Titulo t) {
        return new TituloResponse(
                t.getId(),
                t.getClienteNome(),
                t.getDescricao(),
                t.getValorOriginal(),
                t.getDataVencimento(),
                t.getTaxaJurosFixa(),
                t.getTaxaJurosAtrasoDiaria(),
                calculoService.calcularValorVencimento(t),
                t.getStatus(),
                t.getDataPagamento(),
                t.getValorPago(),
                calculoService.calcularDiasAtraso(t.getDataVencimento()),
                calculoService.calcularJurosAtraso(t),
                calculoService.calcularValorTotal(t),
                calculoService.categoria(t));
    }
}