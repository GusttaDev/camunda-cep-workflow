package com.camunda.cep.workflow.worker;

import com.camunda.cep.workflow.service.ExecucaoService;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import io.camunda.client.api.response.ActivatedJob;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Envolve a execucao de uma caixinha atualizando o estado da execucao (cep_execucao):
 * qual caixinha, qual tentativa, se falhou e por que, e se o processo terminou.
 *
 * Tambem coloca rastreioId/etapa no MDC: qualquer log feito durante a etapa
 * (inclusive no CepService e no ViaCepClient) sai com esses campos.
 */
@Component
@RequiredArgsConstructor
public class RastreioJob {

	private final ExecucaoService execucaoService;

	/**
	 * @param fimDoProcesso dado o resultado da caixinha, diz se o processo termina aqui (e com qual
	 *                      resultado) ou devolve nulo se ele segue. Espelha os gateways do BPMN.
	 */
	public <T> T executar(ActivatedJob job, String rastreioId, Supplier<T> etapa,
			Function<T, ResultadoExecucao> fimDoProcesso) {
		MDC.put("rastreioId", rastreioId);
		MDC.put("etapa", job.getElementId());
		try {
			execucaoService.etapaIniciada(rastreioId, job.getProcessInstanceKey(), job.getElementId());
			T resultado = etapa.get();
			execucaoService.etapaConcluida(rastreioId, fimDoProcesso.apply(resultado));
			return resultado;
		}
		catch (RuntimeException e) {
			execucaoService.etapaFalhou(rastreioId, causaRaiz(e), job.getRetries() <= 1);
			throw e; // o Zeebe precisa ver a excecao para fazer retry / abrir incident
		}
		finally {
			MDC.remove("rastreioId");
			MDC.remove("etapa");
		}
	}

	public void executar(ActivatedJob job, String rastreioId, Runnable etapa, ResultadoExecucao fimDoProcesso) {
		executar(job, rastreioId, () -> {
			etapa.run();
			return null;
		}, ignorado -> fimDoProcesso);
	}

	/**
	 * Tipo da causa raiz + primeira linha da mensagem mais especifica que existir.
	 * Ex.: HttpTimeoutException nao tem mensagem; ai vale a da excecao que a envolve
	 * ("I/O error on GET request for ...").
	 */
	static String causaRaiz(Throwable e) {
		Throwable raiz = e;
		String mensagem = primeiraLinha(e);
		while (raiz.getCause() != null && raiz.getCause() != raiz) {
			raiz = raiz.getCause();
			String daCausa = primeiraLinha(raiz);
			if (!daCausa.isBlank()) {
				mensagem = daCausa;
			}
		}
		return mensagem.isBlank()
				? raiz.getClass().getSimpleName()
				: raiz.getClass().getSimpleName() + ": " + mensagem;
	}

	private static String primeiraLinha(Throwable e) {
		return e.getMessage() == null ? "" : e.getMessage().lines().findFirst().orElse("").trim();
	}
}
