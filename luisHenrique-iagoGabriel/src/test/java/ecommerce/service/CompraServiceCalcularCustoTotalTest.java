package ecommerce.service;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

public class CompraServiceCalcularCustoTotalTest {

    private CompraService compraService;

    @BeforeEach
    void setUp() {
        compraService = new CompraService(null, null, null, null);
    }

    @Test
    void calcularCustoTotal_carrinhoVazio_deveLancarIllegalArgumentException() {
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoVazio();

        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(carrinho));
    }

    @Test
    void calcularCustoTotal_semDescontos_eSemFrete_retornaSubtotal() {
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItemValido(1, new BigDecimal("10.00"));

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        assertThat(total).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void calcularCustoTotal_desconto10porcento_aplicadoCorretamente() {
        // subtotal > 500 e <= 1000 => 10% de desconto
        Produto p = DomainTestData.produtoBasico(new BigDecimal("600.00"));
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // 600 - 10% = 540
        assertThat(total).isEqualByComparingTo(new BigDecimal("540.00"));
    }

    @Test
    void calcularCustoTotal_desconto20porcento_aplicadoCorretamente() {
        // subtotal > 1000 => 20% de desconto
        Produto p = DomainTestData.produtoBasico(new BigDecimal("1200.00"));
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // 1200 - 20% = 960
        assertThat(total).isEqualByComparingTo(new BigDecimal("960.00"));
    }

    @Test
    void calcularCustoTotal_encargoPeso_paraPesoEntre5e10() {
        Produto p = DomainTestData.produtoBasico();
        p.setPreco(new BigDecimal("0.01"));
        // ajustar peso para 6.0
        p.setPesoFisico(new BigDecimal("6.00"));
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // encargoPeso = peso * RATE_B + TAXA_MINIMA = 6*2 + 12 = 24
        // subtotal = 0.01 => total = 24.01
        assertThat(total).isEqualByComparingTo(new BigDecimal("24.01"));
    }

    @Test
    void calcularCustoTotal_encargoPeso_paraPesoMaiorQue50() {
        Produto p = DomainTestData.produtoBasico();
        p.setPreco(new BigDecimal("0.01"));
        p.setPesoFisico(new BigDecimal("51.00"));
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // encargoPeso = 51 * 7 + 12 = 369
        // subtotal = 0.01 => total = 369.01
        assertThat(total).isEqualByComparingTo(new BigDecimal("369.01"));
    }

    @Test
    void calcularCustoTotal_encargoFragilidade_porUnidadeFragil() {
        Produto p = DomainTestData.produtoBasico();
        p.setPreco(new BigDecimal("0.01"));
        p.setFragil(true);
        // quantidade 3
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 3);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // fragilidade = 3 * 5 = 15
        // subtotal = 0.03 => total = 15.03
        assertThat(total).isEqualByComparingTo(new BigDecimal("15.03"));
    }

    @Test
    void calcularCustoTotal_combinado_descontosPesoEFragilidade() {
        // combinar subtotal com desconto e frete (peso + fragilidade)
        Produto p1 = DomainTestData.produtoBasico(new BigDecimal("700.00"));
        p1.setPesoFisico(new BigDecimal("1.00"));
        p1.setFragil(false);

        Produto p2 = DomainTestData.produtoBasico(new BigDecimal("400.00"));
        p2.setPesoFisico(new BigDecimal("6.00"));
        p2.setFragil(true);

        ItemCompra it1 = new ItemCompra();
        it1.setProduto(p1);
        it1.setQuantidade(1L);

        ItemCompra it2 = new ItemCompra();
        it2.setProduto(p2);
        it2.setQuantidade(1L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(java.util.List.of(it1, it2));

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        /*
         subtotal = 700 + 400 = 1100 => desconto 20% (since >1000) => subtotalComDesconto = 880
         pesoTotal = 1 + 6 = 7 => encargoPeso = 7 * 2 + 12 = 26
         fragilidade = 1 unidade fragil * 5 = 5
         total = 880 + 26 + 5 = 911
         */
        assertThat(total).isEqualByComparingTo(new BigDecimal("911.00"));
    }

    @Test
    void calcularCustoTotal_pesoIgualA5_semFrete() {
        Produto p = DomainTestData.produtoBasico();
        p.setPreco(new BigDecimal("0.01"));
        p.setPesoFisico(new BigDecimal("5.00"));
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // peso <= 5 => frete zero; subtotal = 0.01
        assertThat(total).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    void calcularCustoTotal_pesoIgualA10_rateB() {
        Produto p = DomainTestData.produtoBasico();
        p.setPreco(new BigDecimal("0.01"));
        p.setPesoFisico(new BigDecimal("10.00"));
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // encargoPeso = 10 * 2 + 12 = 32; subtotal = 0.01 => total = 32.01
        assertThat(total).isEqualByComparingTo(new BigDecimal("32.01"));
    }

    @Test
    void calcularCustoTotal_pesoIgualA50_rateC() {
        Produto p = DomainTestData.produtoBasico();
        p.setPreco(new BigDecimal("0.01"));
        p.setPesoFisico(new BigDecimal("50.00"));
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // encargoPeso = 50 * 4 + 12 = 212; subtotal = 0.01 => total = 212.01
        assertThat(total).isEqualByComparingTo(new BigDecimal("212.01"));
    }

    @Test
    void calcularCustoTotal_carrinhoNulo_deveLancarIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(null));
    }

    @Test
    void calcularCustoTotal_carrinhoComItensNulos_deveLancarIllegalArgumentException() {
        CarrinhoDeCompras c = new CarrinhoDeCompras();
        c.setItens(null);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(c));
    }

    @Test
    void calcularCustoTotal_subtotalExato1000_aplica10porcento() {
        Produto p = DomainTestData.produtoBasico(new BigDecimal("1000.00"));
        CarrinhoDeCompras c = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(c);

        // 1000 -> não >1000, mas >500 => 10% desconto => 900
        assertThat(total).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    void calcularCustoTotal_subtotalExato500_semDesconto() {
        Produto p = DomainTestData.produtoBasico(new BigDecimal("500.00"));
        CarrinhoDeCompras c = DomainTestData.carrinhoComItem(p, 1);

        BigDecimal total = compraService.calcularCustoTotal(c);

        // 500 -> não >500 => sem desconto
        assertThat(total).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void calcularCustoTotal_multiplasQuantidades_eMesmoTipo() {
        // dois itens do mesmo tipo com quantidades >1
        Produto p1 = DomainTestData.produtoBasico(new BigDecimal("20.00"));
        p1.setPesoFisico(new BigDecimal("2.00"));
        p1.setFragil(false);

        Produto p2 = DomainTestData.produtoBasico(new BigDecimal("30.00"));
        p2.setPesoFisico(new BigDecimal("3.00"));
        p2.setFragil(true);

        ItemCompra it1 = new ItemCompra();
        it1.setProduto(p1);
        it1.setQuantidade(2L);

        ItemCompra it2 = new ItemCompra();
        it2.setProduto(p2);
        it2.setQuantidade(3L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(java.util.List.of(it1, it2));

        BigDecimal total = compraService.calcularCustoTotal(carrinho);

        // subtotal = 20*2 + 30*3 = 40 + 90 = 130 (sem desconto)
        // peso total = 2*2 + 3*3 = 4 + 9 = 13 => encargoPeso = 13 * RATE_C? 13 <=50 => RATE_C(4) -> 13*4 + 12 = 64
        // fragilidade = 3 unidades fragil * 5 = 15
        // total = 130 + 64 + 15 = 209
        assertThat(total).isEqualByComparingTo(new BigDecimal("209.00"));
    }

    @Test
    void calcularPesoFisicoItem_e_calcularPesoTotalCompra() {
        Produto p = DomainTestData.produtoBasico();
        p.setPesoFisico(new BigDecimal("2.50"));

        ItemCompra item = new ItemCompra();
        item.setProduto(p);
        item.setQuantidade(4L);

        BigDecimal pesoItem = compraService.calcularPesoFisicoItem(item);
        assertThat(pesoItem).isEqualByComparingTo(new BigDecimal("10.00"));

        CarrinhoDeCompras c = new CarrinhoDeCompras();
        c.setItens(java.util.List.of(item));

        BigDecimal pesoTotal = compraService.calcularPesoTotalCompra(c);
        assertThat(pesoTotal).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void calcularCustoTotal_produtoComPrecoNulo_deveLancarIllegalArgumentException() {
        Produto p = DomainTestData.produtoBasico();
        p.setPreco(null);

        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItem(p, 1);

        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(carrinho));
    }

    @Test
    void calcularCustoTotal_itemProdutoNulo_deveLancarIllegalArgumentException() {
        CarrinhoDeCompras carrinho = DomainTestData.carrinhoComItemProdutoNulo(1);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(carrinho));
    }

    @Test
    void calcularCustoTotal_itemQuantidadeZero_deveLancarIllegalArgumentException() {
        Produto p = DomainTestData.produtoBasico();
        ItemCompra it = new ItemCompra(); it.setProduto(p); it.setQuantidade(0L);
        CarrinhoDeCompras c = new CarrinhoDeCompras(); c.setItens(java.util.List.of(it));
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(c));
    }

    @Test
    void calcularCustoTotal_produtoDimensoesNulas_deveLancarIllegalArgumentException() {
        Produto p = DomainTestData.produtoBasico();
        p.setAltura(null);
        CarrinhoDeCompras c = DomainTestData.carrinhoComItem(p, 1);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(c));
    }

    @Test
    void calcularCustoTotal_produtoTipoNulo_deveLancarIllegalArgumentException() {
        Produto p = DomainTestData.produtoBasico();
        p.setTipo(null);
        CarrinhoDeCompras c = DomainTestData.carrinhoComItem(p, 1);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(c));
    }

    @Test
    void calcularCustoTotal_produtoNomeDescricaoBlank_deveLancarIllegalArgumentException() {
        Produto p = DomainTestData.produtoBasico();
        p.setNome("  ");
        p.setDescricao("");
        CarrinhoDeCompras c = DomainTestData.carrinhoComItem(p, 1);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(c));
    }

    @Test
    void calcularCustoTotal_produtoPrecoZero_deveLancarIllegalArgumentException() {
        Produto p = DomainTestData.produtoBasico();
        p.setPreco(BigDecimal.ZERO);
        CarrinhoDeCompras c = DomainTestData.carrinhoComItem(p, 1);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(c));
    }

    @Test
    void calcularCustoTotal_produtoPesoZero_deveLancarIllegalArgumentException() {
        Produto p = DomainTestData.produtoBasico();
        p.setPesoFisico(BigDecimal.ZERO);
        CarrinhoDeCompras c = DomainTestData.carrinhoComItem(p, 1);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(c));
    }

    @Test
    void calcularCustoTotal_produtoDescricaoNula_deveLancarIllegalArgumentException() {
        Produto p = DomainTestData.produtoBasico();
        p.setDescricao(null);
        CarrinhoDeCompras c = DomainTestData.carrinhoComItem(p, 1);
        assertThrows(IllegalArgumentException.class, () -> compraService.calcularCustoTotal(c));
    }
    @Test
    void finalizarCompra_disponibilidadeFalse_deveLancarEPassarIdsCorretos() {
        CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
        ClienteService clienteService = mock(ClienteService.class);
        IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
        IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

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

        ArgumentCaptor<java.util.List> captIds = ArgumentCaptor.forClass(java.util.List.class);
        verify(estoqueExternal).verificarDisponibilidade(captIds.capture(), anyList());
        assertThat(captIds.getValue()).isEqualTo(java.util.List.of(11L));
    }

    @Test
    void finalizarCompra_baixaFalha_deveCancelarPagamentoElançar() {
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
