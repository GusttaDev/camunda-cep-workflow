package com.camunda.cep.workflow.client;

import com.camunda.cep.workflow.api.dto.CepResponse;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ViaCepClient {

	private final RestClient restClient;

	/**
	 * Timeouts curtos: se o ViaCEP travar, a chamada falha rapido com ResourceAccessException,
	 * o job falha e o Zeebe faz o retry, em vez de prender o worker esperando.
	 */
	public ViaCepClient(RestClient.Builder builder,
			@Value("${viacep.base-url}") String baseUrl,
			@Value("${viacep.connect-timeout:1s}") Duration connectTimeout,
			@Value("${viacep.read-timeout:2s}") Duration readTimeout) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(connectTimeout)
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(readTimeout);

		this.restClient = builder
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.build();
	}

	/**
	 * @param cep somente digitos (8)
	 * @return o endereco, ou vazio se o ViaCEP nao conhece o CEP.
	 *         Erros de rede/5xx sobem como excecao (o Camunda faz retry e, no fim, abre um incident).
	 */
	public Optional<CepResponse> buscar(String cep) {
		log.debug("Consultando ViaCEP para {}", cep);
		try {
			ViaCepResponse response = restClient.get()
					.uri("/ws/{cep}/json/", cep)
					.retrieve()
					.body(ViaCepResponse.class);

			if (response == null || response.naoEncontrado()) {
				return Optional.empty();
			}
			return Optional.of(new CepResponse(cep, response.logradouro(), response.complemento(),
					response.bairro(), response.localidade(), response.uf(), response.ibge(), response.ddd()));
		}
		catch (HttpClientErrorException.BadRequest e) {
			// ViaCEP responde 400 para CEP com formato invalido
			return Optional.empty();
		}
	}
}
