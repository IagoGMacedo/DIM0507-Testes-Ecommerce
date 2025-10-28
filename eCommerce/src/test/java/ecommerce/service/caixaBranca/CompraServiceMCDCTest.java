package ecommerce.service.caixaBranca;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import ecommerce.service.CarrinhoDeComprasService;
import ecommerce.service.ClienteService;
import ecommerce.service.CompraService;
import static ecommerce.service.DomainTestData.carrinhoComItem;
import static ecommerce.service.DomainTestData.produtoBasico;
import ecommerce.utils.Msg;

public class CompraServiceMCDCTest {

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
    @DisplayName("T1 - A=F, L=F, C=F (válidas) → não lança")
    void calcularCustoTotal_MCDC_T1_FFF_naoLanca() {

        Produto p = produtoBasico(); // A=F, L=F, C=F
        CarrinhoDeCompras carrinho = carrinhoComItem(p, 1L);

        var result = assertDoesNotThrow(
                () -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE));
        assertThat(result).as("Baseline sem violação de dimensões não deve lançar exceção").isNotNull();
    }

    @Test
    @DisplayName("T2 - A=T, L=F, C=F (altura <= 0) → lança dimensões inválidas")
    void calcularCustoTotal_MCDC_T2_TFF_lancaMsgDimensoes() {

        Produto p = produtoBasico();
        p.setAltura(BigDecimal.ZERO); // A = true
        CarrinhoDeCompras carrinho = carrinhoComItem(p, 1L);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE));

        assertThat(ex)
                .as("Altura <= 0 deve acionar a decisão de dimensões inválidas")
                .hasMessage(Msg.get("erro.produto.dimensoes.invalidas"));
    }

    @Test
    @DisplayName("T3 - A=F, L=T, C=F (largura <= 0) → lança dimensões inválidas")
    void calcularCustoTotal_MCDC_T3_FTF_lancaMsgDimensoes() {

        Produto p = produtoBasico();
        p.setLargura(BigDecimal.ZERO); // L = true
        CarrinhoDeCompras carrinho = carrinhoComItem(p, 1L);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE));

        assertThat(ex)
                .as("Largura <= 0 deve acionar a decisão de dimensões inválidas")
                .hasMessage(Msg.get("erro.produto.dimensoes.invalidas"));
    }

    @Test
    @DisplayName("T4 - A=F, L=F, C=T (comprimento <= 0) → lança dimensões inválidas")
    void calcularCustoTotal_MCDC_T4_FFT_lancaMsgDimensoes() {

        Produto p = produtoBasico();
        p.setComprimento(BigDecimal.ZERO); // C = true
        CarrinhoDeCompras carrinho = carrinhoComItem(p, 1L);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE));

        assertThat(ex)
                .as("Comprimento <= 0 deve acionar a decisão de dimensões inválidas")
                .hasMessage(Msg.get("erro.produto.dimensoes.invalidas"));
    }
}
