package ecommerce.service;


import java.math.BigDecimal;
import java.util.List;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.TipoProduto;

public final class DomainTestData {

    private DomainTestData() {}

    public static CarrinhoDeCompras carrinhoVazio() {
        CarrinhoDeCompras c = new CarrinhoDeCompras();
        c.setItens(List.of());
        return c;
    }

    public static CarrinhoDeCompras carrinhoComItemValido(long qtd, BigDecimal preco) {
        Produto p = produtoBasico();
        p.setPreco(preco);
        return carrinhoComItem(p, qtd);
    }

    public static CarrinhoDeCompras carrinhoComItem(Produto p, long qtd) {
        ItemCompra item = new ItemCompra();
        item.setProduto(p);
        item.setQuantidade(qtd);
        CarrinhoDeCompras c = new CarrinhoDeCompras();
        c.setItens(List.of(item));
        return c;
    }

    public static CarrinhoDeCompras carrinhoComItemProdutoNulo(long qtd) {
        ItemCompra item = new ItemCompra();
        item.setProduto(null);
        item.setQuantidade(qtd);
        CarrinhoDeCompras c = new CarrinhoDeCompras();
        c.setItens(List.of(item));
        return c;
    }

    public static Produto produtoBasico() {
        Produto p = new Produto();
        p.setNome("Mouse");
        p.setDescricao("Mouse óptico");
        p.setTipo(TipoProduto.ELETRONICO);
        p.setPreco(new BigDecimal("10.00"));
        p.setPesoFisico(new BigDecimal("0.20"));
        p.setAltura(new BigDecimal("10"));
        p.setLargura(new BigDecimal("5"));
        p.setComprimento(new BigDecimal("5"));
        p.setFragil(false);
        return p;
    }

}
