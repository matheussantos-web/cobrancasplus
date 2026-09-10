"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Landmark, Loader2, LogIn, UserPlus } from "lucide-react";

import { useAuth } from "@/components/AuthProvider";
import { ApiError } from "@/lib/api";

type Modo = "entrar" | "registrar";

export default function EntrarPage() {
  const router = useRouter();
  const { entrar, registrar, usuario, carregando } = useAuth();

  const [modo, setModo] = useState<Modo>("entrar");
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [confirmarSenha, setConfirmarSenha] = useState("");
  const [chavePix, setChavePix] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);
  const [campoErros, setCampoErros] = useState<Record<string, string>>({});
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (usuario) router.replace("/dashboard");
  }, [usuario, router]);

  const aoSubmeter = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();
      setErro(null);
      setSucesso(null);

      if (modo === "registrar") {
        const errosLocais: Record<string, string> = {};
        if (senha.length < 6) {
          errosLocais.senha = "A senha deve ter no minimo 6 caracteres";
        }
        if (senha !== confirmarSenha) {
          errosLocais.confirmacao = "As senhas não coincidem";
        }
        if (Object.keys(errosLocais).length > 0) {
          setCampoErros(errosLocais);
          return;
        }
      }

      setCampoErros({});
      setEnviando(true);
      try {
        if (modo === "entrar") {
          await entrar(email, senha);
          router.push("/");
        } else {
          // Registro NAO autologa: mostra sucesso e prepara o login manual.
          await registrar(nome, email, senha, chavePix);
          setSucesso("Conta criada com sucesso! Faça login para continuar.");
          setModo("entrar");
          setEmail(email);
          setSenha("");
          setConfirmarSenha("");
          setChavePix("");
          setNome("");
          return;
        }
        router.refresh();
      } catch (err) {
        if (err instanceof ApiError) {
          setErro(err.message);
          setCampoErros(err.erros ?? {});
        } else {
          setErro("Nao foi possivel conectar ao servidor.");
        }
      } finally {
        setEnviando(false);
      }
    },
    [entrar, registrar, email, senha, confirmarSenha, chavePix, nome, modo, router]
  );

  if (carregando) {
    return (
      <main className="flex min-h-full flex-1 items-center justify-center bg-background text-foreground">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </main>
    );
  }

  return (
    <main className="flex min-h-full flex-1 items-center justify-center bg-background px-4 py-12 text-foreground">
      <div className="w-full max-w-md">
        <div className="mb-8 flex flex-col items-center gap-3 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary shadow-lg shadow-black/30">
            <Landmark className="h-7 w-7 text-primary-foreground" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Cobrancas+</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Controle de vencimentos e juros por atraso
            </p>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-6 text-card-foreground shadow-xl shadow-black/30">
          <div className="mb-6 grid grid-cols-2 gap-1 rounded-xl bg-secondary p-1">
            {(["entrar", "registrar"] as const).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => {
                  setModo(m);
                  setErro(null);
                  setSucesso(null);
                  setCampoErros({});
                  setConfirmarSenha("");
                  setChavePix("");
                }}
                className={`flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition ${
                  modo === m
                    ? "bg-primary text-primary-foreground shadow"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {m === "entrar" ? <LogIn className="h-4 w-4" /> : <UserPlus className="h-4 w-4" />}
                {m === "entrar" ? "Entrar" : "Criar conta"}
              </button>
            ))}
          </div>

          <form onSubmit={aoSubmeter} className="space-y-4" noValidate>
            {modo === "registrar" && (
              <div>
                <label htmlFor="nome" className="mb-1.5 block text-sm font-medium text-foreground">
                  Nome
                </label>
                <input
                  id="nome"
                  type="text"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Seu nome completo"
                  className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
                {campoErros.nome && (
                  <p className="mt-1 text-xs text-rose-400">{campoErros.nome}</p>
                )}
              </div>
            )}

            <div>
              <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-foreground">
                E-mail
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="voce@empresa.com"
                className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
              />
              {campoErros.email && <p className="mt-1 text-xs text-rose-400">{campoErros.email}</p>}
            </div>

            <div>
              <label htmlFor="senha" className="mb-1.5 block text-sm font-medium text-foreground">
                Senha
              </label>
              <input
                id="senha"
                type="password"
                autoComplete={modo === "entrar" ? "current-password" : "new-password"}
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                placeholder={modo === "registrar" ? "Minimo de 6 caracteres" : "Sua senha"}
                className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
              />
              {campoErros.senha && <p className="mt-1 text-xs text-rose-400">{campoErros.senha}</p>}
            </div>

            {modo === "registrar" && (
              <div>
                <label
                  htmlFor="confirmarSenha"
                  className="mb-1.5 block text-sm font-medium text-foreground"
                >
                  Confirmar Senha
                </label>
                <input
                  id="confirmarSenha"
                  type="password"
                  autoComplete="new-password"
                  value={confirmarSenha}
                  onChange={(e) => setConfirmarSenha(e.target.value)}
                  placeholder="Repita sua senha"
                  className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
                {campoErros.confirmacao && (
                  <p className="mt-1 text-xs text-rose-400">{campoErros.confirmacao}</p>
                )}
              </div>
            )}

            {modo === "registrar" && (
              <div>
                <label htmlFor="chavePix" className="mb-1.5 block text-sm font-medium text-foreground">
                  Chave Pix (opcional)
                </label>
                <input
                  id="chavePix"
                  type="text"
                  value={chavePix}
                  onChange={(e) => setChavePix(e.target.value)}
                  placeholder="CPF, e-mail, telefone ou chave aleatoria"
                  className="w-full rounded-xl border border-border bg-input px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
                {campoErros.chavePix && (
                  <p className="mt-1 text-xs text-rose-400">{campoErros.chavePix}</p>
                )}
              </div>
            )}

            {erro && (
              <div className="rounded-xl border border-rose-500/30 bg-rose-950/50 px-4 py-3 text-sm text-rose-300">
                {erro}
              </div>
            )}

            {sucesso && (
              <div className="rounded-xl border border-emerald-500/30 bg-emerald-950/50 px-4 py-3 text-sm text-emerald-300">
                {sucesso}
              </div>
            )}

            <button
              type="submit"
              disabled={enviando}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-black/30 transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {enviando && <Loader2 className="h-4 w-4 animate-spin" />}
              {modo === "entrar" ? "Acessar painel" : "Criar minha conta"}
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-xs text-muted-foreground">
          Desenvolvido em Java (Spring Boot) + Next.js.{" "}
          <Link href="/" className="text-primary hover:underline">
            Voltar
          </Link>
        </p>
      </div>
    </main>
  );
}