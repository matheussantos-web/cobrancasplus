"use client";

import type { ReactNode } from "react";
import { X } from "lucide-react";

interface ModalProps {
  titulo: string;
  aberto: boolean;
  onFechar: () => void;
  children: ReactNode;
}

export function Modal({ titulo, aberto, onFechar, children }: ModalProps) {
  if (!aberto) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm"
      onClick={onFechar}
    >
      <div
        className="w-full max-w-lg overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-border px-6 py-4">
          <h2 className="text-base font-semibold text-card-foreground">{titulo}</h2>
          <button
            onClick={onFechar}
            className="rounded-lg p-1.5 text-muted-foreground transition hover:bg-secondary hover:text-secondary-foreground"
            aria-label="Fechar"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="max-h-[70vh] overflow-y-auto px-6 py-5">{children}</div>
      </div>
    </div>
  );
}