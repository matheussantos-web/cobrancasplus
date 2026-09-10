package com.emprestimos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.emprestimos.entity.Titulo;

class CalculoFinanceiroServiceTest {

    private CalculoFinanceiroService service;

    @BeforeEach
    void setup() {
        service = new CalculoFinanceiroService();
    }

    private Titulo titulo(BigDecimal valorOriginal, BigDecimal taxaFixa,
                          BigDecimal taxaAtrasoDiaria, LocalDate dataVencimento) {
        return new Titulo(null, "Cliente Teste", null, valorOriginal, dataVencimento,
                taxaFixa, taxaAtrasoDiaria);
    }

    @Test
    @DisplayName("Emprestimo de R$ 1.000 a 30% vale R$ 1.300 no vencimento")
    void valorNoVencimento_comJurosDeContrato() {
        Titulo t = titulo(new BigDecimal("1000.00"), new BigDecimal("30.00"),
                new BigDecimal("2.00"), LocalDate.now().plusDays(1));

        assertEquals(new BigDecimal("1300.00"), service.calcularValorVencimento(t));
        assertEquals(new BigDecimal("0.00"), service.calcularJurosAtraso(t));
        assertEquals(new BigDecimal("1300.00"), service.calcularValorTotal(t));
    }

    @Test
    @DisplayName("5 dias de atraso a 2%/dia sobre R$ 1.300 gera total de R$ 1.430")
    void cincoDiasDeAtraso_generaJurosDeAtraso() {
        Titulo t = titulo(new BigDecimal("1000.00"), new BigDecimal("30.00"),
                new BigDecimal("2.00"), LocalDate.now().minusDays(5));

        assertEquals(5, service.calcularDiasAtraso(t.getDataVencimento()));
        assertEquals(new BigDecimal("130.00"), service.calcularJurosAtraso(t));
        assertEquals(new BigDecimal("1430.00"), service.calcularValorTotal(t));
    }

    @Test
    @DisplayName("Arredondamento HALF_UP em valor de contrato e juros de atraso")
    void arredondamento_halfUp() {
        // 1234.56 + 7% = 1320.9792 -> 1320.98
        Titulo noPrazo = titulo(new BigDecimal("1234.56"), new BigDecimal("7.00"),
                new BigDecimal("2.50"), LocalDate.now().plusDays(1));
        assertEquals(new BigDecimal("1320.98"), service.calcularValorVencimento(noPrazo));

        // 3 dias a 2,5%/dia sobre 1320.98 = 99.0735 -> 99.07; total = 1420.05
        Titulo atrasado = titulo(new BigDecimal("1234.56"), new BigDecimal("7.00"),
                new BigDecimal("2.50"), LocalDate.now().minusDays(3));
        assertEquals(new BigDecimal("99.07"), service.calcularJurosAtraso(atrasado));
        assertEquals(new BigDecimal("1420.05"), service.calcularValorTotal(atrasado));
    }

    @Test
    @DisplayName("Baixa no vencimento usa valor do contrato; baixa com atraso soma juros")
    void valorDeBaixa_respeitaDataDePagamento() {
        Titulo t = titulo(new BigDecimal("1000.00"), new BigDecimal("30.00"),
                new BigDecimal("2.00"), LocalDate.now());

        assertEquals(new BigDecimal("1300.00"),
                service.calcularValorBaixa(t, t.getDataVencimento()));

        BigDecimal pagamentoComAtraso = service.calcularValorBaixa(t, t.getDataVencimento().plusDays(5));
        assertEquals(new BigDecimal("1430.00"), pagamentoComAtraso);
    }

    @Test
    @DisplayName("Titulo pago nao gera mais juros de atraso")
    void tituloPago_noAcumulaJuros() {
        Titulo t = titulo(new BigDecimal("1000.00"), new BigDecimal("30.00"),
                new BigDecimal("2.00"), LocalDate.now().minusDays(10));
        t.darBaixa(LocalDate.now().minusDays(2), new BigDecimal("1430.00"));

        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                service.calcularJurosAtraso(t));
        assertEquals(new BigDecimal("1300.00"), service.calcularValorTotal(t));
    }
}