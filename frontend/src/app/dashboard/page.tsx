"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarPlus, Loader2, LogOut, RefreshCw, Search } from "lucide-react";

import { useAuth } from "@/components/AuthProvider";
import { KanbanBoard } from "@/components/dashboard/KanbanBoard";
import { SummaryCards } from "@/components/dashboard/SummaryCards";
import { BaixaModal, ExcluirModal, NovoTituloModal } from "@/components/dashboard/Modais";
import { ApiError } from "@/lib/api";
import {
  atualizarTitulo,
  buscarResumo,
  criarNovoTitulo,
  efetuarBaixa,
  listarTitulos,
  removerTitulo,
} from "@/services/tituloService";
import type { DashboardResumo, NovoTituloPayload, Titulo } from "@/types";
import { inicial } from "@/utils/formatters";

export default function DashboardPage() {
  const router = useRouter();
  const { usuario, sair, carregando: carregandoSessao } = useAuth();

  const [titulos, setTitulos] = useState<Titulo[]>([]);
  const [resumo, setResumo] = useState<DashboardResumo | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const [novoAberto, setNovoAberto] = useState(false);
  const [baixaTitulo, setBaixaTitulo] = useState<Titulo | null>(null);
  const [editandoTitulo, setEditandoTitulo] = useState<Titulo | null>(null);
  const [excluirTitulo, setExcluirTitulo] = useState<Titulo | null>(null);
  const [busca, setBusca] = useState("");

  const carregar = useCallback(async () => {
    const [lista, resumoRes] = await Promise.all([listarTitulos(), buscarResumo()]);
    setTitulos(lista);
    setResumo(resumoRes);
  }, []);

  // Sessao ainda validando (GET /api/auth/me): tela limpa de carregamento.
  // Sem sessao: redireciona para /entrar sem exibir estado de erro.
  useEffect(() => {
    if (!carregandoSessao && !usuario) {
      router.replace("/entrar");
    }
  }, [carregandoSessao, usuario, router]);

  useEffect(() => {
    let ativo = true;
    async function iniciar() {
      if (!usuario) return;
      try {
        await carregar();
      } catch (err) {
        if (ativo) {
          if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
            sair();
            router.replace("/entrar");
          } else {
            setErro(err instanceof ApiError ? err.message : "Erro ao carregar dados.");
          }
        }
      } finally {
        if (ativo) setCarregando(false);
      }
    }
    iniciar();
    return () => {
      ativo = false;
    };
  }, [usuario, sair, router, carregar]);

  // Atualizacao periodica: os valores financeiros sao recalculados pelo
  // back-end (fonte unica de verdade) e re-buscados a cada 30 segundos.
  useEffect(() => {
    const id = setInterval(() => {
      carregar().catch(() => {});
    }, 30_000);
    return () => clearInterval(id);
  }, [carregar]);

  const criarTitulo = useCallback(
    async (payload: NovoTituloPayload) => {
      await criarNovoTitulo(payload);
      await carregar();
    },
    [carregar]
  );

  const editarTitulo = useCallback(
    async (payload: NovoTituloPayload, tituloId?: number) => {
      if (!tituloId) {
        await criarTitulo(payload);
        return;
      }
      await atualizarTitulo(tituloId, payload);
      setEditandoTitulo(null);
      await carregar();
    },
    [criarTitulo, carregar]
  );

  const confirmarExclusao = useCallback(
    async (titulo: Titulo) => {
      await removerTitulo(titulo.id);
      setExcluirTitulo(null);
      await carregar();
    },
    [carregar]
  );

  const darBaixa = useCallback(
    async (titulo: Titulo) => {
      await efetuarBaixa(titulo.id);
      setBaixaTitulo(null);
      await carregar();
    },
    [carregar]
  );

  // Filtro de busca por nome do cliente ou descricao (aceita palavras com/sem acento).
  const termo = normalizar(busca.trim());
  const titulosFiltrados = termo
    ? titulos.filter(
        (t) =>
          normalizar(t.clienteNome).includes(termo) ||
          (t.descricao ? normalizar(t.descricao).includes(termo) : false)
      )
    : titulos;

  const chavePix = usuario ? usuario.chavePix || usuario.email : null;

  if (carregandoSessao) {
    return (
      <main className="flex min-h-full flex-1 items-center justify-center bg-background text-foreground">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </main>
    );
  }

  if (!usuario) {
    return null;
  }

  if (carregando) {
    return (
      <main className="flex min-h-full flex-1 items-center justify-center bg-background text-foreground">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </main>
    );
  }

  return (
    <main className="min-h-full flex-1 bg-background text-foreground">
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <header className="flex flex-wrap items-center justify-between gap-4 pb-6">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-sm font-bold text-primary-foreground">
              {usuario ? inicial(usuario.nome) : "?"}
            </div>
            <div>
              <h1 className="text-lg font-bold text-foreground">Bem-vindo, {usuario?.nome.split(" ")[0]}</h1>
              <p className="text-xs text-muted-foreground">
                {new Date().toLocaleDateString("pt-BR", {
                  weekday: "long",
                  day: "2-digit",
                  month: "long",
                  year: "numeric",
                })}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setNovoAberto(true)}
              className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-lg shadow-black/30 transition hover:bg-primary/90"
            >
              <CalendarPlus className="h-4 w-4" />
              Novo titulo
            </button>
            <button
              onClick={() => { setErro(null); carregar().catch(() => {}) }}
              className="rounded-xl border border-border bg-secondary p-2.5 text-secondary-foreground transition hover:bg-secondary/80"
              aria-label="Atualizar dados"
            >
              <RefreshCw className="h-4 w-4" />
            </button>
            <button
              onClick={() => { sair(); router.replace("/entrar"); }}
              className="rounded-xl border border-border bg-secondary p-2.5 text-secondary-foreground transition hover:bg-secondary/80 hover:text-destructive"
              aria-label="Sair"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </header>

        {erro && (
          <div className="mb-6 rounded-xl border border-rose-500/30 bg-rose-950/50 px-4 py-3 text-sm text-rose-300">
            {erro}
          </div>
        )}

        <div className="relative mb-6">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="search"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Buscar por nome do cliente ou descricao..."
            aria-label="Buscar titulos"
            className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>

        {resumo && <SummaryCards resumo={resumo} />}

        <section className="mt-8">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <p className="text-xs text-muted-foreground">
              Valores calculados pela API (fonte unica de verdade) e atualizados a cada 30s —
              juros de atraso recalculados pelo back-end na data de hoje.
            </p>
          </div>

          <KanbanBoard
            titulos={titulosFiltrados}
            chavePix={chavePix}
            onBaixar={setBaixaTitulo}
            onEditar={setEditandoTitulo}
            onExcluir={setExcluirTitulo}
          />
        </section>
      </div>

      <NovoTituloModal
        aberto={novoAberto || editandoTitulo !== null}
        titulo={editandoTitulo}
        onFechar={() => {
          setNovoAberto(false);
          setEditandoTitulo(null);
        }}
        aoSalvar={editarTitulo}
      />
      <BaixaModal
        aberto={baixaTitulo !== null}
        titulo={baixaTitulo}
        onFechar={() => setBaixaTitulo(null)}
        onConfirmar={darBaixa}
      />
      <ExcluirModal
        aberto={excluirTitulo !== null}
        titulo={excluirTitulo}
        onFechar={() => setExcluirTitulo(null)}
        onConfirmar={confirmarExclusao}
      />
    </main>
  );
}

function normalizar(texto: string): string {
  return texto
    .toLocaleLowerCase("pt-BR")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}