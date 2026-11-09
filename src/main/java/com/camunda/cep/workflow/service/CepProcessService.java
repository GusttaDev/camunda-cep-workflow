package com.camunda.cep.workflow.service;

import com.camunda.cep.workflow.api.dto.CepResponse;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientHttpException;
import io.camunda.client.api.command.ClientStatusException;
import io.grpc.Status;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Spring -> Camunda.
 * Inicia uma instancia do process-cep e espera ela terminar (withResult),
 * devolvendo a variavel "endereco" produzida pelos workers.
 *
 * A espera NAO bloqueia a thread HTTP: o metodo devolve um CompletableFuture que o
 * Spring MVC completa quando o Zeebe responder (ou quando estourar o timeout).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CepProcessService {

	public static final String PROCESS_ID = "process-cep";

	/** Margem local alem do timeout do Zeebe, caso a resposta dele nunca chegue (ex.: rede). */
	private static final Duration MARGEM_LOCAL = Duration.ofSeconds(2);

	/**
	 * Por que acontece: no camunda-spring-boot-starter 8.10.9, o bean CamundaClient não vem de
	 * um método @Bean. Ele é registrado em tempo de execução por
	 * MultiCamundaClientBeanDefinitionRegistryPostProcessor, que chama
	 * registry.registerBeanDefinition(...) para cada cliente configurado e marca o principal
	 * como @Primary. Isso permite ter vários clientes Camunda. A inspeção do IntelliJ só analisa o
	 * código de forma estática, procurando @Bean, @Component e auto-configurations.
	 * Ela não consegue ver beans registrados assim, então avisa que eles não existem.
	 * Quando a aplicação sobe, o Spring encontra o bean normalmente.*/
	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	private final CamundaClient camundaClient;

	private final ExecucaoService execucaoService;

	/** Quanto tempo esperamos o processo terminar. */
	@Value("${cep.processo.timeout:10s}")
	private Duration timeout;

	/**
	 * Atencao: se o timeout estourar, so a ESPERA e cancelada. A instancia continua
	 * rodando no Zeebe e termina normalmente quando os workers fizerem sua parte.
	 *
	 * Rastreio: cada chamada ganha um rastreioId e uma linha em cep_execucao. O id vai como
	 * variavel do processo; os workers atualizam a linha a cada caixinha. Assim, mesmo que a
	 * resposta HTTP estoure o timeout, a linha mostra onde a mensagem esta.
	 */
	public CompletableFuture<CepResponse> buscarCep(String cep) {
		String rastreioId = UUID.randomUUID().toString();
		execucaoService.iniciar(rastreioId, cep);

		return camundaClient.newCreateInstanceCommand()
				.bpmnProcessId(PROCESS_ID)
				.latestVersion()
				.variables(Map.of("cep", cep, "rastreioId", rastreioId))
				.withResult()
				.fetchVariables("endereco")
				.requestTimeout(timeout)
				.send()
				.toCompletableFuture()
				.orTimeout(timeout.plus(MARGEM_LOCAL).toMillis(), TimeUnit.MILLISECONDS)
				.whenComplete((result, ex) -> {
					if (ex == null) {
						return;
					}
					if (isTimeout(ex)) {
						// so a espera acabou; a instancia segue no Zeebe e a cep_execucao continua sendo atualizada
						log.warn("[rastreio={}] Endpoint parou de esperar apos {} (cep={}). Consulte GET /execucoes/{}",
								rastreioId, timeout, cep, rastreioId);
					}
					else {
						Throwable causa = causaRaiz(ex);
						execucaoService.naoIniciada(rastreioId, causa.getClass().getSimpleName() + ": " + causa.getMessage());
					}
				})
				.thenApply(result -> {
					log.info("[rastreio={}] Instancia {} finalizada para cep={}", rastreioId,
							result.getProcessInstanceKey(), cep);
					CepResponse endereco = result.getVariablesAsType(ResultadoProcesso.class).endereco();
					return endereco != null ? endereco : CepResponse.vazio();
				});
	}

	/**
	 * Diz se o erro e "o processo nao terminou a tempo", seja qual for o caminho:
	 * timeout local, gRPC (DEADLINE_EXCEEDED) ou REST (HTTP 504).
	 */
	public static boolean isTimeout(Throwable ex) {
		Throwable causa = causaRaiz(ex);
		return causa instanceof TimeoutException
				|| (causa instanceof ClientStatusException grpc
						&& grpc.getStatusCode() == Status.Code.DEADLINE_EXCEEDED)
				|| (causa instanceof ClientHttpException http && http.code() == 504);
	}

	private static Throwable causaRaiz(Throwable ex) {
		Throwable causa = ex;
		while ((causa instanceof CompletionException || causa instanceof ExecutionException)
				&& causa.getCause() != null) {
			causa = causa.getCause();
		}
		return causa;
	}

	private record ResultadoProcesso(CepResponse endereco) {
	}
}
