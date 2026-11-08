package com.camunda.cep.workflow.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JSON do ViaCEP. Para CEP inexistente (formato valido) ele responde {"erro": true}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(
		String cep,
		String logradouro,
		String complemento,
		String bairro,
		String localidade,
		String uf,
		String ibge,
		String ddd,
		Boolean erro) {

	public boolean naoEncontrado() {
		return Boolean.TRUE.equals(erro);
	}
}
