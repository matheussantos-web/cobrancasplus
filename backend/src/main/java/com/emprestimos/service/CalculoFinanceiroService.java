package com.emprestimos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.emprestimos.entity.Titulo;
import com.emprestimos.enums.StatusTitulo;

/**
 * Servico financeiro responsavel pelo calculo dos valores de cada titulo.
 * Fonte unica de verdade para todos os valores financeiros (o front-end apenas
 * renderiza os campos calculados retornados pela API).
 *
 * Regra de negocio (juros de contrato + juros de atraso):
 *   valorVencimento = valorOriginal + (valorOriginal * taxaJurosFixa / 100)
 *   diasAtraso      = dataConsulta - dataVencimento                 (0 se <= vencimento)
 *   jurosAtraso     = valorVencimento * (taxaJurosAtrasoDiaria / 100) * diasAtraso
 *   valorTotal      = valorVencimento + jurosAtraso                 (sem atraso = valorVencimento)
 *
 * Todos os resultados sao arredondados com RoundingMode.HALF_UP em 2 casas.
 */
@Service
public class CalculoFinanceiroService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final int ESCALA = 2;

    public LocalDate dataAtual() {
        return LocalDate.now();
    }

    /**
     * Numero de dias de atraso em relacao a data atual. Retorna 0 se o titulo
     * ainda nao venceu.
     */
    public long calcularDiasAtraso(LocalDate dataVencimento) {
        if (dataVencimento == null) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(dataVencimento, dataAtual());
        return Math.max(0, dias);
    }

    /**
     * Valor no vencimento: valorOriginal acrescido dos juros do contrato.
     */
    public BigDecimal calcularValorVencimento(Titulo titulo) {
        BigDecimal jurosContrato = titulo.getValorOriginal()
                .multiply(titulo.getTaxaJurosFixa())
                .divide(CEM, ESCALA, RoundingMode.HALF_UP);
        return titulo.getValorOriginal()
                .add(jurosContrato)
                .setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Juros de atraso acumulados ate hoje (zero para titulo PAGO ou sem atraso).
     */
    public BigDecimal calcularJurosAtraso(Titulo titulo) {
        long dias = calcularDiasAtraso(titulo.getDataVencimento());
        if (titulo.getStatus() == StatusTitulo.PAGO) {
            dias = 0;
        }
        if (dias <= 0) {
            return BigDecimal.ZERO.setScale(ESCALA, RoundingMode.HALF_UP);
        }
        return calcularValorVencimento(titulo)
                .multiply(titulo.getTaxaJurosAtrasoDiaria())
                .multiply(BigDecimal.valueOf(dias))
                .divide(CEM, ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Valor total = valorVencimento + juros de atraso acumulados ate hoje.
     * Sem atraso o valor total e exatamente o valor no vencimento.
     */
    public BigDecimal calcularValorTotal(Titulo titulo) {
        return calcularValorVencimento(titulo)
                .add(calcularJurosAtraso(titulo))
                .setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Valor final da baixa (pagamento): valor do contrato + juros de atraso
     * contados apenas ate a data do pagamento (permite baixa na data ou retroativa).
     */
    public BigDecimal calcularValorBaixa(Titulo titulo, LocalDate dataPagamento) {
        BigDecimal valorVencimento = calcularValorVencimento(titulo);
        long dias = Math.max(0, ChronoUnit.DAYS.between(titulo.getDataVencimento(), dataPagamento));
        if (dias <= 0) {
            return valorVencimento;
        }
        BigDecimal juros = valorVencimento
                .multiply(titulo.getTaxaJurosAtrasoDiaria())
                .multiply(BigDecimal.valueOf(dias))
                .divide(CEM, ESCALA, RoundingMode.HALF_UP);
        return valorVencimento.add(juros).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Categoriza o titulo com base na data de vencimento e na data atual.
     */
    public String categoria(Titulo titulo) {
        if (titulo.getStatus() == StatusTitulo.PAGO) {
            return "BAIXADO";
        }
        LocalDate hoje = dataAtual();
        if (titulo.getDataVencimento().isBefore(hoje)) {
            return "EM_ATRASO";
        }
        if (titulo.getDataVencimento().isEqual(hoje)) {
            return "VENCE_HOJE";
        }
        return "A_VENCER";
    }
}