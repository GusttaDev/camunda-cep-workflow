package com.camunda.cep.workflow.job;

import com.camunda.cep.workflow.domain.Execucao;
import com.camunda.cep.workflow.service.ExecucaoService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Alerta por log: execucoes sem nenhum movimento ha mais que o limite.
 *
 * Pega tambem o caso que nenhum worker consegue registrar: o job nunca foi pego
 * (aplicacao fora do ar, type do BPMN diferente do JobTypes). Nesse caso o status
 * fica AGUARDANDO e o atualizado_em para de andar.
 */
@Slf4j
@Component
class MonitorExecucoesParadas {

	private final ExecucaoService execucaoService;
	private final Duration limite;

	MonitorExecucoesParadas(ExecucaoService execucaoService,
			@Value("${cep.execucao.limite-parada:5m}") Duration limite) {
		this.execucaoService = execucaoService;
		this.limite = limite;
	}

	@Scheduled(fixedDelayString = "${cep.execucao.verificar-a-cada:PT1M}")
	void verificar() {
		for (Execucao e : execucaoService.paradasHaMaisDe(limite)) {
			Duration parada = Duration.between(e.getAtualizadoEm(), execucaoService.agora());
			log.warn("[rastreio={}] Execucao PARADA ha {}s: cep={} status={} etapa={} tentativas={} instancia={} erro={}",
					e.getRastreioId(), parada.toSeconds(), e.getCep(), e.getStatus(), e.getEtapaAtual(),
					e.getTentativas(), e.getProcessInstanceKey(), e.getUltimoErro());
		}
	}
}
