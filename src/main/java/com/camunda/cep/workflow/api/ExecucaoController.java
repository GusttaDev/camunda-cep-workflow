package com.camunda.cep.workflow.api;

import com.camunda.cep.workflow.api.dto.ExecucaoResponse;
import com.camunda.cep.workflow.domain.Execucao;
import com.camunda.cep.workflow.service.ExecucaoService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Consulta do rastreio: onde cada mensagem esta, onde parou e por que.
 */
@RestController
@RequiredArgsConstructor
public class ExecucaoController {

	private final ExecucaoService execucaoService;

	/** Tudo que foi disparado e ainda nao terminou, da que esta parada ha mais tempo para a mais recente. */
	@GetMapping("/execucoes/pendentes")
	public List<ExecucaoResponse> pendentes() {
		return responder(execucaoService.pendentes());
	}

	/** Uma execucao especifica (o rastreioId aparece nos logs). */
	@GetMapping("/execucoes/{rastreioId}")
	public ExecucaoResponse porId(@PathVariable String rastreioId) {
		return execucaoService.porId(rastreioId)
				.map(e -> ExecucaoResponse.from(e, execucaoService.agora()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Execucao nao encontrada"));
	}

	/** Ultimas 20 execucoes de um CEP, da mais recente para a mais antiga. */
	@GetMapping("/ceps/{cep}/execucoes")
	public List<ExecucaoResponse> porCep(@PathVariable String cep) {
		return responder(execucaoService.porCep(CepController.normalizar(cep)));
	}

	private List<ExecucaoResponse> responder(List<Execucao> execucoes) {
		Instant agora = execucaoService.agora();
		return execucoes.stream().map(e -> ExecucaoResponse.from(e, agora)).toList();
	}
}
