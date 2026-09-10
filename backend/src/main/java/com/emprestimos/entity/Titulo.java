package com.emprestimos.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.emprestimos.enums.StatusTitulo;

@Entity
@Table(name = "titulo")
public class Titulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "cliente_nome", nullable = false, length = 160)
    private String clienteNome;

    @Column(length = 500)
    private String descricao;

    @Column(name = "valor_original", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorOriginal;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    /** Porcentagem fixa de juros do contrato aplicada no vencimento (ex.: 30.00 para 30%). */
    @Column(name = "taxa_juros_fixa", nullable = false, precision = 8, scale = 3)
    private BigDecimal taxaJurosFixa;

    /** Porcentagem diaria cobrada somente apos o vencimento (ex.: 2.00 para 2% ao dia). */
    @Column(name = "taxa_juros_atraso_diaria", nullable = false, precision = 8, scale = 3)
    private BigDecimal taxaJurosAtrasoDiaria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusTitulo status = StatusTitulo.PENDENTE;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "valor_pago", precision = 14, scale = 2)
    private BigDecimal valorPago;

    protected Titulo() {
    }

    public Titulo(Usuario usuario, String clienteNome, String descricao,
                  BigDecimal valorOriginal, LocalDate dataVencimento,
                  BigDecimal taxaJurosFixa, BigDecimal taxaJurosAtrasoDiaria) {
        this.usuario = usuario;
        this.clienteNome = clienteNome;
        this.descricao = descricao;
        this.valorOriginal = valorOriginal;
        this.dataVencimento = dataVencimento;
        this.taxaJurosFixa = taxaJurosFixa;
        this.taxaJurosAtrasoDiaria = taxaJurosAtrasoDiaria;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    public void setValorOriginal(BigDecimal valorOriginal) {
        this.valorOriginal = valorOriginal;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public BigDecimal getTaxaJurosFixa() {
        return taxaJurosFixa;
    }

    public void setTaxaJurosFixa(BigDecimal taxaJurosFixa) {
        this.taxaJurosFixa = taxaJurosFixa;
    }

    public BigDecimal getTaxaJurosAtrasoDiaria() {
        return taxaJurosAtrasoDiaria;
    }

    public void setTaxaJurosAtrasoDiaria(BigDecimal taxaJurosAtrasoDiaria) {
        this.taxaJurosAtrasoDiaria = taxaJurosAtrasoDiaria;
    }

    public StatusTitulo getStatus() {
        return status;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public void darBaixa(LocalDate dataPagamento, BigDecimal valorPago) {
        this.status = StatusTitulo.PAGO;
        this.dataPagamento = dataPagamento;
        this.valorPago = valorPago;
    }
}