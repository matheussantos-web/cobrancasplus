package com.emprestimos.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.emprestimos.exception.RegraNegocioException;

class ValidacaoEmailServiceTest {

    /** Stub que nao consulta DNS: controla o resultado da verificacao de MX. */
    private static class ValidadorControlado extends ValidacaoEmailService {
        private final boolean temMx;

        ValidadorControlado(boolean temMx) {
            this.temMx = temMx;
        }

        @Override
        protected boolean possuiRegistroMx(String dominio) {
            return temMx;
        }
    }

    @Test
    void dominioSemRegistroMxLancaRegraNegocio() {
        ValidacaoEmailService validador = new ValidadorControlado(false);
        assertThrows(RegraNegocioException.class,
                () -> validador.validarDominio("teste@sdfjsdkf.com"));
    }

    @Test
    void dominioComRegistroMxNaoLancaExcecao() {
        ValidacaoEmailService validador = new ValidadorControlado(true);
        assertDoesNotThrow(() -> validador.validarDominio("ana@gmail.com"));
    }

    @Test
    void emailSemArrobaLancaRegraNegocio() {
        ValidacaoEmailService validador = new ValidadorControlado(true);
        assertThrows(RegraNegocioException.class, () -> validador.validarDominio("sem-arroba"));
    }

    @Test
    void dominioVazioLancaRegraNegocio() {
        ValidacaoEmailService validador = new ValidadorControlado(true);
        assertThrows(RegraNegocioException.class, () -> validador.validarDominio("usuario@"));
    }
}