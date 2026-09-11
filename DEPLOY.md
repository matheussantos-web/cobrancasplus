# Deploy — Cobrancas+ (Render + Vercel)

Implantação gratuita/hospedada: **Backend (Spring Boot + PostgreSQL)** no [Render](https://render.com) e
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

### 1.2 Banco de dados (PostgreSQL — Neon)

O backend usa **PostgreSQL em produção** (perfil `prod`), **não** disco/arquivo. Isso evita o disco
persistente, que **não é suportado em instâncias gratuitas do Render** ("Paid services can preserve local
filesystem changes by attaching a persistent disk, but Free web services cannot").

1. Crie uma base grátis no [Neon](https://neon.tech) (ou use um PostgreSQL gerido do Render).
2. Copie a **connection string** (`postgresql://...`) — ela já contém usuário/senha; o pooler `-pooler`
   recomendado para conexões JDBC.
3. Cole-a em `SPRING_DATASOURCE_URL` (Envs, abaixo). **Nunca** coloque a senha no código/git.

> **SQLite** permanece como banco de dev/testes local (perfil default + `application-test.properties`).
> Em produção o perfil `prod` troca o driver/dialeto e usa as migrações `db/migration-postgres`.

### 1.3 Variáveis de Ambiente (Environment Variables)

| Chave | Valor | Observação |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | **Obrigatória** — ativa PostgreSQL + `db/migration-postgres` |
| `SPRING_DATASOURCE_URL` | `postgresql://USUARIO:SENHA@host/db?sslmode=require` | Connection string do Neon (com credenciais) |
| `JWT_SECRET` | *(aleatória com ≥48 bytes / 384 bits)* | **Obrigatória** — sem ela o boot falha (fail-fast no `JwtService`) |
| `COOKIE_SECURE` | `true` | Obrigatório: Render serve HTTPS |
| `COOKIE_SAME_SITE` | `None` | **Essencial (cross-site)**: permite o cookie do `onrender.com` ser enviado a partir do `vercel.app` |
| `CORS_ALLOWED_ORIGINS` | `https://seu-app.vercel.app` | Origem real do frontend (sem barra final) |
| `VALIDAR_MX` | `true` | Valida domínio MX no registro (default) |

> Gerar `JWT_SECRET` (PowerShell): `-join ((48..127) | Get-Random -Count 48 | % {[char]$_})`.

O Render injeta `PORT` automaticamente; o backend escuta em `server.port=${PORT:8080}`
(`application.properties`).

> **⚠ Sobre o parâmetro `channel_binding`**: é nativo do `psql`, não do driver JDBC. Pode deixar na URL
> (o driver ignora parâmetros desconhecidos), mas é mais limpo usar apenas
> `?sslmode=require` no `SPRING_DATASOURCE_URL`.
>
> **Formato:** o backend aceita a connection string com **ou sem** o prefixo `jdbc:` — tanto
> `postgresql://USUARIO:SENHA@host/db` (como o Neon exibe) quanto `jdbc:postgresql://...` funcionam.

### 1.4 Migrações

O Flyway roda no boot, criando/atualizando o schema **sem ação manual**:

- **Produção** (`prod`): `locations=classpath:db/migration-postgres` (sintaxe PostgreSQL); a base Neon
  começa vazia e recebe `V1`/`V2` no primeiro deploy.
- **Dev/testes** (default com SQLite): `locations=classpath:db/migration` (sintaxe SQLite).

> Se a base Neon não estiver vazia, o Flyway exige o schema igual (mesma cronologia de migrações).

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
  Os dados ficam no PostgreSQL (Neon) — sobrevivem a restarts/deploys, independente do disco do container.
- Vercel: novo push na branch de produção redeploya o frontend.