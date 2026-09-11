const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const TOKEN_KEY = "emprestimos_token";

export function armazenarToken(token: string): void {
  if (typeof window !== "undefined") localStorage.setItem(TOKEN_KEY, token);
}

export function removerToken(): void {
  if (typeof window !== "undefined") localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
  readonly status: number;
  readonly erros?: Record<string, string>;

  constructor(mensagem: string, status: number, erros?: Record<string, string>) {
    super(mensagem);
    this.name = "ApiError";
    this.status = status;
    this.erros = erros;
  }
}

async function parseErro(res: Response): Promise<ApiError> {
  let mensagem = `Erro inesperado (${res.status})`;
  let erros: Record<string, string> | undefined;
  try {
    const corpo = (await res.json()) as { mensagem?: string; erros?: Record<string, string> };
    if (typeof corpo.mensagem === "string" && corpo.mensagem) mensagem = corpo.mensagem;
    erros = corpo.erros;
  } catch {
    // corpo nao e JSON
  }
  return new ApiError(mensagem, res.status, erros);
}

/**
 * Cliente HTTP unico. Injeta Authorization: Bearer com o token salvo em
 * localStorage; o backend tambem aceita o cookie de sessao para compatibilidade.
 */
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const { headers, ...resto } = options;

  const token = typeof window !== "undefined" ? localStorage.getItem(TOKEN_KEY) : null;

  const cabecalhos: Record<string, string> = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(headers as Record<string, string> | undefined),
  };

  const res = await fetch(`${API_URL}${path}`, {
    ...resto,
    headers: cabecalhos,
    credentials: "include",
  });

  if (!res.ok) {
    throw await parseErro(res);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}