"use client";

import { useState } from "react";
import { Check, CheckCircle2, Clock, Handshake, MessageCircle, Pencil, TrendingUp, Trash2 } from "lucide-react";

import type { Titulo } from "@/types";
import { formatarData, formatarMoeda, formatarTaxa, formatarTaxaAtraso } from "@/utils/formatters";

interface TituloCardProps {
  titulo: Titulo;
  emAtraso: boolean;
  chavePix: string | null;
  onBaixar: (titulo: Titulo) => void;
  onEditar: (titulo: Titulo) => void;
  onExcluir: (titulo: Titulo) => void;
}

export function TituloCard({ titulo, emAtraso, chavePix, onBaixar, onEditar, onExcluir }: TituloCardProps) {
  const [copiado, setCopiado] = useState(false);

  async function copiarCobranca() {
    const texto = montarCobranca(titulo, chavePix);
    try {
      await navigator.clipboard.writeText(texto);
    } catch {
      const area = document.createElement("textarea");
      area.value = texto;
      document.body.appendChild(area);
      area.select();
      document.execCommand("copy");
      document.body.removeChild(area);
    }
    setCopiado(true);
    setTimeout(() => setCopiado(false), 2000);
  }

  if (titulo.status === "PAGO") {
    return (
      <div className="rounded-xl border border-border bg-card p-4 text-card-foreground opacity-75">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-card-foreground">{titulo.clienteNome}</p>
            {titulo.descricao && (
              <p className="mt-0.5 truncate text-xs text-muted-foreground">{titulo.descricao}</p>
            )}
          </div>
          <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-400" />
        </div>
        <div className="mt-3 space-y-1 border-t border-border pt-3 text-xs">
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Valor do emprestimo</span>
            <span className="tabular-nums text-card-foreground line-through">
              {formatarMoeda(titulo.valorOriginal)}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Valor do contrato</span>
            <span className="tabular-nums text-card-foreground">
              {formatarMoeda(titulo.valorVencimento)}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-muted-foreground">Pago em {formatarData(titulo.dataPagamento)}</span>
            <span className="font-bold tabular-nums text-emerald-400">
              {formatarMoeda(titulo.valorPago ?? titulo.valorTotal)}
            </span>
          </div>
        </div>
        <div className="mt-3 flex justify-end border-t border-border pt-3">
          <button
            onClick={() => onExcluir(titulo)}
            className="flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium text-muted-foreground transition hover:bg-secondary/60 hover:text-destructive"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Excluir
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="group rounded-xl border border-border bg-card p-4 text-card-foreground transition hover:border-primary/50">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-card-foreground">{titulo.clienteNome}</p>
          {titulo.descricao && (
            <p className="mt-0.5 truncate text-xs text-muted-foreground">{titulo.descricao}</p>
          )}
        </div>
        <span
          className={`shrink-0 rounded-md px-2 py-0.5 text-[11px] font-medium ${
            emAtraso ? "bg-destructive/15 text-destructive" : "bg-secondary text-secondary-foreground"
          }`}
        >
          {emAtraso
            ? `${titulo.diasAtraso} dia${titulo.diasAtraso > 1 ? "s" : ""} de atraso`
            : formatarData(titulo.dataVencimento)}
        </span>
      </div>

      <div className="mt-3 space-y-1.5 border-t border-border pt-3 text-xs">
        <div className="flex items-center justify-between gap-2">
          <span className="text-muted-foreground">Valor do emprestimo</span>
          <span className="tabular-nums font-medium text-card-foreground">
            {formatarMoeda(titulo.valorOriginal)}
          </span>
        </div>
        <div className="flex items-center justify-between gap-2">
          <span className="text-muted-foreground">
            Valor do contrato ({formatarTaxa(titulo.taxaJurosFixa)})
          </span>
          <span className="tabular-nums font-semibold text-primary">
            {formatarMoeda(titulo.valorVencimento)}
          </span>
        </div>
        {titulo.diasAtraso > 0 || emAtraso ? (
          <div className="flex items-center justify-between gap-2">
            <span className="flex items-center gap-1.5 text-muted-foreground">
              <Clock className="h-3.5 w-3.5 text-destructive" />
              Acrescimo por atraso ({titulo.diasAtraso} dia{titulo.diasAtraso > 1 ? "s" : ""} a{" "}
              {formatarTaxaAtraso(titulo.taxaJurosAtrasoDiaria)})
            </span>
            <span className="tabular-nums font-semibold text-destructive">
              +{formatarMoeda(titulo.jurosAcumulados)}
            </span>
          </div>
        ) : (
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <Clock className="h-3.5 w-3.5 text-emerald-400" />
            Sem atraso — nao ha juros de atraso
          </div>
        )}
      </div>

      <div className="mt-3 flex items-center justify-between gap-2 border-t border-border pt-3">
        <div>
          <p className="flex items-center gap-1 text-[11px] text-muted-foreground">
            <TrendingUp className="h-3 w-3 text-amber-400" />
            Total atual
          </p>
          <p className="text-sm font-bold tabular-nums text-primary">
            {formatarMoeda(titulo.valorTotal)}
          </p>
        </div>
        <button
          onClick={() => onBaixar(titulo)}
          className="flex items-center gap-1.5 rounded-lg bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground shadow transition hover:bg-primary/90"
        >
          <Handshake className="h-3.5 w-3.5" />
          Dar baixa
        </button>
      </div>

      <div className="mt-3 flex items-center gap-2 border-t border-border pt-3">
        <button
          onClick={copiarCobranca}
          className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-medium transition ${
            copiado
              ? "bg-emerald-500/15 text-emerald-400"
              : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
          }`}
          title="Copia a mensagem de cobranca com a chave Pix"
        >
          {copiado ? <Check className="h-3.5 w-3.5" /> : <MessageCircle className="h-3.5 w-3.5" />}
          {copiado ? "Copiado!" : "WhatsApp"}
        </button>
        <button
          onClick={() => onEditar(titulo)}
          className="flex items-center justify-center gap-1.5 rounded-lg bg-secondary px-2.5 py-2 text-xs font-medium text-secondary-foreground transition hover:bg-secondary/80"
          title="Editar titulo"
        >
          <Pencil className="h-3.5 w-3.5" />
        </button>
        <button
          onClick={() => onExcluir(titulo)}
          className="flex items-center justify-center gap-1.5 rounded-lg bg-secondary px-2.5 py-2 text-xs font-medium text-secondary-foreground transition hover:bg-secondary/80 hover:text-destructive"
          title="Excluir titulo"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
}

function montarCobranca(titulo: Titulo, chavePix: string | null): string {
  const linhas: string[] = [];
  linhas.push(`*Cobranca — ${titulo.clienteNome}*`);
  if (titulo.descricao) linhas.push(titulo.descricao);
  linhas.push("");
  linhas.push(`*Emprestimo:* ${formatarMoeda(titulo.valorOriginal)}`);
  linhas.push(`*Vencimento:* ${formatarData(titulo.dataVencimento)}`);
  linhas.push(`*Juros do contrato:* ${formatarTaxa(titulo.taxaJurosFixa)}`);
  if (titulo.diasAtraso > 0) {
    linhas.push(
      `*Acrescimo:* ${titulo.diasAtraso} dia(s) a ${formatarTaxaAtraso(titulo.taxaJurosAtrasoDiaria)} (+${formatarMoeda(titulo.jurosAcumulados)})`
    );
  }
  linhas.push(`*Total atualizado:* ${formatarMoeda(titulo.valorTotal)}`);
  linhas.push("");
  linhas.push(`Pix: ${chavePix ?? "(_configure sua chave Pix_)"}`);
  return linhas.join("\n");
}