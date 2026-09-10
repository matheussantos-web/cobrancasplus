import { api } from "@/lib/api";
import type { AuthResponse, UsuarioSessao } from "@/types";

export async function logar(email: string, senha: string): Promise<AuthResponse> {
  return api<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, senha }),
  });
}

export async function cadastrar(
  nome: string,
  email: string,
  senha: string,
  chavePix?: string
): Promise<void> {
  await api<{ mensagem: string }>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ nome, email, senha, chavePix: chavePix?.trim() || undefined }),
  });
}

/** Valida a cookie de sessao (httpOnly) e devolve o usuario autenticado. */
export function buscarSessao(): Promise<UsuarioSessao> {
  return api<UsuarioSessao>("/api/auth/me");
}

/** Expira a cookie de sessao no servidor. */
export function encerrarSessao(): Promise<void> {
  return api<void>("/api/auth/logout", { method: "POST" });
}