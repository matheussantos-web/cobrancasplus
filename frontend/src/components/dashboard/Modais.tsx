"use client";

import { useMemo, useState, type FormEvent } from "react";
import { AlertCircle, Loader2, Trash2 } from "lucide-react";

import { ApiError } from "@/lib/api";
import type { NovoTituloPayload, Titulo } from "@/types";
import { calcularJurosAtrasoPrevisao, calcularValorVencimento, diasEntre, hojeISO } from "@/utils/calculo";
import { formatarMoeda, formatarTaxa, formatarTaxaAtraso } from "@/utils/formatters";
import { Modal } from "../ui/Modal";

type ErrosForm = Record<string, string>;

interface NovoTituloModalProps {
  aberto: boolean;
  onFechar: () => void;
  aoSalvar: (payload: NovoTituloPayload, tituloId?: number) => Promise<void>;
  /** Quando informado, abre o modal em modo de edicao do titulo. */
  titulo?: Titulo | null;
}

export function NovoTituloModal({ aberto, onFechar, aoSalvar, titulo = null }: NovoTituloModalProps) {
  const [clienteNome, setClienteNome] = useState("");
  const [descricao, setDescricao] = useState("");
  const [valorOriginal, setValorOriginal] = useState("");
  const [taxaJurosFixa, setTaxaJurosFixa] = useState("30");
  const [taxaJurosAtrasoDiaria, setTaxaJurosAtrasoDiaria] = useState("2");
  const [dataVencimento, setDataVencimento] = useState(hojeISO());
  const [erros, setErros] = useState<ErrosForm>({});
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const modoEdicao = titulo !== null;

  // Ao reabrir com um titulo diferente (ou novo), reseta os campos.
  // Ajuste de estado durante a renderizacao: padrao oficial do React para
  // sincronizar estado derivado de props sem usar useEffect.
  const [previaTitulo, setPreviaTitulo] = useState(titulo);
  const [previaAberta, setPreviaAberta] = useState(aberto);
  if (aberto && (previaAberta !== aberto || previaTitulo !== titulo)) {
    setPreviaAberta(aberto);
    setPreviaTitulo(titulo);
    setErros({});
    setErroGeral(null);
    if (titulo) {
      setClienteNome(titulo.clienteNome);
      setDescricao(titulo.descricao ?? "");
      setValorOriginal(String(titulo.valorOriginal));
      setTaxaJurosFixa(String(titulo.taxaJurosFixa));
      setTaxaJurosAtrasoDiaria(String(titulo.taxaJurosAtrasoDiaria));
      setDataVencimento(titulo.dataVencimento);
    } else {
      setClienteNome("");
      setDescricao("");
      setValorOriginal("");
      setTaxaJurosFixa("30");
      setTaxaJurosAtrasoDiaria("2");
      setDataVencimento(hojeISO());
    }
  }

  const valor = Number.parseFloat(valorOriginal.replace(",", "."));
  const taxaFixa = Number.parseFloat(taxaJurosFixa.replace(",", "."));
  const taxaAtraso = Number.parseFloat(taxaJurosAtrasoDiaria.replace(",", "."));

  const previsao = useMemo(() => {
    const valorVencimento = calcularValorVencimento(valor, taxaFixa);
    const diasAtraso = dataVencimento ? Math.max(0, diasEntre(dataVencimento, hojeISO())) : 0;
    const acrescimo = calcularJurosAtrasoPrevisao(valorVencimento, taxaAtraso, diasAtraso);
    return {
      valorVencimento,
      diasAtraso,
      acrescimo,
      totalHoje: arredondar(valorVencimento + acrescimo),
    };
  }, [valor, taxaFixa, taxaAtraso, dataVencimento]);

  async function submeter(e: FormEvent) {
    e.preventDefault();
    setErroGeral(null);
    setErros({});

    const novosErros: ErrosForm = {};
    if (!clienteNome.trim()) novosErros.clienteNome = "Informe o nome do cliente";
    if (clienteNome.trim().length > 160) novosErros.clienteNome = "Maximo de 160 caracteres";
    if (isNaN(valor) || valor <= 0) novosErros.valorOriginal = "Informe o valor do emprestimo";
    if (isNaN(taxaFixa) || taxaFixa < 0) novosErros.taxaJurosFixa = "Taxa invalida";
    if (isNaN(taxaAtraso) || taxaAtraso < 0) novosErros.taxaJurosAtrasoDiaria = "Taxa invalida";
    if (!dataVencimento) novosErros.dataVencimento = "Informe a data de vencimento";
    if (Object.keys(novosErros).length > 0) {
      setErros(novosErros);
      return;
    }

    setEnviando(true);
    try {
      await aoSalvar(
        {
          clienteNome: clienteNome.trim(),
          descricao: descricao.trim() || undefined,
          valorOriginal: valor,
          dataVencimento,
          taxaJurosFixa: taxaFixa,
          taxaJurosAtrasoDiaria: taxaAtraso,
        },
        titulo?.id
      );
      onFechar();
    } catch (err) {
      if (err instanceof ApiError) {
        setErroGeral(err.message);
        setErros(err.erros ?? {});
      } else {
        setErroGeral("Nao foi possivel salvar o titulo.");
      }
    } finally {
      setEnviando(false);
    }
  }

  const previsaoValida = !isNaN(valor) && valor > 0 && !isNaN(taxaFixa) && taxaFixa >= 0;

  return (
    <Modal titulo={modoEdicao ? "Editar titulo" : "Novo titulo"} aberto={aberto} onFechar={onFechar}>
      <form onSubmit={submeter} className="space-y-4" noValidate>
        <div>
          <label className="mb-1.5 block text-sm font-medium text-foreground">Cliente</label>
          <input
            type="text"
            value={clienteNome}
            onChange={(e) => setClienteNome(e.target.value)}
            placeholder="Nome do cliente / devedor"
            className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
          {erros.clienteNome && <p className="mt-1 text-xs text-rose-400">{erros.clienteNome}</p>}
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-foreground">Descricao</label>
          <input
            type="text"
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            placeholder="Opcional — exemplo: Contrato 0123"
            className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-foreground">
            Valor do Emprestimo (R$)
          </label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            value={valorOriginal}
            onChange={(e) => setValorOriginal(e.target.value)}
            placeholder="1000,00"
            className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
          {erros.valorOriginal && (
            <p className="mt-1 text-xs text-rose-400">{erros.valorOriginal}</p>
          )}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-foreground">
              Juros do Contrato (%)
            </label>
            <input
              type="number"
              step="0.1"
              min="0"
              value={taxaJurosFixa}
              onChange={(e) => setTaxaJurosFixa(e.target.value)}
              placeholder="30"
              className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
            />
            {erros.taxaJurosFixa && (
              <p className="mt-1 text-xs text-rose-400">{erros.taxaJurosFixa}</p>
            )}
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-medium text-foreground">
              Juros de Atraso por dia (%)
            </label>
            <input
              type="number"
              step="0.1"
              min="0"
              value={taxaJurosAtrasoDiaria}
              onChange={(e) => setTaxaJurosAtrasoDiaria(e.target.value)}
              placeholder="2"
              className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
            />
            {erros.taxaJurosAtrasoDiaria && (
              <p className="mt-1 text-xs text-rose-400">{erros.taxaJurosAtrasoDiaria}</p>
            )}
          </div>
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-medium text-foreground">Data de vencimento</label>
          <input
            type="date"
            value={dataVencimento}
            onChange={(e) => setDataVencimento(e.target.value)}
            className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
          />
          {erros.dataVencimento && (
            <p className="mt-1 text-xs text-rose-400">{erros.dataVencimento}</p>
          )}
        </div>

        {previsaoValida && (
          <div className="rounded-xl border border-primary/20 bg-primary/5 px-4 py-3 space-y-1 text-xs">
            <p className="text-muted-foreground">
              Previa — o valor final e calculado pela API no momento da consulta:
            </p>
            <div className="flex justify-between">
              <span className="text-card-foreground">Valor do emprestimo</span>
              <span className="tabular-nums text-card-foreground">{formatarMoeda(valor)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">
                Juros do contrato ({formatarTaxa(taxaFixa)}):{" "}
                <span className="text-amber-300">+{formatarMoeda(previsao.valorVencimento - valor)}</span>
              </span>
              <span className="tabular-nums font-semibold text-foreground">
                Valor no vencimento: {formatarMoeda(previsao.valorVencimento)}
              </span>
            </div>
            {previsao.diasAtraso > 0 ? (
              <div className="flex justify-between border-t border-primary/20 pt-1">
                <span className="text-rose-300">
                  Acrescimo por atraso ({previsao.diasAtraso} dia(s) a {formatarTaxaAtraso(taxaAtraso)})
                </span>
                <span className="tabular-nums font-semibold text-rose-300">
                  +{formatarMoeda(previsao.acrescimo)}
                </span>
              </div>
            ) : (
              <p className="text-emerald-300">Sem atraso: o total sera o valor no vencimento.</p>
            )}
            <div className="flex justify-between border-t border-primary/20 pt-1">
              <span className="font-medium text-emerald-300">Total hoje</span>
              <span className="font-bold tabular-nums text-emerald-300">
                {formatarMoeda(previsao.totalHoje)}
              </span>
            </div>
          </div>
        )}

        {erroGeral && (
          <div className="flex items-start gap-2 rounded-xl border border-rose-500/30 bg-rose-950/50 px-4 py-3 text-sm text-rose-300">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            {erroGeral}
          </div>
        )}

        <div className="flex justify-end gap-3 pt-1">
          <button
            type="button"
            onClick={onFechar}
            className="rounded-xl border border-border bg-secondary px-4 py-2.5 text-sm font-medium text-secondary-foreground transition hover:bg-secondary/80"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={enviando}
            className="flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:opacity-60"
          >
            {enviando && <Loader2 className="h-4 w-4 animate-spin" />}
            {modoEdicao ? "Salvar alteracoes" : "Cadastrar titulo"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

interface BaixaModalProps {
  aberto: boolean;
  titulo: Titulo | null;
  onFechar: () => void;
  onConfirmar: (titulo: Titulo) => Promise<void>;
}

export function BaixaModal({ aberto, titulo, onFechar, onConfirmar }: BaixaModalProps) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  async function confirmar() {
    if (!titulo) return;
    setEnviando(true);
    setErro(null);
    try {
      await onConfirmar(titulo);
      onFechar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : "Nao foi possivel dar baixa.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal titulo="Confirmar baixa" aberto={aberto} onFechar={onFechar}>
      {titulo && (
        <div className="space-y-5">
          <div>
            <p className="text-sm font-semibold text-card-foreground">{titulo.clienteNome}</p>
            {titulo.descricao && <p className="text-xs text-muted-foreground">{titulo.descricao}</p>}
          </div>

          <div className="space-y-2 rounded-xl border border-border bg-secondary/40 p-4 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Valor do emprestimo</span>
              <span className="tabular-nums text-foreground">{formatarMoeda(titulo.valorOriginal)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">
                Valor do contrato ({formatarTaxa(titulo.taxaJurosFixa)})
              </span>
              <span className="tabular-nums font-semibold text-primary">
                {formatarMoeda(titulo.valorVencimento)}
              </span>
            </div>
            {titulo.diasAtraso > 0 ? (
              <div className="flex justify-between">
                <span className="text-muted-foreground">
                  Acrescimo por atraso ({titulo.diasAtraso} dia(s) a {formatarTaxaAtraso(titulo.taxaJurosAtrasoDiaria)})
                </span>
                <span className="tabular-nums font-semibold text-rose-400">
                  +{formatarMoeda(titulo.jurosAcumulados)}
                </span>
              </div>
            ) : (
              <p className="text-emerald-300">Sem atraso: nao ha juros de atraso.</p>
            )}
            <div className="border-t border-border pt-2">
              <div className="flex justify-between text-base">
                <span className="font-medium text-foreground">Total a receber</span>
                <span className="font-bold tabular-nums text-emerald-400">
                  {formatarMoeda(titulo.valorTotal)}
                </span>
              </div>
            </div>
          </div>

          {erro && (
            <div className="flex items-start gap-2 rounded-xl border border-rose-500/30 bg-rose-950/50 px-4 py-3 text-sm text-rose-300">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              {erro}
            </div>
          )}

          <div className="flex justify-end gap-3">
            <button
              onClick={onFechar}
              className="rounded-xl border border-border bg-secondary px-4 py-2.5 text-sm font-medium text-secondary-foreground transition hover:bg-secondary/80"
            >
              Cancelar
            </button>
            <button
              onClick={confirmar}
              disabled={enviando}
              className="flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:opacity-60"
            >
              {enviando && <Loader2 className="h-4 w-4 animate-spin" />}
              Confirmar pagamento
            </button>
          </div>
        </div>
      )}
    </Modal>
  );
}

interface ExcluirModalProps {
  aberto: boolean;
  titulo: Titulo | null;
  onFechar: () => void;
  onConfirmar: (titulo: Titulo) => Promise<void>;
}

export function ExcluirModal({ aberto, titulo, onFechar, onConfirmar }: ExcluirModalProps) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  async function confirmar() {
    if (!titulo) return;
    setEnviando(true);
    setErro(null);
    try {
      await onConfirmar(titulo);
      onFechar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : "Nao foi possivel excluir o titulo.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Modal titulo="Excluir titulo" aberto={aberto} onFechar={onFechar}>
      {titulo && (
        <div className="space-y-5">
          <div className="flex items-start gap-3 rounded-xl border border-rose-500/30 bg-rose-950/40 px-4 py-3">
            <Trash2 className="mt-0.5 h-5 w-5 shrink-0 text-rose-400" />
            <div className="text-sm">
              <p className="font-semibold text-rose-300">Excluir esta cobranca?</p>
              <p className="mt-1 text-xs text-rose-200/70">
                Esta acao nao pode ser desfeita. O titulo sera removido permanentemente.
              </p>
            </div>
          </div>

          <div>
            <p className="text-sm font-semibold text-card-foreground">{titulo.clienteNome}</p>
            {titulo.descricao && <p className="text-xs text-muted-foreground">{titulo.descricao}</p>}
            <div className="mt-2 flex items-center justify-between text-sm">
              <span className="text-muted-foreground">Valor total</span>
              <span className="font-bold tabular-nums text-card-foreground">
                {formatarMoeda(titulo.valorTotal)}
              </span>
            </div>
          </div>

          {erro && (
            <div className="flex items-start gap-2 rounded-xl border border-rose-500/30 bg-rose-950/50 px-4 py-3 text-sm text-rose-300">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              {erro}
            </div>
          )}

          <div className="flex justify-end gap-3">
            <button
              onClick={onFechar}
              className="rounded-xl border border-border bg-secondary px-4 py-2.5 text-sm font-medium text-secondary-foreground transition hover:bg-secondary/80"
            >
              Cancelar
            </button>
            <button
              onClick={confirmar}
              disabled={enviando}
              className="flex items-center gap-2 rounded-xl bg-destructive px-5 py-2.5 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-60"
            >
              {enviando && <Loader2 className="h-4 w-4 animate-spin" />}
              Excluir titulo
            </button>
          </div>
        </div>
      )}
    </Modal>
  );
}

function arredondar(valor: number): number {
  return Math.round(valor * 100) / 100;
}