package com.emprestimos.service;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.stereotype.Service;

import com.emprestimos.exception.RegraNegocioException;

/**
 * Validacao adicional de e-mail no registro: alem da sintaxe (dada pelo
 * @Email no DTO), garante que o dominio do e-mail possua registros MX
 * (servidores de e-mail) consultados via DNS — bloqueia dominios
 * inexistentes ou aleatorios (ex.: usuario@dominioqnaoexiste.com).
 *
 * O metodo {@link #possuiRegistroMx(String)} e protected para permitir
 * stub nos testes de unidade (sem chamada de rede).
 */
@Service
public class ValidacaoEmailService {

    private static final String FABRICA_DIRECTORY = "com.sun.jndi.dns.DnsContextFactory";
    private static final String PROVEDOR_DNS = "dns:";
    private static final String[] TIPOS_MX = { "MX" };

    private static final String MENSAGEM_DOMINIO_INVALIDO =
            "O domínio do e-mail digitado não é válido para recebimento de mensagens.";

    public void validarDominio(String email) {
        String dominio = extrairDominio(email);
        if (dominio == null || !possuiRegistroMx(dominio)) {
            throw new RegraNegocioException(MENSAGEM_DOMINIO_INVALIDO);
        }
    }

    protected String extrairDominio(String email) {
        if (email == null) {
            return null;
        }
        int indice = email.lastIndexOf('@');
        if (indice < 0 || indice == email.length() - 1) {
            return null;
        }
        String dominio = email.substring(indice + 1).trim().toLowerCase();
        return dominio.isBlank() ? null : dominio;
    }

    protected boolean possuiRegistroMx(String dominio) {
        Hashtable<String, String> ambiente = new Hashtable<>();
        ambiente.put(Context.INITIAL_CONTEXT_FACTORY, FABRICA_DIRECTORY);
        ambiente.put(Context.PROVIDER_URL, PROVEDOR_DNS);

        try {
            DirContext contexto = new InitialDirContext(ambiente);
            try {
                Attributes atributos = contexto.getAttributes(dominio, TIPOS_MX);
                return atributos != null && atributos.get("MX") != null;
            } finally {
                contexto.close();
            }
        } catch (NamingException e) {
            return false;
        }
    }
}