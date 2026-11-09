package com.camunda.cep.workflow.worker;

import com.camunda.cep.workflow.api.dto.CepResponse;
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
 * Caixinha "Consultar ViaCEP".
 * Saida: encontradoNoViaCep e endereco (vazio quando nao encontrado).
 * Falha de rede vira excecao: o Zeebe faz retry e, esgotado, abre um incident.
 * Cada falha fica registrada na cep_execucao com o motivo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultarViaCepWorker {

	private final CepService cepService;
	private final RastreioJob rastreio;

	@JobWorker(type = JobTypes.CONSULTAR_VIACEP)
	public Map<String, Object> executar(ActivatedJob job,
			@Variable("cep") String cep,
			@Variable(value = "rastreioId", optional = true) String rastreioId) {
		return rastreio.executar(job, rastreioId, () -> {
			log.info("[{}] cep={}", JobTypes.CONSULTAR_VIACEP, cep);
			return cepService.buscarNoViaCep(cep)
					.<Map<String, Object>>map(endereco -> Map.of(
							"encontradoNoViaCep", true,
							"endereco", endereco))
					.orElseGet(() -> Map.of(
							"encontradoNoViaCep", false,
							"endereco", CepResponse.vazio()));
		}, saida -> Boolean.TRUE.equals(saida.get("encontradoNoViaCep")) ? null : ResultadoExecucao.NAO_ENCONTRADO);
	}
}
