-- Chave Pix do usuario para gerar a mensagem de cobranca do WhatsApp.
ALTER TABLE usuario ADD COLUMN chave_pix VARCHAR(100);