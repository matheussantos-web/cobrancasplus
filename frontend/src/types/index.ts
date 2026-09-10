export type TituloStatus = "PENDENTE" | "PAGO";

export type Categoria = "A_VENCER" | "VENCE_HOJE" | "EM_ATRASO" | "BAIXADO";

export interface Titulo {
  id: number;
  clienteNome: string;
  descricao: string | null;
  valorOriginal: number;
  dataVencimento: string;
  taxaJurosFixa: number;
  taxaJurosAtrasoDiaria: number;
  valorVencimento: number;
  status: TituloStatus;
  dataPagamento: string | null;
  valorPago: number | null;
  diasAtraso: number;
  jurosAcumulados: number;
  valorTotal: number;
  categoria: Categoria;
}

export interface DashboardResumo {
  totalAReceber: number;
  titulosPendentes: number;
  jurosAcumulados: number;
  titulosVencemHoje: number;
  totalVencemHoje: number;
  titulosEmAtraso: number;
  totalEmAtraso: number;
  totalBaixado: number;
}

export interface AuthResponse {
  token: string;
  tipo: string;
  usuarioId: number;
  nome: string;
  email: string;
  chavePix: string | null;
}

export interface UsuarioSessao {
  id: number;
  nome: string;
  email: string;
  chavePix: string | null;
}

export interface NovoTituloPayload {
  clienteNome: string;
  descricao?: string;
  valorOriginal: number;
  dataVencimento: string;
  taxaJurosFixa: number;
  taxaJurosAtrasoDiaria: number;
}