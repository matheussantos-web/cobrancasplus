"use client";

import { Banknote, CalendarClock, Percent, TriangleAlert } from "lucide-react";

import type { DashboardResumo } from "@/types";
import { formatarMoeda } from "@/utils/formatters";

interface CardProps {
  titulo: string;
  valor: string;
  icone: React.ReactNode;
  corDestaque: string;
  rodape: string;
  negativo?: boolean;
}

function Cartao({ titulo, valor, icone, corDestaque, rodape, negativo }: CardProps) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 text-card-foreground shadow-lg shadow-black/30">
      <div className="flex items-start justify-between">
        <p className="text-sm text-muted-foreground">{titulo}</p>
        <div className={`rounded-xl p-2 ${corDestaque}`}>{icone}</div>
      </div>
      <p
        className={`mt-3 text-2xl font-bold tabular-nums ${
          negativo ? "text-destructive" : "text-card-foreground"
        }`}
      >
        {valor}
      </p>
      <p className="mt-1 text-xs text-muted-foreground">{rodape}</p>
    </div>
  );
}

export function SummaryCards({ resumo }: { resumo: DashboardResumo }) {
  const quantidadeBaixados = resumo.titulosPendentes;
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Cartao
        titulo="Total a receber"
        valor={formatarMoeda(resumo.totalAReceber)}
        icone={<Banknote className="h-5 w-5 text-emerald-300" />}
        corDestaque="bg-emerald-950/60"
        rodape={`${quantidadeBaixados} titulo(s) pendente(s)`}
      />
      <Cartao
        titulo="Juros acumulados"
        valor={formatarMoeda(resumo.jurosAcumulados)}
        icone={<Percent className="h-5 w-5 text-amber-300" />}
        corDestaque="bg-amber-950/60"
        rodape="Juros de contrato + atraso diario sobre titulos pendentes"
      />
      <Cartao
        titulo="Vencem hoje"
        valor={formatarMoeda(resumo.totalVencemHoje)}
        icone={<CalendarClock className="h-5 w-5 text-sky-300" />}
        corDestaque="bg-sky-950/60"
        rodape={`${resumo.titulosVencemHoje} titulo(s) com vencimento em ${new Date().toLocaleDateString("pt-BR", { day: "2-digit", month: "short" })}`}
      />
      <Cartao
        titulo="Em atraso"
        valor={formatarMoeda(resumo.totalEmAtraso)}
        icone={<TriangleAlert className="h-5 w-5 text-rose-300" />}
        corDestaque="bg-rose-950/60"
        rodape={`${resumo.titulosEmAtraso} titulo(s) gerando juros`}
        negativo={resumo.titulosEmAtraso > 0}
      />
    </div>
  );
}