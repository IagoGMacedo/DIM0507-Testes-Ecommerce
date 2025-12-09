package ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ecommerce.dto.CompraDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.TipoProduto;
import ecommerce.external.fake.EstoqueSimulado;
import ecommerce.external.fake.PagamentoSimulado;

@ExtendWith(MockitoExtension.class)
public class CompraServiceFinalizarCompraCenario1Test {
    @Mock
    private ClienteService clienteService; // MOCK

    @Mock
    private CarrinhoDeComprasService carrinhoService; // MOCK

    private EstoqueSimulado estoqueFake; // FAKE externo
    private PagamentoSimulado pagamentoFake; // FAKE externo

    private CompraService compraService; // SUT (ajuste o nome se for outro)

    @BeforeEach
    void setUp() {
        estoqueFake = new EstoqueSimulado();
        pagamentoFake = new PagamentoSimulado();
        compraService = new CompraService(carrinhoService, clienteService, estoqueFake, pagamentoFake);
    }

    private CarrinhoDeCompras criarCarrinhoBasico() {
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
        carrinho.setItens(List.of(item));
        return carrinho;
    }

    @Test
    void deveFinalizarCompraComSucesso_quandoTudoOk() {
        Long clienteId = 1L;
        Long carrinhoId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        CarrinhoDeCompras carrinho = criarCarrinhoBasico();

        when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);

        estoqueFake.configurarDisponibilidade(true);
        estoqueFake.configurarBaixaSucesso(true);
        pagamentoFake.configurarAutorizacao(true);
        pagamentoFake.configurarProximaTransacao(123L);

        CompraDTO resultado = compraService.finalizarCompra(carrinhoId, clienteId);

        assertTrue(resultado.sucesso());
        assertEquals(123L, resultado.transacaoPagamentoId());
        assertEquals("Compra finalizada com sucesso.", resultado.mensagem());

        verify(clienteService).buscarPorId(clienteId);
        verify(carrinhoService).buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

        assertTrue(estoqueFake.isVerificarDisponibilidadeChamado());
        assertTrue(estoqueFake.isDarBaixaChamado());
        assertFalse(pagamentoFake.isCancelarChamado());
    }

    @Test
    void deveLancarExcecao_quandoItensForaDeEstoque() {
        Long clienteId = 1L;
        Long carrinhoId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        CarrinhoDeCompras carrinho = criarCarrinhoBasico();

        when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);

        estoqueFake.configurarDisponibilidade(false); // Fora de estoque

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> compraService.finalizarCompra(carrinhoId, clienteId));

        assertEquals("Itens fora de estoque.", ex.getMessage());

        verify(clienteService).buscarPorId(clienteId);
        verify(carrinhoService).buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

        assertTrue(estoqueFake.isVerificarDisponibilidadeChamado());
        assertFalse(estoqueFake.isDarBaixaChamado());
        assertFalse(pagamentoFake.isCancelarChamado());
    }

    @Test
    void deveLancarExcecao_quandoPagamentoNaoAutorizado() {
        Long clienteId = 1L;
        Long carrinhoId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        CarrinhoDeCompras carrinho = criarCarrinhoBasico();

        when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);

        estoqueFake.configurarDisponibilidade(true);
        pagamentoFake.configurarAutorizacao(false);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> compraService.finalizarCompra(carrinhoId, clienteId));

        assertEquals("Pagamento não autorizado.", ex.getMessage());

        verify(clienteService).buscarPorId(clienteId);
        verify(carrinhoService).buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

        assertTrue(estoqueFake.isVerificarDisponibilidadeChamado());
        assertFalse(estoqueFake.isDarBaixaChamado());
        assertFalse(pagamentoFake.isCancelarChamado());
    }

    @Test
    void deveCancelarPagamentoELancarExcecao_quandoFalhaNaBaixaDeEstoque() {
        Long clienteId = 1L;
        Long carrinhoId = 10L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        CarrinhoDeCompras carrinho = criarCarrinhoBasico();

        when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);

        estoqueFake.configurarDisponibilidade(true);
        pagamentoFake.configurarAutorizacao(true);
        pagamentoFake.configurarProximaTransacao(999L);
        estoqueFake.configurarBaixaSucesso(false); // falha na baixa

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> compraService.finalizarCompra(carrinhoId, clienteId));

        assertEquals("Erro ao dar baixa no estoque.", ex.getMessage());

        verify(clienteService).buscarPorId(clienteId);
        verify(carrinhoService).buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

        assertTrue(estoqueFake.isVerificarDisponibilidadeChamado());
        assertTrue(estoqueFake.isDarBaixaChamado());
        assertTrue(pagamentoFake.isCancelarChamado());
        assertEquals(clienteId, pagamentoFake.getUltimoClienteCancelamento());
        assertEquals(999L, pagamentoFake.getUltimaTransacaoCancelamento());
    }
}
