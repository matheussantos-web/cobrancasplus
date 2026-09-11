-- Estrutura inicial para PostgreSQL: usuario e titulo.
-- Regra de juros: taxa_juros_fixa (contrato) + taxa_juros_atraso_diaria (somente em atraso).

CREATE TABLE usuario (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL UNIQUE,
    senha      VARCHAR(100) NOT NULL,
    criado_em  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE titulo (
    id                        BIGSERIAL PRIMARY KEY,
    usuario_id                BIGINT NOT NULL,
    cliente_nome              VARCHAR(160) NOT NULL,
    descricao                 VARCHAR(500),
    valor_original            DECIMAL(14,2) NOT NULL,
    data_vencimento           DATE NOT NULL,
    taxa_juros_fixa           DECIMAL(8,3) NOT NULL,
    taxa_juros_atraso_diaria  DECIMAL(8,3) NOT NULL,
    status                    VARCHAR(10) NOT NULL DEFAULT 'PENDENTE',
    data_pagamento            DATE,
    valor_pago                DECIMAL(14,2),
    CONSTRAINT fk_titulo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX idx_titulo_usuario_id ON titulo(usuario_id);
CREATE INDEX idx_titulo_vencimento ON titulo(data_vencimento);