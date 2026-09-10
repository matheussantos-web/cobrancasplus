package com.emprestimos.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Testes de integracao da API via MockMvc:
 * (a) isolamento multi-tenant entre usuarios;
 * (b) HTTP 401 (token ausente/invalido) e 429 (rate limit de login) no
 * padrao ProblemDetail (RFC 7807).
 *
 * A ordem dos metodos e relevante: o teste de rate limit consome toda a
 * janela permitida para o IP 127.0.0.1 e deve rodar por ultimo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TituloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String novoEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    private void registrar(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailJson(email)))
                .andExpect(status().isCreated());
    }

    /**
     * Autentica o usuario. Usa um IP unico via X-Forwarded-For para nao
     * consumir a janela de rate limit compartilhada do 127.0.0.1, que e
     * reservada ao teste de 429 (executado por ultimo).
     */
    private String logar(String email) throws Exception {
        String ip = "10.99.0." + origem(email);
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("token").asText();
    }

    /** Deriva o 4o octeto do IP a partir do hash do email (1..254). */
    private int origem(String email) {
        return 1 + Math.floorMod(email.hashCode(), 254);
    }

    private String emailJson(String email) {
        return "{\"nome\":\"Usuario Teste\",\"email\":\"" + email + "\",\"senha\":\"123456\"}";
    }

    private long criarTitulo(String token) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/titulos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteNome\":\"Cliente A\",\"descricao\":\"Teste isolamento\","
                                + "\"valorOriginal\":1000.00,\"dataVencimento\":\"2999-12-31\","
                                + "\"taxaJurosFixa\":30,\"taxaJurosAtrasoDiaria\":2}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    @Test
    @Order(1)
    void isolamentoMultiTenantBloqueiaAcessoATitulosDeOutroUsuario() throws Exception {
        String emailA = novoEmail();
        String emailB = novoEmail();

        registrar(emailA);
        String tokenA = logar(emailA);

        long idTituloA = criarTitulo(tokenA);

        // Usuario A ve apenas os proprios titulos.
        mockMvc.perform(get("/api/titulos")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Usuario B nao ve titulos de A...
        registrar(emailB);
        String tokenB = logar(emailB);

        mockMvc.perform(get("/api/titulos")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // ...e nao consegue dar baixa no titulo de A (404).
        mockMvc.perform(patch("/api/titulos/" + idTituloA + "/baixa")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(2)
    void edicaoSomenteDoProprietario() throws Exception {
        String emailA = novoEmail();
        String emailB = novoEmail();

        registrar(emailA);
        String tokenA = logar(emailA);
        long idTituloA = criarTitulo(tokenA);

        String corpo = "{\"clienteNome\":\"Cliente Editado\",\"descricao\":\"Contrato renovado\","
                + "\"valorOriginal\":1500.00,\"dataVencimento\":\"2999-12-31\","
                + "\"taxaJurosFixa\":25,\"taxaJurosAtrasoDiaria\":1.5}";

        // Dono edita o titulo (PENDENTE) -> 200 com dados recalculados.
        mockMvc.perform(put("/api/titulos/" + idTituloA)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteNome", is("Cliente Editado")))
                .andExpect(jsonPath("$.descricao", is("Contrato renovado")))
                .andExpect(jsonPath("$.valorOriginal", is(1500.00)))
                .andExpect(jsonPath("$.valorVencimento", is(1875.00)));

        // Outro usuario nao consegue editar titulo de A -> 404 (isolamento).
        registrar(emailB);
        String tokenB = logar(emailB);

        mockMvc.perform(put("/api/titulos/" + idTituloA)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    void exclusaoSomenteDoProprietario() throws Exception {
        String emailA = novoEmail();
        String emailB = novoEmail();

        registrar(emailA);
        String tokenA = logar(emailA);
        long idTituloA = criarTitulo(tokenA);

        // Outro usuario nao consegue excluir titulo de A -> 404 (isolamento).
        registrar(emailB);
        String tokenB = logar(emailB);

        mockMvc.perform(delete("/api/titulos/" + idTituloA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // O titulo continua visivel para o dono.
        mockMvc.perform(get("/api/titulos")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Dono exclui -> 204 No Content e lista fica vazia.
        mockMvc.perform(delete("/api/titulos/" + idTituloA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/titulos")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @Order(4)
    void acessoSemTokenRetorna401ComProblemDetail() throws Exception {
        mockMvc.perform(get("/api/titulos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Nao autenticado")));

        mockMvc.perform(get("/api/titulos")
                        .header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void loginExcedeOLimiteRetorna429ComProblemDetail() throws Exception {
        int primeira = 0;
        Integer primeiroStatus = null;
        Integer ultimoStatus = null;

        for (int i = 0; i < 6; i++) {
            MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nao-existe@teste.com\",\"senha\":\"errada\"}"))
                    .andReturn();
            int codigo = resultado.getResponse().getStatus();
            if (i == primeira) {
                primeiroStatus = codigo;
            }
            ultimoStatus = codigo;
        }

        // Credenciais incorretas: 401. Apos a janela de 5 tentativas: 429.
        org.assertj.core.api.Assertions.assertThat(primeiroStatus).isEqualTo(401);
        org.assertj.core.api.Assertions.assertThat(ultimoStatus).isEqualTo(429);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-existe@teste.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(429)));
    }
}