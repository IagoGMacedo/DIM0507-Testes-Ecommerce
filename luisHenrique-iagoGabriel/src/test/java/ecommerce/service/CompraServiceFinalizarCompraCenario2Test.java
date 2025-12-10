package ecommerce.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.TipoProduto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import ecommerce.external.fake.CarrinhoDeComprasRepositoryFake;
import ecommerce.external.fake.ClienteRepositoryFake;

@ExtendWith(MockitoExtension.class)
public class CompraServiceFinalizarCompraCenario2Test {
    @Mock
    private IEstoqueExternal estoqueExternal;

    @Mock
    private IPagamentoExternal pagamentoExternal;

    private ClienteRepositoryFake clienteRepositoryFake;
    private CarrinhoDeComprasRepositoryFake carrinhoRepositoryFake;

    private ClienteService clienteService;
    private CarrinhoDeComprasService carrinhoService;

    private CompraService compraService;

    @BeforeEach
    void setUp() {
        clienteRepositoryFake = new ClienteRepositoryFake();
        carrinhoRepositoryFake = new CarrinhoDeComprasRepositoryFake();

        clienteService = new ClienteService(clienteRepositoryFake);
        carrinhoService = new CarrinhoDeComprasService(carrinhoRepositoryFake);

        compraService = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);
    }

    private CarrinhoDeCompras criarCarrinhoBasico(Long carrinhoId, Cliente cliente) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Mouse");
        produto.setPreco(new BigDecimal("50.00"));
        produto.setDescricao("Mouse óptico");
        produto.setTipo(TipoProduto.ELETRONICO);
        produto.setPesoFisico(new BigDecimal("0.20"));
        produto.setAltura(new BigDecimal("10"));
        produto.setLargura(new BigDecimal("5"));
        produto.setComprimento(new BigDecimal("5"));
        produto.setFragil(false);

        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(2L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setId(carrinhoId);
        carrinho.setCliente(cliente);
        carrinho.setItens(List.of(item));
        return carrinho;
    }

    @Test
    void deveFinalizarCompraComSucesso_quandoTudoOk() {
        Long clienteId = 1L;
        Long carrinhoId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        clienteRepositoryFake.salvar(cliente);

        CarrinhoDeCompras carrinho = criarCarrinhoBasico(carrinhoId, cliente);
        carrinhoRepositoryFake.salvar(carrinho);

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));

        when(pagamentoExternal.autorizarPagamento(eq(clienteId), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 123L));

        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(true));

        CompraDTO resultado = compraService.finalizarCompra(carrinhoId, clienteId);

        assertTrue(resultado.sucesso());
        assertEquals(123L, resultado.transacaoPagamentoId());
        assertEquals("Compra finalizada com sucesso.", resultado.mensagem());

        verify(estoqueExternal).verificarDisponibilidade(anyList(), anyList());
        verify(pagamentoExternal).autorizarPagamento(eq(clienteId), anyDouble());
        verify(estoqueExternal).darBaixa(anyList(), anyList());
        verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
    }

    @Test
    void deveLancarExcecao_quandoItensForaDeEstoque() {
        Long clienteId = 1L;
        Long carrinhoId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        clienteRepositoryFake.salvar(cliente);

        CarrinhoDeCompras carrinho = criarCarrinhoBasico(carrinhoId, cliente);
        carrinhoRepositoryFake.salvar(carrinho);

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(false, List.of(1L)));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> compraService.finalizarCompra(carrinhoId, clienteId));

        assertEquals("Itens fora de estoque.", ex.getMessage());

        verify(estoqueExternal).verificarDisponibilidade(anyList(), anyList());
        verify(pagamentoExternal, never()).autorizarPagamento(anyLong(), anyDouble());
        verify(estoqueExternal, never()).darBaixa(anyList(), anyList());
        verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
    }

    @Test
    void deveLancarExcecao_quandoPagamentoNaoAutorizado() {
        Long clienteId = 1L;
        Long carrinhoId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        clienteRepositoryFake.salvar(cliente);

        CarrinhoDeCompras carrinho = criarCarrinhoBasico(carrinhoId, cliente);
        carrinhoRepositoryFake.salvar(carrinho);

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));

        when(pagamentoExternal.autorizarPagamento(eq(clienteId), anyDouble()))
                .thenReturn(new PagamentoDTO(false, null));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> compraService.finalizarCompra(carrinhoId, clienteId));

        assertEquals("Pagamento não autorizado.", ex.getMessage());

        verify(estoqueExternal).verificarDisponibilidade(anyList(), anyList());
        verify(pagamentoExternal).autorizarPagamento(eq(clienteId), anyDouble());
        verify(estoqueExternal, never()).darBaixa(anyList(), anyList());
        verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
    }

    @Test
    void deveCancelarPagamentoELancarExcecao_quandoFalhaNaBaixaDeEstoque() {
        Long clienteId = 1L;
        Long carrinhoId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        clienteRepositoryFake.salvar(cliente);

        CarrinhoDeCompras carrinho = criarCarrinhoBasico(carrinhoId, cliente);
        carrinhoRepositoryFake.salvar(carrinho);

        Long transacaoId = 999L;

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));

        when(pagamentoExternal.autorizarPagamento(eq(clienteId), anyDouble()))
                .thenReturn(new PagamentoDTO(true, transacaoId));

        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(false));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> compraService.finalizarCompra(carrinhoId, clienteId));

        assertEquals("Erro ao dar baixa no estoque.", ex.getMessage());

        verify(estoqueExternal).verificarDisponibilidade(anyList(), anyList());
        verify(pagamentoExternal).autorizarPagamento(eq(clienteId), anyDouble());
        verify(estoqueExternal).darBaixa(anyList(), anyList());
        verify(pagamentoExternal).cancelarPagamento(clienteId, transacaoId);
    }
}
