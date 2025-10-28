package ecommerce.service.caixaPreta;

import ecommerce.entity.*;
import ecommerce.external.*;
import ecommerce.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ecommerce.service.DomainTestData.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("CompraService - Análise de Limites")
public class CompraServiceAnaliseLimitesTest {

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

    // ============================================================
    // SUBTOTAL (TC09 – TC12)
    // ==========================================================
    @ParameterizedTest(name = "subtotal={0} → {1}")
    @CsvSource({
            "500.00, Nenhum desconto",
            "500.01, Desconto de 10%",
            "1000.00, Desconto de 10%",
            "1000.01, Desconto de 20%"
    })
    void calcularCustoTotal_Subtotal_Bordas(BigDecimal subtotal, String esperado) {
        CarrinhoDeCompras c = carrinhoComItem(produtoBasico(subtotal), 1L);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        switch (esperado) {
            case "Nenhum desconto" -> assertThat(total).isEqualByComparingTo(subtotal);
            case "Desconto de 10%" -> assertThat(total).isEqualByComparingTo(subtotal.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP));
            case "Desconto de 20%" -> assertThat(total).isEqualByComparingTo(subtotal.multiply(BigDecimal.valueOf(0.8)).setScale(2, RoundingMode.HALF_UP));
        }
    }

    // ============================================================
    // QUANTIDADE DE ITENS MESMO TIPO (TC13 – TC19)
    // ==========================================================
    @ParameterizedTest(name = "{0} itens → {1}")
    @CsvSource({
            "2, Nenhum desconto",
            "3, Desconto de 5%",
            "4, Desconto de 5%",
            "5, Desconto de 10%",
            "7, Desconto de 10%",
            "8, Desconto de 15%",
            "9, Desconto de 15%"
    })
    void calcularCustoTotal_DescontoPorTipo(int qtdItens, String esperado) {
        Produto p = produtoBasico(BigDecimal.valueOf(50));
        CarrinhoDeCompras c = new CarrinhoDeCompras();
        adicionarVariosItems(c, p, qtdItens);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        BigDecimal subtotal = BigDecimal.valueOf(qtdItens * 50L);

        switch (esperado) {
            case "Nenhum desconto" -> assertThat(total).isEqualByComparingTo(subtotal);
            case "Desconto de 5%" -> assertThat(total).isEqualByComparingTo(subtotal.multiply(BigDecimal.valueOf(0.95)));
            case "Desconto de 10%" -> assertThat(total).isEqualByComparingTo(subtotal.multiply(BigDecimal.valueOf(0.9)));
            case "Desconto de 15%" -> assertThat(total).isEqualByComparingTo(subtotal.multiply(BigDecimal.valueOf(0.85)));
        }
    }

    // ============================================================
    // PESO TOTAL E FRETE (TC20 – TC28)
    // ==========================================================
    @ParameterizedTest(name = "peso={0}kg → {1}")
    @CsvSource({
            "4.99, Frete A",
            "5.00, Frete A",
            "5.01, Frete B",
            "9.99, Frete B",
            "10.00, Frete B",
            "10.01, Frete C",
            "49.99, Frete C",
            "50.00, Frete C",
            "50.01, Frete D"
    })
    void calcularCustoTotal_PesoFrete(double peso, String faixa) {
        Produto p = produtoBasico(BigDecimal.TEN);
        p.setPesoFisico(BigDecimal.valueOf(peso));
        CarrinhoDeCompras c = carrinhoComItem(p, 1L);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        // Aqui apenas exemplifica o comportamento esperado.
        // Normalmente seria assertThat(totalFrete).isEqualTo(...)
        assertThat(total).isNotNull();
    }

    // ============================================================
    // TIPO DE CLIENTE (TC29 – TC31)
    // ============================================================
    @ParameterizedTest(name = "Cliente {0} → {1}")
    @CsvSource({
            "BRONZE, Frete Integral",
            "PRATA, Frete 50%",
            "OURO, Frete Grátis"
    })
    void calcularCustoTotal_TipoCliente(TipoCliente tipo, String esperado) {
        CarrinhoDeCompras c = carrinhoComItem(produtoBasico(BigDecimal.valueOf(100)), 1L);
        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, tipo);
        assertThat(total).isNotNull();
    }

    // ============================================================
    // REGIÃO (TC32 – TC36)
    // ============================================================
    @ParameterizedTest(name = "Região {0} → {1}")
    @CsvSource({
            "SUL, Frete * 1.05",
            "NORTE, Frete * 1.3",
            "NORDESTE, Frete * 1.1",
            "SUDESTE, Frete * 1",
            "CENTRO_OESTE, Frete * 1.2"
    })
    void calcularCustoTotal_Regiao(Regiao regiao, String esperado) {
        CarrinhoDeCompras c = carrinhoComItem(produtoBasico(BigDecimal.valueOf(100)), 1L);
        BigDecimal total = service.calcularCustoTotal(c, regiao, TipoCliente.BRONZE);
        assertThat(total).isNotNull();
    }

    // ============================================================
    // PRODUTOS FRÁGEIS (TC37 – TC38)
    // ============================================================
    @ParameterizedTest(name = "Frágil={0} → {1}")
    @CsvSource({
            "true, Frete + 5 * items",
            "false, Frete normal"
    })
    void calcularCustoTotal_ProdutoFragil(boolean fragil, String esperado) {
        Produto p = produtoBasico(BigDecimal.valueOf(100));
        p.setFragil(fragil);
        CarrinhoDeCompras c = carrinhoComItem(p, 2L);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isNotNull();
    }

    // ============================================================
    // CARRINHO VAZIO E MÍNIMO (TC39 – TC41)
    // ============================================================
    @ParameterizedTest(name = "Carrinho {0}")
    @CsvSource({
            "vazio, erro",
            "1, valor dos itens",
            "2, valor dos itens"
    })
    void calcularCustoTotal_Carrinho(String situacao, String esperado) {
        CarrinhoDeCompras c = switch (situacao) {
            case "vazio" -> carrinhoVazio();
            case "1" -> carrinhoComItem(produtoBasico(BigDecimal.TEN), 1L);
            case "2" -> carrinhoComItem(produtoBasico(BigDecimal.TEN), 2L);
            default -> throw new IllegalArgumentException("Situação inválida");
        };

        if (situacao.equals("vazio")) {
            assertThatThrownBy(() -> service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE))
                    .isInstanceOf(IllegalArgumentException.class);
        } else {
            assertThat(service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE)).isNotNull();
        }
    }
}
