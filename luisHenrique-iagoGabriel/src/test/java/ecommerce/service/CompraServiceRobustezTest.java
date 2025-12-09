package ecommerce.service;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Produto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import static ecommerce.service.DomainTestData.carrinhoComItem;
import static ecommerce.service.DomainTestData.carrinhoComItemProdutoNulo;
import static ecommerce.service.DomainTestData.carrinhoVazio;
import static ecommerce.service.DomainTestData.produtoBasico;
import ecommerce.utils.Msg;

public class CompraServiceRobustezTest {
        private CarrinhoDeComprasService carrinhoSvc;
        private ClienteService clienteSvc;
        private IEstoqueExternal estoque;
        private IPagamentoExternal pagamento;
        private CompraService service;

        @BeforeEach
        void setUp() {
                carrinhoSvc = Mockito.mock(CarrinhoDeComprasService.class);
                clienteSvc = Mockito.mock(ClienteService.class);
                estoque = Mockito.mock(IEstoqueExternal.class);
                pagamento = Mockito.mock(IPagamentoExternal.class);
                service = new CompraService(carrinhoSvc, clienteSvc, estoque, pagamento);
        }

        @Test
        void calcularCustoTotal_quandoCarrinhoInvalido_deveLancarIAE() {

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoVazio()));

                assertThat(ex)
                                .as("Carrinho vazio é inválido")
                                .hasMessage(Msg.get("erro.carrinho.invalido"));
        }

        @Test
        void calcularCustoTotal_quandoQuantidadeNaoPositiva_deveLancarIAE() {

                CarrinhoDeCompras carrinho = carrinhoComItem(produtoBasico(), 0L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Quantidade zero é inválida")
                                .hasMessage(Msg.get("erro.item.quantidade.invalida"));
        }

        @Test
        void calcularCustoTotal_quandoPrecoNegativo_deveLancarIAE() {

                Produto p = produtoBasico();
                p.setPreco(new BigDecimal("-1.00"));
                CarrinhoDeCompras carrinho = carrinhoComItem(p, 1L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Preço negativo é inválido")
                                .hasMessage(Msg.get("erro.produto.preco.invalido"));
        }

        @Test
        void calcularCustoTotal_quandoItemTemProdutoNulo_deveLancarIAE_mensagemCorreta() {
                var carrinho = carrinhoComItemProdutoNulo(1L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Item sem produto deve ser invalidado")
                                .hasMessage(Msg.get("erro.item.produto.invalido"));
        }

        @Test
        void calcularCustoTotal_quandoDimensoesZeradas_deveLancarIAE_mensagemCorreta() {
                var p = produtoBasico();
                p.setAltura(BigDecimal.ZERO); // aciona dimensões inválidas (<= 0)
                var carrinho = carrinhoComItem(p, 1L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Dimensões (altura/largura/comprimento) <= 0 devem ser invalidadas")
                                .hasMessage(Msg.get("erro.produto.dimensoes.invalidas"));
        }

        @Test
        void calcularCustoTotal_quandoPesoFisicoNaoPositivo_deveLancarIAE_mensagemCorreta() {
                var p = produtoBasico();
                p.setPesoFisico(BigDecimal.ZERO); // <= 0
                var carrinho = carrinhoComItem(p, 1L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Peso físico <= 0 deve ser invalidado")
                                .hasMessage(Msg.get("erro.produto.peso.invalido"));
        }

        @Test
        void calcularCustoTotal_quandoPrecoNaoPositivo_deveLancarIAE_mensagemCorreta() {
                var p = produtoBasico();
                p.setPreco(BigDecimal.ZERO);
                var carrinho = carrinhoComItem(p, 1L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Preço <= 0 deve ser invalidado")
                                .hasMessage(Msg.get("erro.produto.preco.invalido"));
        }

        @Test
        void calcularCustoTotal_quandoTipoProdutoNulo_deveLancarIAE_mensagemCorreta() {
                var p = produtoBasico();
                p.setTipo(null);
                var carrinho = carrinhoComItem(p, 1L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Tipo do produto nulo deve ser invalidado")
                                .hasMessage(Msg.get("erro.produto.tipo.invalido"));
        }

        @Test
        void calcularCustoTotal_quandoNomeVazio_deveLancarIAE_mensagemCorreta() {
                var p = produtoBasico();
                p.setNome("   "); // blank
                var carrinho = carrinhoComItem(p, 1L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Nome blank deve ser invalidado")
                                .hasMessage(Msg.get("erro.produto.nome.invalido"));
        }

        @Test
        void calcularCustoTotal_quandoDescricaoVazia_deveLancarIAE_mensagemCorreta() {
                var p = produtoBasico();
                p.setDescricao(""); // empty
                var carrinho = carrinhoComItem(p, 1L);

                var ex = assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho));

                assertThat(ex)
                                .as("Descrição vazia deve ser invalidada")
                                .hasMessage(Msg.get("erro.produto.descricao.invalida"));
        }

}
