"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Loader2 } from "lucide-react";

import { useAuth } from "@/components/AuthProvider";

export default function HomePage() {
  const router = useRouter();
  const { usuario, carregando } = useAuth();

  useEffect(() => {
    if (carregando) return;
    router.replace(usuario ? "/dashboard" : "/entrar");
  }, [carregando, usuario, router]);

  if (carregando) {
    return (
      <main className="flex min-h-full flex-1 items-center justify-center bg-background text-foreground">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </main>
    );
  }

  return null;
}