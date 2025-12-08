package ecommerce.service;

import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class CompraServiceFinalizarCompraTest {

    @Test
    void finalizarCompra_disponibilidadeFalse_deveLancarEPassarIdsCorretos() {
        // Mocks
        CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
        ClienteService clienteService = mock(ClienteService.class);
        IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
        IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

        // Prepare domain objects
        Produto p = DomainTestData.produtoBasico();
        p.setId(7L);
        ItemCompra it = new ItemCompra(); it.setProduto(p); it.setQuantidade(2L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(); carrinho.setItens(java.util.List.of(it));

        Cliente cliente = new Cliente(); cliente.setId(1L);

        when(clienteService.buscarPorId(1L)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(2L, cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(false, java.util.List.of(p.getId())));

        CompraService svc = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);

        assertThrows(IllegalStateException.class, () -> svc.finalizarCompra(2L, 1L));

        ArgumentCaptor<java.util.List> captIds = ArgumentCaptor.forClass(java.util.List.class);
        ArgumentCaptor<java.util.List> captQty = ArgumentCaptor.forClass(java.util.List.class);
        verify(estoqueExternal).verificarDisponibilidade(captIds.capture(), captQty.capture());

        assertThat(captIds.getValue()).isEqualTo(java.util.List.of(7L));
        assertThat(captQty.getValue()).isEqualTo(java.util.List.of(2L));
    }

    @Test
    void finalizarCompra_pagamentoNaoAutorizado_deveLancarEPassarValorCorreto() {
        CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
        ClienteService clienteService = mock(ClienteService.class);
        IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
        IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

        Produto p = DomainTestData.produtoBasico(new BigDecimal("100.00"));
        p.setId(11L);
        ItemCompra it = new ItemCompra(); it.setProduto(p); it.setQuantidade(1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(); carrinho.setItens(java.util.List.of(it));

        Cliente cliente = new Cliente(); cliente.setId(5L);

        when(clienteService.buscarPorId(5L)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(10L, cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, java.util.List.of()));
        when(pagamentoExternal.autorizarPagamento(eq(5L), anyDouble()))
                .thenReturn(new PagamentoDTO(false, 0L));

        CompraService svc = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);

        assertThrows(IllegalStateException.class, () -> svc.finalizarCompra(10L, 5L));

        // ensure we passed correct ids (detect lambda mutation)
        ArgumentCaptor<java.util.List> captIds = ArgumentCaptor.forClass(java.util.List.class);
        verify(estoqueExternal).verificarDisponibilidade(captIds.capture(), anyList());
        assertThat(captIds.getValue()).isEqualTo(java.util.List.of(11L));
    }

    @Test
    void finalizarCompra_baixaFalha_deveCancelarPagamentoElancar() {
        CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
        ClienteService clienteService = mock(ClienteService.class);
        IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
        IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

        Produto p = DomainTestData.produtoBasico(new BigDecimal("5.00"));
        p.setId(21L);
        ItemCompra it = new ItemCompra(); it.setProduto(p); it.setQuantidade(2L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(); carrinho.setItens(java.util.List.of(it));

        Cliente cliente = new Cliente(); cliente.setId(9L);

        when(clienteService.buscarPorId(9L)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(3L, cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, java.util.List.of()));
        when(pagamentoExternal.autorizarPagamento(eq(9L), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 12345L));
        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(false));

        CompraService svc = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);

        assertThrows(IllegalStateException.class, () -> svc.finalizarCompra(3L, 9L));

        verify(pagamentoExternal).cancelarPagamento(9L, 12345L);
    }

    @Test
    void finalizarCompra_caminhoFeliz_deveRetornarCompraDTOSucesso() {
        CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
        ClienteService clienteService = mock(ClienteService.class);
        IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
        IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

        Produto p = DomainTestData.produtoBasico(new BigDecimal("10.00"));
        p.setId(31L);
        ItemCompra it = new ItemCompra(); it.setProduto(p); it.setQuantidade(1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(); carrinho.setItens(java.util.List.of(it));

        Cliente cliente = new Cliente(); cliente.setId(7L);

        when(clienteService.buscarPorId(7L)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(4L, cliente)).thenReturn(carrinho);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, java.util.List.of()));
        when(pagamentoExternal.autorizarPagamento(eq(7L), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 9876L));
        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(true));

        CompraService svc = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);

        var result = svc.finalizarCompra(4L, 7L);

        assertThat(result).isNotNull();
        assertThat(result.sucesso()).isTrue();
        assertThat(result.transacaoPagamentoId()).isEqualTo(9876L);
    }
}
