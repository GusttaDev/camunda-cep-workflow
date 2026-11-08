package com.camunda.cep.workflow.api.dto;

import com.camunda.cep.workflow.domain.Cep;

/**
 * Objeto devolvido pela API e tambem guardado como variavel "endereco" no processo.
 */
public record CepResponse(
		String cep,
		String logradouro,
		String complemento,
		String bairro,
		String localidade,
		String uf,
		String ibge,
		String ddd) {

	/** CEP nao encontrado nem na base nem no ViaCEP. */
	public static CepResponse vazio() {
		return new CepResponse("", "", "", "", "", "", "", "");
	}

	public static CepResponse from(Cep entity) {
		return new CepResponse(entity.getCep(), entity.getLogradouro(), entity.getComplemento(),
				entity.getBairro(), entity.getLocalidade(), entity.getUf(), entity.getIbge(), entity.getDdd());
	}

	public Cep toEntity() {
		return new Cep(cep, logradouro, complemento, bairro, localidade, uf, ibge, ddd);
	}
}
