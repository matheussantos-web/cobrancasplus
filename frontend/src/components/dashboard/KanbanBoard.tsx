"use client";

import { CalendarClock, CalendarPlus, Handshake, TriangleAlert } from "lucide-react";

import type { Categoria, Titulo } from "@/types";
import { formatarMoeda } from "@/utils/formatters";
import { TituloCard } from "./TituloCard";

interface Coluna {
  categoria: Categoria;
  titulo: string;
  icone: React.ReactNode;
  corCabecalho: string;
  corBadge: string;
}

const COLUNAS: Coluna[] = [
  {
    categoria: "A_VENCER",
    titulo: "A Vencer",
    icone: <CalendarPlus className="h-4 w-4" />,
    corCabecalho: "text-sky-300",
    corBadge: "bg-sky-950/70 text-sky-300",
  },
  {
    categoria: "VENCE_HOJE",
    titulo: "Vencem Hoje",
    icone: <CalendarClock className="h-4 w-4" />,
    corCabecalho: "text-amber-300",
    corBadge: "bg-amber-950/70 text-amber-300",
  },
  {
    categoria: "EM_ATRASO",
    titulo: "Em Atraso",
    icone: <TriangleAlert className="h-4 w-4" />,
    corCabecalho: "text-rose-300",
    corBadge: "bg-rose-950/70 text-rose-300",
  },
  {
    categoria: "BAIXADO",
    titulo: "Baixados",
    icone: <Handshake className="h-4 w-4" />,
    corCabecalho: "text-emerald-300",
    corBadge: "bg-emerald-950/70 text-emerald-300",
  },
];

interface KanbanBoardProps {
  titulos: Titulo[];
  chavePix: string | null;
  onBaixar: (titulo: Titulo) => void;
  onEditar: (titulo: Titulo) => void;
  onExcluir: (titulo: Titulo) => void;
}

export function KanbanBoard({ titulos, chavePix, onBaixar, onEditar, onExcluir }: KanbanBoardProps) {
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
      {COLUNAS.map((coluna) => {
        const itens = titulos.filter((t) => t.categoria === coluna.categoria);
        const total = itens.reduce(
          (acc, t) =>
            acc +
            (t.status === "PAGO"
              ? t.valorPago ?? 0
              : t.valorPago ?? t.valorTotal),
          0
        );

        return (
          <section key={coluna.categoria} className="flex flex-col">
            <header className="mb-3 flex items-center justify-between px-1">
              <div className={`flex items-center gap-2 text-sm font-semibold ${coluna.corCabecalho}`}>
                {coluna.icone}
                {coluna.titulo}
              </div>
              <span
                className={`rounded-md px-2 py-0.5 text-xs font-medium tabular-nums ${coluna.corBadge}`}
              >
                {itens.length}
              </span>
            </header>

            <div className="flex flex-col gap-3 rounded-2xl border border-border bg-secondary/30 p-3 min-h-24">
              {itens.length === 0 ? (
                <p className="py-6 text-center text-xs text-muted-foreground">
                  Nenhum titulo {coluna.titulo.toLowerCase()}
                </p>
              ) : (
                itens.map((titulo) => (
                  <TituloCard
                    key={titulo.id}
                    titulo={titulo}
                    emAtraso={coluna.categoria === "EM_ATRASO"}
                    chavePix={chavePix}
                    onBaixar={onBaixar}
                    onEditar={onEditar}
                    onExcluir={onExcluir}
                  />
                ))
              )}
              {itens.length > 0 && (
                <div className="mt-auto border-t border-border px-1 pt-2 text-right text-[11px] text-muted-foreground">
                  Total: <span className="font-semibold text-foreground">{formatarMoeda(total)}</span>
                </div>
              )}
            </div>
          </section>
        );
      })}
    </div>
  );
}