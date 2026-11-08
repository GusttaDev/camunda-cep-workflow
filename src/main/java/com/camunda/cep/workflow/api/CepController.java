package com.camunda.cep.workflow.api;

import com.camunda.cep.workflow.api.dto.CepResponse;
import com.camunda.cep.workflow.service.CepProcessService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ceps")
@RequiredArgsConstructor
public class CepController {

	private final CepProcessService cepProcessService;

	/**
	 * Ex.: GET /ceps/01001000 ou /ceps/01001-000
	 * Retornar CompletableFuture libera a thread do Tomcat enquanto o processo roda;
	 * o Spring MVC envia a resposta quando o future completar.
	 */
	@GetMapping("/{cep}")
	public CompletableFuture<CepResponse> buscar(@PathVariable String cep) {
		return cepProcessService.buscarCep(normalizar(cep))
				.exceptionally(ex -> {
					if (CepProcessService.isTimeout(ex)) {
						throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
								"Processo nao terminou a tempo", ex);
					}
					throw ex instanceof CompletionException ce ? ce : new CompletionException(ex);
				});
	}

	static String normalizar(String cep) {
		String normalizado = cep.replace("-", "").trim();
		if (!normalizado.matches("\\d{8}")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP deve ter 8 digitos");
		}
		return normalizado;
	}
}
