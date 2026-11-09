package com.camunda.cep.workflow.worker;

import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import com.camunda.cep.workflow.service.CepService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caixinha "Buscar CEP na base".
 * Saida: encontradoNaBase (sempre) e endereco (quando encontrado).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuscarCepBaseWorker {

	private final CepService cepService;
	private final RastreioJob rastreio;

	@JobWorker(type = JobTypes.BUSCAR_CEP_BASE)
	public Map<String, Object> executar(ActivatedJob job,
			@Variable("cep") String cep,
			@Variable(value = "rastreioId", optional = true) String rastreioId) {
		return rastreio.executar(job, rastreioId, () -> {
			log.info("[{}] cep={}", JobTypes.BUSCAR_CEP_BASE, cep);
			return cepService.buscarNaBase(cep)
					.<Map<String, Object>>map(endereco -> Map.of(
							"encontradoNaBase", true,
							"endereco", endereco))
					.orElseGet(() -> Map.of("encontradoNaBase", false));
		}, saida -> Boolean.TRUE.equals(saida.get("encontradoNaBase")) ? ResultadoExecucao.BASE : null);
	}
}
