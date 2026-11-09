package com.camunda.cep.workflow.api.dto;

import com.camunda.cep.workflow.domain.Execucao;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import com.camunda.cep.workflow.domain.enums.StatusExecucao;
import java.time.Duration;
import java.time.Instant;

/**
 * Estado de uma execucao.
 * semMovimentoHaSegundos: so para execucoes nao finalizadas; quanto tempo a mensagem esta parada.
 * duracaoMs: so para finalizadas; de solicitadoEm ate finalizadoEm.
 */
public record ExecucaoResponse(
		String rastreioId,
		String cep,
		Long processInstanceKey,
		StatusExecucao status,
		String etapaAtual,
		Integer tentativas,
		String ultimoErro,
		ResultadoExecucao resultado,
		Instant solicitadoEm,
		Instant atualizadoEm,
		Instant finalizadoEm,
		Long semMovimentoHaSegundos,
		Long duracaoMs) {

	public static ExecucaoResponse from(Execucao e, Instant agora) {
		Long parada = e.getFinalizadoEm() == null && e.getAtualizadoEm() != null
				? Duration.between(e.getAtualizadoEm(), agora).toSeconds()
				: null;
		Long duracao = e.getFinalizadoEm() != null && e.getSolicitadoEm() != null
				? Duration.between(e.getSolicitadoEm(), e.getFinalizadoEm()).toMillis()
				: null;
		return new ExecucaoResponse(e.getRastreioId(), e.getCep(), e.getProcessInstanceKey(), e.getStatus(),
				e.getEtapaAtual(), e.getTentativas(), e.getUltimoErro(), e.getResultado(), e.getSolicitadoEm(),
				e.getAtualizadoEm(), e.getFinalizadoEm(), parada, duracao);
	}
}
