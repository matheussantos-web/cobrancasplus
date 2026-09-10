/**
 * Helpers de data e pre-visualizacao de cadastro.
 *
 * IMPORTANTE: o back-end (CalculoFinanceiroService) e a fonte unica de verdade
 * para valores financeiros. Este arquivo nao recalcula juros para renderizacao;
 * apenas auxilia a pre-visualizacao "Valor no Vencimento" no formulario.
 */

export function hojeISO(): string {
  const hoje = new Date();
  const mes = String(hoje.getMonth() + 1).padStart(2, "0");
  const dia = String(hoje.getDate()).padStart(2, "0");
  return `${hoje.getFullYear()}-${mes}-${dia}`;
}

export function diasEntre(menorISO: string, maiorISO: string): number {
  const [aY, aM, aD] = menorISO.split("-").map(Number);
  const [bY, bM, bD] = maiorISO.split("-").map(Number);
  const inicio = new Date(aY, aM - 1, aD).getTime();
  const fim = new Date(bY, bM - 1, bD).getTime();
  return Math.round((fim - inicio) / 86_400_000);
}

function arredondar(valor: number): number {
  return Math.round(valor * 100) / 100;
}

/** valorVencimento = valorOriginal + (valorOriginal * taxaJurosFixa / 100). */
export function calcularValorVencimento(valorOriginal: number, taxaJurosFixa: number): number {
  if (isNaN(valorOriginal) || isNaN(taxaJurosFixa) || valorOriginal <= 0) return 0;
  return arredondar(valorOriginal + valorOriginal * (taxaJurosFixa / 100));
}

/** jurosAtraso = valorVencimento * (taxaAtraso / 100) * dias. */
export function calcularJurosAtrasoPrevisao(valorVencimento: number, taxaAtraso: number, dias: number): number {
  if (isNaN(valorVencimento) || isNaN(taxaAtraso) || dias <= 0) return 0;
  return arredondar(valorVencimento * (taxaAtraso / 100) * dias);
}