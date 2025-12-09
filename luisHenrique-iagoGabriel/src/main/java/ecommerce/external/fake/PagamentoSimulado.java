package ecommerce.external.fake;

import org.springframework.stereotype.Service;

import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IPagamentoExternal;

@Service
public class PagamentoSimulado implements IPagamentoExternal {

	private boolean autorizado = true;
	private Long proximaTransacaoId = 1L;

	private boolean cancelarChamado = false;
	private Long ultimoClienteCancelamento;
	private Long ultimaTransacaoCancelamento;

	public void configurarAutorizacao(boolean autorizado) {
		this.autorizado = autorizado;
	}

	public void configurarProximaTransacao(Long transacaoId) {
		this.proximaTransacaoId = transacaoId;
	}

	public boolean isCancelarChamado() {
		return cancelarChamado;
	}

	public Long getUltimoClienteCancelamento() {
		return ultimoClienteCancelamento;
	}

	public Long getUltimaTransacaoCancelamento() {
		return ultimaTransacaoCancelamento;
	}

	@Override
	public PagamentoDTO autorizarPagamento(Long clienteId, Double custoTotal) {
		// record PagamentoDTO(Boolean autorizado, Long transacaoId)
		return new PagamentoDTO(autorizado, proximaTransacaoId);
	}

	@Override
	public void cancelarPagamento(Long clienteId, Long pagamentoTransacaoId) {
		this.cancelarChamado = true;
		this.ultimoClienteCancelamento = clienteId;
		this.ultimaTransacaoCancelamento = pagamentoTransacaoId;
	}
}
