# Deploy — Cobrancas+ (Render + Vercel)

Implantação gratuita/hospedada: **Backend (Spring Boot + SQLite)** no [Render](https://render.com) e
**Frontend (Next.js)** na [Vercel](https://vercel.com).

> **Domínios diferentes (cross-site):** o frontend (`*.vercel.app`) e o backend (`*.onrender.com`) são
> sites distintos para o navegador. Isso exige o cookie de sessão com `SameSite=None; Secure`, senão o
> cookie **nunca** é enviado nas requisições da Vercel ao Render (todas as APIs autenticadas retornariam 401).
> As configurações abaixo já contemplam este cenário.

---

## PARTE 1 — Backend + Banco no Render

### 1.1 Criar o Web Service

1. No painel do Render: **New + > Web Service**.
2. Conecte o repositório (GitHub/GitLab) do projeto.
3. Na configuração:
   - **Environment**: `Docker`
   - **Root Directory**: `backend` *(o Dockerfile multi-stage fica em `backend/Dockerfile`)*
   - O Render detecta o Dockerfile e usa o `mvn clean package` interno; **não há** `mvnw` no projeto
     (por isso Docker, e não buildpack Java nativo).
4. **Create Web Service**.

> **Sem Docker**: seria necessário adicionar o Maven Wrapper (`./mvnw`) — os passos usam Docker por
> já existir e ser idempotente.

### 1.2 Disco persistente (SQLite)

Instâncias gratuitas do Render reiniciam o disco **efêmero** — o arquivo `emprestimos.db` seria perdido.
Adicione um disco persistente:

- **Disks → Add Disk**
  - **Name**: `sqlite-data`
  - **Mount Path**: `/app/data`
  - **Size**: 1 GB

O `Dockerfile` cria `/tmp` e `/app/data` dentro do container. Por padrão o SQLite usa
`jdbc:sqlite:/tmp/emprestimos.db` (efêmero); para persistir entre deploys aponte
`SPRING_DATASOURCE_URL` para o disco montado: `jdbc:sqlite:/app/data/emprestimos.db` (abaixo).

### 1.3 Variáveis de Ambiente (Environment Variables)

| Chave | Valor | Observação |
|---|---|---|
| `JWT_SECRET` | *(aleatória com ≥48 bytes / 384 bits)* | **Obrigatória** — sem ela o boot falha (fail-fast no `JwtService`) |
| `COOKIE_SECURE` | `true` | Obrigatório: Render serve HTTPS |
| `COOKIE_SAME_SITE` | `None` | **Essencial (cross-site)**: permite o cookie do `onrender.com` ser enviado a partir do `vercel.app` |
| `CORS_ALLOWED_ORIGINS` | `https://seu-app.vercel.app` | Origem real do frontend (sem barra final) |
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:/app/data/emprestimos.db` | Aponta para o disco persistente |
| `VALIDAR_MX` | `true` | Valida domínio MX no registro (default) |

> Gerar `JWT_SECRET` (PowerShell): `-join ((48..127) | Get-Random -Count 48 | % {[char]$_})`.

O Render injeta `PORT` automaticamente; o backend escuta em `server.port=${PORT:8080}`
(`application.properties`).

### 1.4 Migrações

O Flyway roda no boot (`spring.flyway.enabled=true`, `locations=classpath:db/migration`), criando/atualizando
o schema no disco persistente sem ação manual.

---

## PARTE 2 — Frontend na Vercel

1. **Add New + > Project** → importe o repositório.
2. **Root Directory**: `frontend` (Next.js detectado automaticamente; `next.config.ts` usa
   `output: "standalone"`, compatível).
3. **Environment Variables** (durante a criação ou em **Settings → Environment Variables**):

| Chave | Valor |
|---|---|
| `NEXT_PUBLIC_API_URL` | `https://seu-backend.onrender.com` |

> `NEXT_PUBLIC_*` é embutida **em build-time** no `src/lib/api.ts`; é consumida pelo navegador
> (`fetch` com `credentials: "include"`). Alterá-la exige novo deploy (variável de **build**).

---

## PARTE 3 — Ordem de deploy

1. Deploy do **backend** no Render **primeiro**.
2. Copie a URL gerada (`https://seu-backend.onrender.com`) para `NEXT_PUBLIC_API_URL` na Vercel.
3. Deploy do **frontend** na Vercel.

---

## PARTE 4 — Pós-deploy / Teste

- Frontend: `https://seu-app.vercel.app`
- API: `https://seu-backend.onrender.com/api/titulos` → **401** sem cookie (esperado).
- Fluxo completo: **Register** (e-mail com domínio real, ex. gmail — gera DNS MX lookup) → login manual →
  `Cookie` `SameSite=None; Secure` aparece no DevTools → `/me` → criar/editar/excluir títulos → logout.
- `curl` de fora do navegador: autentique com `Authorization: Bearer <token>` (o backend aceita os dois
  modos — cookie e Bearer).

## Sobre CORS e Segurança

- **CORS**: `CorsConfig` permite apenas a origem em `CORS_ALLOWED_ORIGINS`, com `allowCredentials(true)`
  (nunca `*` com credenciais) e `OPTIONS` liberado para preflight.
- **CSRF**: `CsrfOriginFilter` rejeita requests de origens não permitidas (e.g. script malicioso); por isso
  `CORS_ALLOWED_ORIGINS` precisa ser exatamente a URL do frontend.
- **Cookie**: `HttpOnly; Secure; SameSite=<config>`. Com `SameSite=None`, exige `Secure=true` (obrigatório
  no Render). A proteção contra CSRF não depende do SameSite — vem do `CsrfOriginFilter`.
- **JWT**: HS384 com chave ≥48 bytes via `JWT_SECRET`; expiração 24h; o cookie expira junto.

## Rollback / Atualização

- Render: novo push na branch do web service dispara novo build (`./Dockerfile` → `mvn package`).
  O disco `/app/data` persiste entre deploys.
- Vercel: novo push na branch de produção redeploya o frontend.