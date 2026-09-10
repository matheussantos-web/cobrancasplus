package com.emprestimos.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TituloRequest(
        @NotBlank(message = "O nome do cliente e obrigatorio")
        @Size(max = 160, message = "O nome do cliente deve ter no maximo 160 caracteres")
        String clienteNome,

        @Size(max = 500, message = "A descricao deve ter no maximo 500 caracteres")
        String descricao,

        @NotNull(message = "O valor original e obrigatorio")
        @DecimalMin(value = "0.01", message = "O valor original deve ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "Valor original invalido")
        BigDecimal valorOriginal,

        @NotNull(message = "A data de vencimento e obrigatoria")
        LocalDate dataVencimento,

        @NotNull(message = "A taxa de juros do contrato e obrigatoria")
        @DecimalMin(value = "0", message = "A taxa de juros do contrato deve ser maior ou igual a zero")
        @Digits(integer = 5, fraction = 3, message = "Taxa de juros do contrato invalida")
        BigDecimal taxaJurosFixa,

        @NotNull(message = "A taxa de juros de atraso diaria e obrigatoria")
        @DecimalMin(value = "0", message = "A taxa de juros de atraso diaria deve ser maior ou igual a zero")
        @Digits(integer = 5, fraction = 3, message = "Taxa de juros de atraso diaria invalida")
        BigDecimal taxaJurosAtrasoDiaria) {
}