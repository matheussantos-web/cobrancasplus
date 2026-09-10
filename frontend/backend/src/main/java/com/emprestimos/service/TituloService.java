package com.emprestimos.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emprestimos.dto.request.TituloRequest;
import com.emprestimos.dto.response.TituloResponse;
import com.emprestimos.entity.Titulo;
import com.emprestimos.entity.Usuario;
import com.emprestimos.enums.StatusTitulo;
import com.emprestimos.mapper.TituloMapper;
import com.emprestimos.repository.TituloRepository;
import com.emprestimos.repository.UsuarioRepository;

/**
 * Servico de tÃ­tulos. TODAS as operacoes fixam o usuarioId do token JWT
 * autenticado, garantindo o isolamento dos dados entre contas (multi-tenant).
 */
@Service
public class TituloService {

    private final TituloRepository tituloRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalculoFinanceiroService calculoService;
    private final TituloMapper tituloMapper;

    public TituloService(TituloRepository tituloRepository,
                         UsuarioRepository usuarioRepository,
                         CalculoFinanceiroService calculoService,
                         TituloMapper tituloMapper) {
        this.tituloRepository = tituloRepository;
        this.usuarioRepository = usuarioRepository;
        this.calculoService = calculoService;
        this.tituloMapper = tituloMapper;
    }

    @Transactional(readOnly = true)
    public List<TituloResponse> listar(Long usuarioId) {
        return tituloRepository.findByUsuarioIdOrderByDataVencimentoAsc(usuarioId)
                .stream()
                .map(tituloMapper::paraResposta)
                .toList();
    }

    @Transactional
    public TituloResponse criar(Long usuarioId, TituloRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario nao encontrado"));

        Titulo titulo = new Titulo(
                usuario,
                request.clienteNome().trim(),
                request.descricao() == null ? null : request.descricao().trim(),
                request.valorOriginal(),
                request.dataVencimento(),
                request.taxaJurosFixa(),
                request.taxaJurosAtrasoDiaria());

        Titulo salvo = tituloRepository.save(titulo);
        return tituloMapper.paraResposta(salvo);
    }

    /**
     * Baixa (marca como PAGO) apenas se o titulo pertence ao usuario autenticado.
     * O valor pago e o valor total calculado com os juros ate a data do pagamento.
     */
    @Transactional
    public TituloResponse darBaixa(Long usuarioId, Long tituloId) {
        Titulo titulo = buscarDoUsuario(usuarioId, tituloId);

        if (titulo.getStatus() == StatusTitulo.PAGO) {
            throw new IllegalStateException("Titulo ja foi baixado");
        }

        var dataPagamento = calculoService.dataAtual();
        var valorPago = calculoService.calcularValorBaixa(titulo, dataPagamento);
        titulo.darBaixa(dataPagamento, valorPago);

        return tituloMapper.paraResposta(tituloRepository.save(titulo));
    }

    /**
     * Edita cliente, descricao, valor, vencimento e taxas de um titulo
     * PENDENTE pertencente ao usuario autenticado. Titulos ja baixados (PAGO)
     * nao podem ser editados.
     */
    @Transactional
    public TituloResponse editar(Long usuarioId, Long tituloId, TituloRequest request) {
        Titulo titulo = buscarDoUsuario(usuarioId, tituloId);

        if (titulo.getStatus() == StatusTitulo.PAGO) {
            throw new IllegalStateException("Somente titulos pendentes podem ser editados");
        }

        titulo.setClienteNome(request.clienteNome().trim());
        titulo.setDescricao(request.descricao() == null ? null : request.descricao().trim());
        titulo.setValorOriginal(request.valorOriginal());
        titulo.setDataVencimento(request.dataVencimento());
        titulo.setTaxaJurosFixa(request.taxaJurosFixa());
        titulo.setTaxaJurosAtrasoDiaria(request.taxaJurosAtrasoDiaria());

        return tituloMapper.paraResposta(tituloRepository.save(titulo));
    }

    /**
     * Exclui o titulo do banco se pertencer ao usuario autenticado
     * (isolamento multi-tenant).
     */
    @Transactional
    public void excluir(Long usuarioId, Long tituloId) {
        Titulo titulo = buscarDoUsuario(usuarioId, tituloId);
        tituloRepository.delete(titulo);
    }

    private Titulo buscarDoUsuario(Long usuarioId, Long tituloId) {
        Titulo titulo = tituloRepository.findById(tituloId)
                .orElseThrow(() -> new NoSuchElementException("Titulo nao encontrado"));

        if (!titulo.getUsuario().getId().equals(usuarioId)) {
            throw new NoSuchElementException("Titulo nao encontrado");
        }
        return titulo;
    }
}