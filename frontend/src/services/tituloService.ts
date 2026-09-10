import { api } from "@/lib/api";
import type { DashboardResumo, NovoTituloPayload, Titulo } from "@/types";

export function listarTitulos(): Promise<Titulo[]> {
  return api<Titulo[]>("/api/titulos");
}

export function buscarResumo(): Promise<DashboardResumo> {
  return api<DashboardResumo>("/api/dashboard/resumo");
}

export function criarNovoTitulo(payload: NovoTituloPayload): Promise<Titulo> {
  return api<Titulo>("/api/titulos", { method: "POST", body: JSON.stringify(payload) });
}

export function atualizarTitulo(id: number, payload: NovoTituloPayload): Promise<Titulo> {
  return api<Titulo>(`/api/titulos/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function removerTitulo(id: number): Promise<void> {
  return api<void>(`/api/titulos/${id}`, { method: "DELETE" });
}

export function efetuarBaixa(id: number): Promise<Titulo> {
  return api<Titulo>(`/api/titulos/${id}/baixa`, { method: "PATCH" });
}