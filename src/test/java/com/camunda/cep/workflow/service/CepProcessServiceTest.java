package com.camunda.cep.workflow.service;

import static com.camunda.cep.workflow.CepFixtures.PRACA_DA_SE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.camunda.cep.workflow.api.dto.CepResponse;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.command.ClientHttpException;
import io.camunda.client.api.command.ClientStatusException;
import io.camunda.client.api.response.ProcessInstanceResult;
import io.grpc.Status;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.test.util.ReflectionTestUtils;

class CepProcessServiceTest {

	CamundaClient camundaClient = mock(CamundaClient.class, Answers.RETURNS_DEEP_STUBS);
	CamundaFuture<ProcessInstanceResult> future = mockFuture();
	ExecucaoService execucaoService = mock(ExecucaoService.class);
	CepProcessService service = new CepProcessService(camundaClient, execucaoService);

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "timeout", Duration.ofSeconds(5));
		when(camundaClient.newCreateInstanceCommand()
				.bpmnProcessId(CepProcessService.PROCESS_ID)
				.latestVersion()
				.variables(any(Map.class))
				.withResult()
				.fetchVariables("endereco")
				// exige exatamente o timeout configurado: outro valor faria send() devolver null
				.requestTimeout(Duration.ofSeconds(5))
				.send())
				.thenReturn(future);
	}

	@Test
	void buscarCep_devolveEnderecoDoProcesso() {
		responderCom(PRACA_DA_SE);

		assertThat(service.buscarCep("01001000").join()).isEqualTo(PRACA_DA_SE);

		verify(execucaoService).iniciar(anyString(), eq("01001000"));
	}

	@Test
	void buscarCep_semEndereco_devolveVazio() {
		responderCom(null);

		assertThat(service.buscarCep("01001000").join()).isEqualTo(CepResponse.vazio());
	}

	@Test
	void buscarCep_repassaTimeoutDoZeebe() {
		when(future.toCompletableFuture()).thenReturn(
				CompletableFuture.failedFuture(new ClientHttpException(504, "Gateway Timeout")));

		assertThatThrownBy(() -> service.buscarCep("01001000").join())
				.isInstanceOf(CompletionException.class)
				.satisfies(ex -> assertThat(CepProcessService.isTimeout(ex)).isTrue());

		// timeout so encerra a espera: a execucao continua viva, nao e marcada como finalizada
		verify(execucaoService, never()).naoIniciada(anyString(), anyString());
	}

	@Test
	void buscarCep_quandoZeebeRecusa_marcaComoNaoIniciada() {
		when(future.toCompletableFuture()).thenReturn(
				CompletableFuture.failedFuture(new ClientHttpException(503, "Service Unavailable")));

		assertThatThrownBy(() -> service.buscarCep("01001000").join()).isInstanceOf(CompletionException.class);

		verify(execucaoService).naoIniciada(anyString(), startsWith("ClientHttpException"));
	}

	@Test
	void isTimeout_reconheceTodasAsFormas() {
		assertThat(CepProcessService.isTimeout(new CompletionException(new TimeoutException()))).isTrue();
		assertThat(CepProcessService.isTimeout(
				new ClientStatusException(Status.DEADLINE_EXCEEDED, null))).isTrue();
		assertThat(CepProcessService.isTimeout(new ClientHttpException(504, "x"))).isTrue();

		assertThat(CepProcessService.isTimeout(new ClientHttpException(500, "x"))).isFalse();
		assertThat(CepProcessService.isTimeout(new IllegalStateException())).isFalse();
	}

	private void responderCom(CepResponse endereco) {
		ProcessInstanceResult result = mock(ProcessInstanceResult.class);
		when(result.getProcessInstanceKey()).thenReturn(123L);
		when(result.getVariablesAsType(any())).thenAnswer(inv -> {
			Class<?> tipo = inv.getArgument(0);
			var construtor = tipo.getDeclaredConstructor(CepResponse.class);
			construtor.setAccessible(true);
			return construtor.newInstance(endereco);
		});
		when(future.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture(result));
	}

	@SuppressWarnings("unchecked")
	private static CamundaFuture<ProcessInstanceResult> mockFuture() {
		return mock(CamundaFuture.class);
	}
}
