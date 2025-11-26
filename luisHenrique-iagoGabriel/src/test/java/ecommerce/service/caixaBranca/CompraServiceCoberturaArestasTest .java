package ecommerce.service.caixaBranca;

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

@DisplayName("CompraService - Cobertura de Arestas (Completa)")
class CompraServiceCoberturaArestasTest {

    private static final BigDecimal DESCONTO_05 = new BigDecimal("0.05");
    private static final BigDecimal DESCONTO_10 = new BigDecimal("0.10");
    private static final BigDecimal DESCONTO_15 = new BigDecimal("0.15");
    private static final BigDecimal DESCONTO_20 = new BigDecimal("0.20");
    private static final BigDecimal TAXA_MINIMA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_FRAGIL = new BigDecimal("5.00");

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
    @DisplayName("Frete faixa A → peso <= 5 → frete isento")
    void calcularCustoTotal_quandoPesoAte5kg_entaoFreteIsento() {
        var p = produtoBasico();
        p.setPesoFisico(new BigDecimal("4.99"));
        var c = carrinhoComItem(p, 1L);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).isEqualByComparingTo(p.getPreco());
    }

    @Test
    @DisplayName("Frete faixa B → 5 < peso <= 10 → R$2/kg + taxa mínima")
    void calcularCustoTotal_quandoPesoEntre5e10_entaoFreteFaixaB() {
        var p = produtoBasico();
        p.setPesoFisico(new BigDecimal("8"));
        var c = carrinhoComItem(p, 1L);

        BigDecimal esperado = p.getPreco()
                .add(new BigDecimal("8").multiply(new BigDecimal("2.00")).add(TAXA_MINIMA))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo(esperado);
    }

    @Test
    @DisplayName("Frete faixa C → 10 < peso <= 50 → R$4/kg + taxa mínima")
    void calcularCustoTotal_quandoPesoEntre10e50_entaoFreteFaixaC() {
        var p = produtoBasico();
        p.setPesoFisico(new BigDecimal("20"));
        var c = carrinhoComItem(p, 1L);

        BigDecimal esperado = p.getPreco()
                .add(new BigDecimal("20").multiply(new BigDecimal("4.00")).add(TAXA_MINIMA))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo(esperado);
    }

    @Test
    @DisplayName("Frete faixa D → peso > 50 → R$7/kg + taxa mínima")
    void calcularCustoTotal_quandoPesoMaiorQue50_entaoFreteFaixaD() {
        var p = produtoBasico();
        p.setPesoFisico(new BigDecimal("60"));
        var c = carrinhoComItem(p, 1L);

        BigDecimal esperado = p.getPreco()
                .add(new BigDecimal("60").multiply(new BigDecimal("7.00")).add(TAXA_MINIMA))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo(esperado);
    }

    @ParameterizedTest(name = "Região {0} → multiplicador aplicado")
    @CsvSource({
            "NORTE, 1.3",
            "NORDESTE, 1.1",
            "SUL, 1.05",
            "SUDESTE, 1.0",
            "CENTRO_OESTE, 1.2"
    })
    void calcularCustoTotal_quandoRegiaoVariada_entaoMultiplicadorAplicado(Regiao regiao, double fator) {
        var p = produtoBasico();
        p.setPesoFisico(new BigDecimal("10"));
        var c = carrinhoComItem(p, 1L);

        BigDecimal freteBase = new BigDecimal("10").multiply(new BigDecimal("2.00")).add(TAXA_MINIMA);
        BigDecimal esperado = p.getPreco().add(freteBase.multiply(BigDecimal.valueOf(fator)));

        BigDecimal total = service.calcularCustoTotal(c, regiao, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo(esperado.setScale(2, RoundingMode.HALF_UP));
    }

    @ParameterizedTest(name = "Subtotal={0} → desconto esperado={1}")
    @CsvSource({
            "400.00, 1.00", // sem desconto
            "600.00, 0.9", // 10%
            "1500.00, 0.8" // 20%
    })
    void calcularCustoTotal_quandoSubtotalNasFaixas_entaoDescontoCorreto(BigDecimal subtotal, double fatorEsperado) {
        var p = produtoBasico(subtotal);
        var c = carrinhoComItem(p, 1L);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        BigDecimal esperado = subtotal.multiply(BigDecimal.valueOf(fatorEsperado));
        assertThat(total).isEqualByComparingTo(esperado.setScale(2, RoundingMode.HALF_UP));
    }

    @ParameterizedTest(name = "{0} → fator desconto frete {1}")
    @CsvSource({
            "BRONZE, 1.00",
            "PRATA, 0.50",
            "OURO, 0.00"
    })
    void calcularCustoTotal_quandoTipoClienteVariado_entaoDescontoFreteAplicado(TipoCliente tipo, double fator) {
        var p = produtoBasico();
        p.setPesoFisico(BigDecimal.TEN);
        var c = carrinhoComItem(p, 1L);

        BigDecimal freteBase = BigDecimal.TEN.multiply(new BigDecimal("2")).add(TAXA_MINIMA);
        BigDecimal esperado = p.getPreco().add(freteBase.multiply(BigDecimal.valueOf(fator)));

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, tipo);
        assertThat(total).isEqualByComparingTo(esperado.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Produto frágil → adiciona taxa de R$5 por unidade")
    void calcularCustoTotal_quandoProdutoFragil_entaoSomaTaxaFragilidade() {
        var p = produtoBasico();
        p.setFragil(true);
        var c = carrinhoComItem(p, 2L);

        BigDecimal esperado = p.getPreco().multiply(BigDecimal.valueOf(2))
                .add(TAXA_FRAGIL.multiply(BigDecimal.valueOf(2)));
        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).isEqualByComparingTo(esperado.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Cenário completo: subtotal alto, tipo OURO, região NORDESTE, produto frágil")
    void calcularCustoTotal_cenarioCompleto_entaoAplicaTodosFatores() {
        var p = produtoBasico(BigDecimal.valueOf(200));
        p.setFragil(true);
        p.setPesoFisico(new BigDecimal("8"));
        var c = carrinhoVazio();
        adicionarVariosItems(c, p, 8);

        // Subtotal = 1600 → 20% desconto subtotal, 15% desconto tipo produto
        BigDecimal subtotal = BigDecimal.valueOf(1600);
        BigDecimal descontoTipo = subtotal.multiply(BigDecimal.ONE.subtract(DESCONTO_15));
        BigDecimal descontoTotal = descontoTipo.multiply(BigDecimal.ONE.subtract(DESCONTO_20));

        // Frete base faixa B = 8kg * R$2 + R$12 = 28; Região NE = *1.1; Cliente OURO =
        // 100% off
        BigDecimal freteFinal = BigDecimal.ZERO;

        BigDecimal esperado = descontoTotal.add(freteFinal).setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.NORDESTE, TipoCliente.OURO);

        assertThat(total)
                .as("Todos os descontos aplicados corretamente no cenário completo")
                .isEqualByComparingTo(esperado);
    }
}
