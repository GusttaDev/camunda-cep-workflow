package com.camunda.cep.workflow.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camunda.cep.workflow.service.ExecucaoService;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import io.camunda.client.api.response.ActivatedJob;
import java.sql.SQLSyntaxErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RastreioJobTest {

	@Mock
	ExecucaoService execucaoService;

	@Mock
	ActivatedJob job;

	RastreioJob rastreio;

	@BeforeEach
	void setUp() {
		rastreio = new RastreioJob(execucaoService);
		when(job.getProcessInstanceKey()).thenReturn(123L);
		when(job.getElementId()).thenReturn("Task_ConsultarViaCep");
	}

	@Test
	void sucesso_marcaEtapaEDecideSeOProcessoTerminou() {
		assertThat(rastreio.executar(job, "r1", () -> "nao-achou",
				saida -> ResultadoExecucao.NAO_ENCONTRADO)).isEqualTo("nao-achou");

		InOrder ordem = inOrder(execucaoService);
		ordem.verify(execucaoService).etapaIniciada("r1", 123L, "Task_ConsultarViaCep");
		ordem.verify(execucaoService).etapaConcluida("r1", ResultadoExecucao.NAO_ENCONTRADO);
		assertThat(MDC.get("rastreioId")).isNull();
	}

	@Test
	void falhaComRetries_marcaFalhandoEPropagaExcecao() {
		when(job.getRetries()).thenReturn(3);

		assertThatThrownBy(() -> rastreio.executar(job, "r1", () -> {
			throw new ResourceAccessException("I/O error", new java.net.ConnectException("Connection refused"));
		}, saida -> null)).isInstanceOf(ResourceAccessException.class);

		verify(execucaoService).etapaFalhou("r1", "ConnectException: Connection refused", false);
		verify(execucaoService, never()).etapaConcluida(anyString(), any());
	}

	@Test
	void falhaNaUltimaTentativa_marcaIncident() {
		when(job.getRetries()).thenReturn(1);

		assertThatThrownBy(() -> rastreio.executar(job, "r1", () -> {
			throw new ResourceAccessException("timeout");
		}, saida -> null)).isInstanceOf(ResourceAccessException.class);

		verify(execucaoService).etapaFalhou(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(true));
	}

	@Test
	void causaRaiz_ficaCurta() {
		var erro = new InvalidDataAccessResourceUsageException("Could not prepare statement [select ... muito longo]",
				new SQLSyntaxErrorException("Column \"C1_0.ATUALIZADO_EM\" not found; SQL statement:\nselect c1_0.cep ..."));

		assertThat(RastreioJob.causaRaiz(erro))
				.isEqualTo("SQLSyntaxErrorException: Column \"C1_0.ATUALIZADO_EM\" not found; SQL statement:");
	}

	@Test
	void causaRaizSemMensagem_usaAMensagemDaExcecaoQueAEnvolve() {
		var erro = new ResourceAccessException(
				"I/O error on GET request for \"https://viacep.com.br/ws/01001000/json/\": null",
				new java.net.http.HttpTimeoutException(null));

		assertThat(RastreioJob.causaRaiz(erro)).isEqualTo(
				"HttpTimeoutException: I/O error on GET request for \"https://viacep.com.br/ws/01001000/json/\": null");
	}

	@Test
	void naoMexeNoEstadoQuandoFalhaSemRastreio() {
		when(job.getRetries()).thenReturn(3);
		assertThatThrownBy(() -> rastreio.executar(job, null, () -> {
			throw new IllegalStateException("x");
		}, saida -> null)).isInstanceOf(IllegalStateException.class);
		// o ExecucaoService ignora rastreioId nulo; aqui so garantimos que a excecao sobe
		verify(execucaoService).etapaFalhou(org.mockito.ArgumentMatchers.isNull(), anyString(), anyBoolean());
	}
}
