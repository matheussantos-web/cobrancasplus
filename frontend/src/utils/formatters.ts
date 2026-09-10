const moedaFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

export function formatarMoeda(valor: number): string {
  return moedaFormatter.format(valor);
}

const dataFormatter = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "short",
  year: "numeric",
});

export function formatarData(iso: string | null | undefined): string {
  if (!iso) return "—";
  const [ano, mes, dia] = iso.split("-").map(Number);
  return dataFormatter.format(new Date(ano, mes - 1, dia));
}

export function formatarTaxa(taxa: number): string {
  return `${taxa.toLocaleString("pt-BR")}%`;
}

export function formatarTaxaAtraso(taxa: number): string {
  return `${taxa.toLocaleString("pt-BR")}% a.d.`;
}

export function inicial(nome: string): string {
  return nome
    .trim()
    .split(/\s+/)
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}