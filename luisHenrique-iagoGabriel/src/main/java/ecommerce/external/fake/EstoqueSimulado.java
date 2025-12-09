package ecommerce.external.fake;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.external.IEstoqueExternal;

@Service
public class EstoqueSimulado implements IEstoqueExternal {
	private boolean disponivel = true;
	private boolean baixaSucesso = true;

	private boolean verificarDisponibilidadeChamado = false;
	private boolean darBaixaChamado = false;

	public void configurarDisponibilidade(boolean disponivel) {
		this.disponivel = disponivel;
	}

	public void configurarBaixaSucesso(boolean baixaSucesso) {
		this.baixaSucesso = baixaSucesso;
	}

	public boolean isVerificarDisponibilidadeChamado() {
		return verificarDisponibilidadeChamado;
	}

	public boolean isDarBaixaChamado() {
		return darBaixaChamado;
	}

	@Override
	public EstoqueBaixaDTO darBaixa(List<Long> produtosIds, List<Long> produtosQuantidades) {
		this.darBaixaChamado = true;
		// record EstoqueBaixaDTO(Boolean sucesso)
		return new EstoqueBaixaDTO(baixaSucesso);
	}

	@Override
	public DisponibilidadeDTO verificarDisponibilidade(List<Long> produtosIds, List<Long> produtosQuantidades) {
		this.verificarDisponibilidadeChamado = true;
		// record DisponibilidadeDTO(Boolean disponivel, List<Long>
		// idsProdutosIndisponiveis)
		return new DisponibilidadeDTO(disponivel, Collections.emptyList());
	}
}
