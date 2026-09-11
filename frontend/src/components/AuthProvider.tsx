"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { buscarSessao, cadastrar, encerrarSessao, logar } from "@/services/authService";
import { armazenarToken, removerToken } from "@/lib/api";
import type { UsuarioSessao } from "@/types";

interface AuthContextValue {
  usuario: UsuarioSessao | null;
  /** true enquanto a sessao carrega do backend (GET /api/auth/me). */
  carregando: boolean;
  entrar: (email: string, senha: string) => Promise<void>;
  registrar: (nome: string, email: string, senha: string, chavePix?: string) => Promise<void>;
  sair: () => Promise<void>;
}

const valorPadrao: AuthContextValue = {
  usuario: null,
  carregando: true,
  entrar: () => Promise.reject(new Error("AuthProvider ainda nao montado")),
  registrar: () => Promise.reject(new Error("AuthProvider ainda nao montado")),
  sair: () => Promise.resolve(),
};

const AuthContext = createContext<AuthContextValue>(valorPadrao);

/**
 * Sessao baseada em cookie httpOnly: no primeiro load pergunta ao backend
 * (GET /api/auth/me); o token nao fica acessivel via JavaScript (mitigacao XSS).
 * O registro cria a conta sem autologin: o usuario deve entrar manualmente.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioSessao | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    let ativo = true;
    async function checarSessao() {
      try {
        const sessao = await buscarSessao();
        if (ativo) setUsuario(sessao);
      } catch {
        if (ativo) setUsuario(null);
      } finally {
        if (ativo) setCarregando(false);
      }
    }
    checarSessao();
    return () => {
      ativo = false;
    };
  }, []);

  const entrar = useCallback(async (email: string, senha: string) => {
    const res = await logar(email, senha);
    armazenarToken(res.token);
    setUsuario({
      id: res.usuarioId,
      nome: res.nome,
      email: res.email,
      chavePix: res.chavePix ?? null,
    });
  }, []);

  const registrar = useCallback(async (nome: string, email: string, senha: string, chavePix?: string) => {
    await cadastrar(nome, email, senha, chavePix);
  }, []);

  const sair = useCallback(async () => {
    try {
      await encerrarSessao();
    } catch {
      // Cookie pode ja ter expirado; seguir para o estado deslogado.
    }
    removerToken();
    setUsuario(null);
  }, []);

  const valor = useMemo(
    () => ({ usuario, carregando, entrar, registrar, sair }),
    [usuario, carregando, entrar, registrar, sair]
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth deve ser usado dentro de <AuthProvider>");
  return ctx;
}