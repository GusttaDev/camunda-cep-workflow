package com.camunda.cep.workflow.worker;

import com.camunda.cep.workflow.api.dto.CepResponse;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import com.camunda.cep.workflow.service.CepService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Caixinha "Salvar CEP na base". Nao devolve variaveis: o job e apenas completado.
 * Grava junto o rastreioId, que liga o registro a execucao (cep_execucao) que o salvou.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalvarCepWorker {

	private final CepService cepService;
	private final RastreioJob rastreio;

	@JobWorker(type = JobTypes.SALVAR_CEP)
	public void executar(ActivatedJob job,
			@Variable("endereco") CepResponse endereco,
			@Variable(value = "rastreioId", optional = true) String rastreioId) {
		rastreio.executar(job, rastreioId, () -> {
			log.info("[{}] cep={}", JobTypes.SALVAR_CEP, endereco.cep());
			cepService.salvar(endereco, rastreioId);
		}, ResultadoExecucao.VIACEP);
	}
}
