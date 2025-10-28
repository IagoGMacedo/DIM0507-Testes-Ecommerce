package ecommerce.service.caixaPreta;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import ecommerce.service.CarrinhoDeComprasService;
import ecommerce.service.ClienteService;
import ecommerce.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static ecommerce.service.DomainTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CompraServiceParticoes {
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
    public void calcularCustoTotal_TC01_carrinhoVazio() {
        CarrinhoDeCompras c = carrinhoVazio();

        assertThrows(IllegalArgumentException.class,
                () -> service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE),
                "Carrinho vazio deve lançar exceção");
    }
    @Test
    public void calcularCustoTotal_TC02_carrinhoSemDescontoIsento() {
        var produto = produtoBasico();
        CarrinhoDeCompras c = carrinhoComItem(produto, 1L);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total)
                .as("Subtotal < 500 e peso <= 5 → sem desconto e frete 0")
                .isEqualByComparingTo(produto.getPreco());
    }
    @Test
    public void calcularCustoTotal_TC03_carrinhoComDescontoPorTipoEFreteMinimo() {
        var produto = produtoBasico();
        CarrinhoDeCompras c = carrinhoVazio();
        adicionarVariosItems(c, produto, 4); //Carrinho com 4 itens de mesmo tipo, desconto de 5%

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        BigDecimal subtotal = produto.getPreco().multiply(BigDecimal.valueOf(4));
        BigDecimal esperado = subtotal.multiply(BigDecimal.valueOf(0.95));

        assertThat(total)
                .as("Desconto 5% por tipo aplicado e frete mínimo (isento até 5kg)")
                .isEqualByComparingTo(esperado);
    }
    @Test
    public void calcularCustoTotal_TC04_carrinhoComDescontoPorTipoEDescontoPorTotal() {
        var produto = produtoBasico();
        produto.setPreco(BigDecimal.valueOf(100));
        CarrinhoDeCompras c = carrinhoVazio();
        adicionarVariosItems(c, produto, 6); //Subtotal = 600 e 6 itens do mesmo tipo, desconto de 10% por tipo + 10% de desconto por total

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        BigDecimal esperado = BigDecimal.valueOf(600).multiply(BigDecimal.valueOf(0.9)).multiply(BigDecimal.valueOf(0.9));

        assertThat(total)
                .as("Descontos acumulados e multiplicador da região aplicados corretamente")
                .isCloseTo(esperado, within(BigDecimal.valueOf(0.01)));
    }
    @Test
    public void calcularCustoTotal_TC05_carrinhoComDescontoPorTipoEDescontoPorTotalEFrete() {
        var produto = produtoBasico();
        produto.setPreco(BigDecimal.valueOf(200));
        produto.setPesoFisico(BigDecimal.ONE);
        CarrinhoDeCompras c = carrinhoVazio();
        adicionarVariosItems(c, produto, 8); //Subtotal = 1600 e 8 itens do mesmo tipo e 8kg, desconto de 15% por tipo + 20% de desconto por total + frete

        BigDecimal total = service.calcularCustoTotal(c, Regiao.NORDESTE, TipoCliente.BRONZE);

        BigDecimal esperado = BigDecimal.valueOf(1600)
                .multiply(BigDecimal.valueOf(0.85))
                .multiply(BigDecimal.valueOf(0.8))
                .add(BigDecimal.valueOf(2 * 8 + 12).multiply(BigDecimal.valueOf(1.1))); // Frete = R$2 * 8KG + R$12 X taxa nordeste

        assertThat(total)
                .as("Descontos acumulados e multiplicador da região aplicados corretamente")
                .isCloseTo(esperado, within(BigDecimal.valueOf(0.01)));
    }
    @Test
    public void calcularCustoTotal_TC06_carrinhoComTaxaFragil() {
        var produto = produtoBasico();
        produto.setFragil(true);
        CarrinhoDeCompras c = carrinhoComItem(produto, 2L);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        BigDecimal subtotal = produto.getPreco().multiply(BigDecimal.valueOf(2));
        BigDecimal esperado = subtotal.add(BigDecimal.valueOf(10)); // 2x R$5 taxa
        assertThat(total)
                .as("Taxa frágil de 5,00 por item deve ser somada ao total")
                .isEqualByComparingTo(esperado);
    }
    @ParameterizedTest
    @CsvSource({
            "BRONZE, 1.00", // sem desconto
            "PRATA, 0.50",  // metade do frete
            "OURO, 0.00"    // frete gratuito
    })
    public void calcularCustoTotal_TC07_TC08_carrinhoComDescontoTipoCliente(TipoCliente tipo, double fatorFrete) {
        var produto = produtoBasico();
        produto.setPesoFisico(BigDecimal.TEN);
        CarrinhoDeCompras c = carrinhoComItem(produto, 1L);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, tipo);

        BigDecimal subtotal = produto.getPreco();
        BigDecimal freteBase = BigDecimal.valueOf(2 * 10).add(BigDecimal.valueOf(12)); // R$2/kg + taxa mínima
        BigDecimal esperado = subtotal.add(freteBase.multiply(BigDecimal.valueOf(fatorFrete)));

        assertThat(total)
                .as("Desconto de fidelidade no frete aplicado conforme o tipo de cliente")
                .isEqualByComparingTo(esperado);
    }
}
