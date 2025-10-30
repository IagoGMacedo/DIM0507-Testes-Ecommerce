package ecommerce.service.caixaPreta;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import ecommerce.service.CarrinhoDeComprasService;
import ecommerce.service.ClienteService;
import ecommerce.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ecommerce.service.DomainTestData.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("CompraService - Tabela de Decisão")
public class CompraServiceTabelaDecisaoTest {
    private CompraService service;

    @BeforeEach
    void setUp() {
        CarrinhoDeComprasService carrinhoSvc = Mockito.mock(CarrinhoDeComprasService.class);
        ClienteService clienteSvc = Mockito.mock(ClienteService.class);
        IEstoqueExternal estoque = Mockito.mock(IEstoqueExternal.class);
        IPagamentoExternal pagamento = Mockito.mock(IPagamentoExternal.class);
        service = new CompraService(carrinhoSvc, clienteSvc, estoque, pagamento);
    }

    @ParameterizedTest
    @CsvSource({
            "400,2,0+0",    //TC42
            "400,3,5+0",    //TC43
            "400,6,10+0",   //TC44
            "400,8,15+0",   //TC45
            "800,2,0+10",   //TC46
            "800,3,5+10",   //TC47
            "800,6,10+10",  //TC48
            "800,8,15+10",  //TC49
            "1200,2,0+20",  //TC50
            "1200,3,5+20",  //TC51
            "1200,6,10+20", //TC52
            "1200,8,15+20"  //TC53
    })
    public void calcularSubtotal_tabelaDecisao(BigDecimal subtotal, Integer itensMesmoTipo, String descontoEsperado){
        CarrinhoDeCompras carrinho = carrinhoVazio();
        BigDecimal valorIndividual = subtotal.divide(BigDecimal.valueOf(itensMesmoTipo), 4, RoundingMode.HALF_UP);
        adicionarVariosItems(carrinho, produtoBasico(valorIndividual), itensMesmoTipo);

        BigDecimal resultado = service.calcularSubtotal(carrinho).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotalEsperado = calcularDesconto(descontoEsperado, subtotal).setScale(2, RoundingMode.HALF_UP);

        assertThat(resultado).isCloseTo(subtotalEsperado, within(BigDecimal.valueOf(0.01)));
    }

    @ParameterizedTest
    @CsvSource({
            "SUL, 3, OURO, true",                   // TC54
            "SUL, 3, BRONZE, false",                // TC55
            "SUL, 3, BRONZE, true",                 // TC56
            "SUL, 3, PRATA, true",                  // TC57
            "NORTE, 6, BRONZE, false",              // TC58
            "NORTE, 6, BRONZE, true",               // TC59
            "NORTE, 6, PRATA, false",               // TC60
            "NORTE, 6, PRATA, true",                // TC61
            "NORTE, 25, BRONZE, false",             // TC62
            "NORTE, 25, BRONZE, true",              // TC63
            "NORTE, 25, PRATA, false",              // TC64
            "NORTE, 25, PRATA, true",               // TC65
            "NORTE, 80, BRONZE, false",             // TC66
            "NORTE, 80, BRONZE, true",              // TC67
            "NORTE, 80, PRATA, false",              // TC68
            "NORTE, 80, PRATA, true",               // TC69
            "NORDESTE, 6, BRONZE, false",           // TC70
            "NORDESTE, 6, BRONZE, true",            // TC71
            "NORDESTE, 6, PRATA, false",            // TC72
            "NORDESTE, 6, PRATA, true",             // TC73
            "NORDESTE, 25, BRONZE, false",          // TC74
            "NORDESTE, 25, BRONZE, true",           // TC75
            "NORDESTE, 25, PRATA, false",           // TC76
            "NORDESTE, 25, PRATA, true",            // TC77
            "NORDESTE, 80, BRONZE, false",          // TC78
            "NORDESTE, 80, BRONZE, true",           // TC79
            "NORDESTE, 80, PRATA, false",           // TC80
            "NORDESTE, 80, PRATA, true",            // TC81
            "SUL, 6, BRONZE, false",                // TC82
            "SUL, 6, BRONZE, true",                 // TC83
            "SUL, 6, PRATA, false",                 // TC84
            "SUL, 6, PRATA, true",                  // TC85
            "SUL, 25, BRONZE, false",               // TC86
            "SUL, 25, BRONZE, true",                // TC87
            "SUL, 25, PRATA, false",                // TC88
            "SUL, 25, PRATA, true",                 // TC89
            "SUL, 80, BRONZE, false",               // TC90
            "SUL, 80, BRONZE, true",                // TC91
            "SUL, 80, PRATA, false",                // TC92
            "SUL, 80, PRATA, true",                 // TC93
            "SUDESTE, 6, BRONZE, false",            // TC94
            "SUDESTE, 6, BRONZE, true",             // TC95
            "SUDESTE, 6, PRATA, false",             // TC96
            "SUDESTE, 6, PRATA, true",              // TC97
            "SUDESTE, 25, BRONZE, false",           // TC98
            "SUDESTE, 25, BRONZE, true",            // TC99
            "SUDESTE, 25, PRATA, false",            // TC100
            "SUDESTE, 25, PRATA, true",             // TC101
            "SUDESTE, 80, BRONZE, false",           // TC102
            "SUDESTE, 80, BRONZE, true",            // TC103
            "SUDESTE, 80, PRATA, false",            // TC104
            "SUDESTE, 80, PRATA, true",             // TC105
            "CENTRO_OESTE, 6, BRONZE, false",       // TC106
            "CENTRO_OESTE, 6, BRONZE, true",        // TC107
            "CENTRO_OESTE, 6, PRATA, false",        // TC108
            "CENTRO_OESTE, 6, PRATA, true",         // TC109
            "CENTRO_OESTE, 25, BRONZE, false",      // TC110
            "CENTRO_OESTE, 25, BRONZE, true",       // TC111
            "CENTRO_OESTE, 25, PRATA, false",       // TC112
            "CENTRO_OESTE, 25, PRATA, true",        // TC113
            "CENTRO_OESTE, 80, BRONZE, false",      // TC114
            "CENTRO_OESTE, 80, BRONZE, true",       // TC115
            "CENTRO_OESTE, 80, PRATA, false",       // TC116
            "CENTRO_OESTE, 80, PRATA, true",        // TC117
    })
    public void calcularFrete_tabelaDecisao(Regiao regiao, BigDecimal peso, TipoCliente tipoCliente, boolean fragil){
        Produto p = produtoBasico();
        p.setPesoFisico(peso);
        p.setFragil(fragil);
        CarrinhoDeCompras carrinho = carrinhoComItem(p, 1L);

        BigDecimal resultado = service.calcularFreteCompra(carrinho, regiao, tipoCliente);
        BigDecimal esperado = freteEsperado(regiao, peso, tipoCliente, fragil);

        assertThat(resultado).isEqualByComparingTo(esperado);
    }

    private BigDecimal freteEsperado(Regiao regiao, BigDecimal peso, TipoCliente tipoCliente, boolean fragil){
        BigDecimal frete = BigDecimal.ZERO;
        if(tipoCliente.equals(TipoCliente.OURO)) return frete;

        if(peso.compareTo(BigDecimal.valueOf(5)) > 0){
            if(peso.compareTo(BigDecimal.valueOf(10)) <= 0){
                frete = peso.multiply(BigDecimal.valueOf(2)).add(BigDecimal.valueOf(12));
            } else if(peso.compareTo(BigDecimal.valueOf(50)) <= 0){
                frete = peso.multiply(BigDecimal.valueOf(4)).add(BigDecimal.valueOf(12));
            } else {
                frete = peso.multiply(BigDecimal.valueOf(7)).add(BigDecimal.valueOf(12));
            }
        }
        if(fragil){
            frete = frete.add(BigDecimal.valueOf(5));
        }
        switch (regiao){
            case NORTE -> frete = frete.multiply(BigDecimal.valueOf(1.3));
            case NORDESTE -> frete = frete.multiply(BigDecimal.valueOf(1.1));
            case SUL -> frete = frete.multiply(BigDecimal.valueOf(1.05));
            case CENTRO_OESTE -> frete = frete.multiply(BigDecimal.valueOf(1.2));
        }
        if (tipoCliente.equals(TipoCliente.PRATA)){
            return frete.multiply(BigDecimal.valueOf(0.5));
        } else {
            return frete;
        }
    }

    private BigDecimal calcularDesconto(String desconto, BigDecimal valor){
        if (desconto == null || desconto.isBlank()) return valor;

        String[] partes = desconto.split("\\+");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Expressão inválida. Use o formato 'X+Y'.");
        }

        double p1 = Double.parseDouble(partes[0]);
        double p2 = Double.parseDouble(partes[1]);

        return valor.multiply(BigDecimal.valueOf(1 - p1 / 100)).multiply (BigDecimal.valueOf(1 - p2 / 100));
    }
}
