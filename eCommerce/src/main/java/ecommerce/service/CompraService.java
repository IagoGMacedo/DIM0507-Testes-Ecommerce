package ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import static ecommerce.utils.Msg.get;
import jakarta.transaction.Transactional;

@Service
public class CompraService {
	private static final BigDecimal LIMIAR_1000 = new BigDecimal("1000.00");
	private static final BigDecimal LIMIAR_500 = new BigDecimal("500.00");

	private static final BigDecimal DESCONTO_20 = new BigDecimal("0.20");
	private static final BigDecimal DESCONTO_15 = new BigDecimal("0.15");
	private static final BigDecimal DESCONTO_10 = new BigDecimal("0.10");
	private static final BigDecimal DESCONTO_05 = new BigDecimal("0.05");

	private static final BigDecimal FATOR_CUBICO = new BigDecimal("6000");

	private static final BigDecimal CINCO = new BigDecimal("5.00");
	private static final BigDecimal DEZ = new BigDecimal("10.00");
	private static final BigDecimal CINQUENTA = new BigDecimal("50.00");

	private static final BigDecimal RATE_B = new BigDecimal("2.00");
	private static final BigDecimal RATE_C = new BigDecimal("4.00");
	private static final BigDecimal RATE_D = new BigDecimal("7.00");

	private static final BigDecimal TAXA_MINIMA = new BigDecimal("12.00");
	private static final BigDecimal TAXA_FRAGIL_POR_UNIDADE = new BigDecimal("5.00");

	private static final BigDecimal MULTIPLICADOR_SUDESTE = new BigDecimal("1.00");
	private static final BigDecimal MULTIPLICADOR_SUL = new BigDecimal("1.05");
	private static final BigDecimal MULTIPLICADOR_NORDESTE = new BigDecimal("1.10");
	private static final BigDecimal MULTIPLICADOR_CENTRO_OESTE = new BigDecimal("1.20");
	private static final BigDecimal MULTIPLICADOR_NORTE = new BigDecimal("1.30");

	private static final BigDecimal DESCONTO_BRONZE = BigDecimal.ZERO;
	private static final BigDecimal DESCONTO_PRATA = new BigDecimal("0.5");
	private static final BigDecimal DESCONTO_OURO = BigDecimal.ONE;

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal) {
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel()) {
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado()) {
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso()) {
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	}

	private void validarTipoClienteParaCompra(TipoCliente tipoCliente) {
		if (tipoCliente == null) {
			throw new IllegalArgumentException(get("erro.tipoCliente.invalido"));
		}
	}

	private void validarCarrinhoParaCompra(CarrinhoDeCompras carrinho) {
		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			throw new IllegalArgumentException(get("erro.carrinho.invalido"));
		}

		carrinho.getItens().forEach(item -> {
			validarItemCompra(item);
		});
	}

	private void validarItemCompra(ItemCompra item) {
		if (item.getProduto() == null) {
			throw new IllegalArgumentException(get("erro.item.produto.invalido"));
		}

		if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
			throw new IllegalArgumentException(get("erro.item.quantidade.invalida"));
		}

		validarProduto(item.getProduto());

	}

	private void validarProduto(Produto produto) {
		if (islNullOrLessEqualZero(produto.getAltura()) ||
				islNullOrLessEqualZero(produto.getLargura()) ||
				islNullOrLessEqualZero(produto.getComprimento())) {
			throw new IllegalArgumentException(get("erro.produto.dimensoes.invalidas"));
		}
		if (islNullOrLessEqualZero(produto.getPesoFisico()))
			throw new IllegalArgumentException(get("erro.produto.peso.invalido"));
		if (islNullOrLessEqualZero(produto.getPreco()))
			throw new IllegalArgumentException(get("erro.produto.preco.invalido"));
		if (produto.getTipo() == null)
			throw new IllegalArgumentException(get("erro.produto.tipo.invalido"));
		if (produto.getNome() == null || produto.getNome().isBlank())
			throw new IllegalArgumentException(get("erro.produto.nome.invalido"));
		if (produto.getDescricao() == null || produto.getDescricao().isBlank())
			throw new IllegalArgumentException(get("erro.produto.descricao.invalida"));

	}

	private boolean islNullOrLessEqualZero(BigDecimal value) {
		return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {
		validarCarrinhoParaCompra(carrinho);
		validarTipoClienteParaCompra(tipoCliente);

		var subtotal = calcularSubtotal(carrinho);
		var frete = calcularFreteCompra(carrinho, regiao, tipoCliente);

		return subtotal.add(frete).setScale(2, RoundingMode.HALF_UP);
	}

	// Subtotal com desconto
	public BigDecimal calcularSubtotal(CarrinhoDeCompras carrinho) {

		Map<TipoProduto, List<ItemCompra>> itensPorTipo = carrinho.getItens().stream()
				.collect(Collectors.groupingBy(
						i -> i.getProduto().getTipo(),
						() -> new EnumMap<>(TipoProduto.class),
						Collectors.toList()));

		BigDecimal subtotal = itensPorTipo.values().stream()
				.map(this::calcularSubtotalPorCategoriaProduto)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal percentual = obterPercentualDescontoCarrinho(subtotal);
		BigDecimal totalComDesconto = subtotal.subtract(subtotal.multiply(percentual));

		return totalComDesconto;
	}

	private BigDecimal obterPercentualDescontoCarrinho(BigDecimal subtotal) {
		if (subtotal.compareTo(LIMIAR_1000) > 0) {
			return DESCONTO_20;
		}
		if (subtotal.compareTo(LIMIAR_500) > 0) {
			return DESCONTO_10;
		}
		return BigDecimal.ZERO;
	}

	public BigDecimal calcularSubtotalPorCategoriaProduto(List<ItemCompra> itens) {
		BigDecimal subtotal = itens.stream()
				.map(i -> i.getProduto().getPreco()
						.multiply(BigDecimal.valueOf(i.getQuantidade())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// long totalItens = itens.stream()
		// .map(ItemCompra::getQuantidade)
		// .mapToLong(Long::longValue)
		// .sum();
		long totalItens = itens.stream()
				.count();

		BigDecimal percentual = obterPercentualDescontoPorQuantidade(totalItens);
		BigDecimal desconto = subtotal.multiply(percentual);

		return subtotal.subtract(desconto);
	}

	private BigDecimal obterPercentualDescontoPorQuantidade(long totalItens) {
		if (totalItens >= 8)
			return DESCONTO_15;
		if (totalItens >= 5)
			return DESCONTO_10;
		if (totalItens >= 3)
			return DESCONTO_05;
		return BigDecimal.ZERO;
	}

	public BigDecimal calcularFreteCompra(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {

		BigDecimal freteBase = calcularFreteBase(carrinho, regiao);

		BigDecimal fatorDesconto = BigDecimal.ONE.subtract(obterPercentualDescontoFrete(tipoCliente));

		return freteBase.multiply(fatorDesconto);
	}

	private BigDecimal obterPercentualDescontoFrete(TipoCliente tipoCliente) {
		switch (tipoCliente) {
			case BRONZE:
				return DESCONTO_BRONZE;
			case PRATA:
				return DESCONTO_PRATA;
			case OURO:
				return DESCONTO_OURO;
			default:
				return BigDecimal.ZERO;
		}
	}

	public BigDecimal calcularFreteBase(CarrinhoDeCompras carrinho, Regiao regiao) {
		BigDecimal pesoTotal = calcularPesoTotalCompra(carrinho);
		BigDecimal encargoPeso = calcularEncargoPeso(pesoTotal);
		BigDecimal encargoFragilidade = calcularEncargoFragilidade(carrinho);

		BigDecimal subtotal = encargoPeso.add(encargoFragilidade);
		return subtotal.multiply(obterMultiplicadorRegiao(regiao));
	}

	private BigDecimal obterMultiplicadorRegiao(Regiao regiao) {
		switch (regiao) {
			case NORTE:
				return MULTIPLICADOR_NORTE;
			case NORDESTE:
				return MULTIPLICADOR_NORDESTE;
			case SUL:
				return MULTIPLICADOR_SUL;
			case SUDESTE:
				return MULTIPLICADOR_SUDESTE;
			case CENTRO_OESTE:
				return MULTIPLICADOR_CENTRO_OESTE;
			default:
				return BigDecimal.ONE;
		}
	}

	private BigDecimal calcularEncargoPeso(BigDecimal peso) {

		if (peso == null || peso.compareTo(CINCO) <= 0) {
			return BigDecimal.ZERO;
		}

		BigDecimal rate;
		if (peso.compareTo(DEZ) <= 0) {
			rate = RATE_B;
		} else if (peso.compareTo(CINQUENTA) <= 0) {
			rate = RATE_C;
		} else {
			rate = RATE_D;
		}

		return peso.multiply(rate).add(TAXA_MINIMA);
	}

	private BigDecimal calcularEncargoFragilidade(CarrinhoDeCompras carrinho) {
		long unidadesFragil = carrinho.getItens().stream()
				.filter(i -> i.getProduto().isFragil())
				.map(ItemCompra::getQuantidade)
				.mapToLong(Long::longValue)
				.sum();

		return BigDecimal.valueOf(unidadesFragil).multiply(TAXA_FRAGIL_POR_UNIDADE);
	}

	public BigDecimal calcularPesoTotalCompra(CarrinhoDeCompras carrinho) {
		BigDecimal pesoTotal = carrinho.getItens().stream()
				.map(this::calcularPesoTributavelItem)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return pesoTotal;
	}

	public BigDecimal calcularPesoTributavelItem(ItemCompra item) {
		BigDecimal pesoFisico = calcularPesoFisicoItem(item);
		BigDecimal pesoCubico = calcularPesoCubicoItem(item);

		return pesoFisico.max(pesoCubico);
	}

	public BigDecimal calcularPesoFisicoItem(ItemCompra item) {
		return item.getProduto().getPesoFisico().multiply(BigDecimal.valueOf(item.getQuantidade()));
	}

	public BigDecimal calcularPesoCubicoItem(ItemCompra item) {
		BigDecimal comprimento = item.getProduto().getComprimento();
		BigDecimal largura = item.getProduto().getLargura();
		BigDecimal altura = item.getProduto().getAltura();

		BigDecimal pesoCubicoUnit = comprimento.multiply(largura).multiply(altura)
				.divide(FATOR_CUBICO, 10, RoundingMode.HALF_UP);

		BigDecimal qtd = BigDecimal.valueOf(item.getQuantidade());

		BigDecimal pesoCubicoTotal = pesoCubicoUnit.multiply(qtd);

		return pesoCubicoTotal;
	}
}
