package com.camunda.cep.workflow.worker;

import static com.camunda.cep.workflow.CepFixtures.PRACA_DA_SE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camunda.cep.workflow.api.dto.CepResponse;
import com.camunda.cep.workflow.service.ExecucaoService;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import com.camunda.cep.workflow.service.CepService;
import io.camunda.client.api.response.ActivatedJob;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class ConsultarViaCepWorkerTest {

	@Mock
	CepService cepService;

	@Mock
	ExecucaoService execucaoService;

	@Mock
	ActivatedJob job;

	ConsultarViaCepWorker worker;

	@BeforeEach
	void setUp() {
		worker = new ConsultarViaCepWorker(cepService, new RastreioJob(execucaoService));
	}

	@Test
	void quandoViaCepEncontra_retornaEncontradoEEndereco() {
		when(cepService.buscarNoViaCep("01001000")).thenReturn(Optional.of(PRACA_DA_SE));

		assertThat(worker.executar(job, "01001000", "rastreio-1"))
				.containsEntry("encontradoNoViaCep", true)
				.containsEntry("endereco", PRACA_DA_SE);
		verify(execucaoService).etapaConcluida("rastreio-1", null); // processo segue para o Salvar
	}

	@Test
	void quandoViaCepNaoEncontra_retornaEnderecoVazio() {
		when(cepService.buscarNoViaCep("99999999")).thenReturn(Optional.empty());

		assertThat(worker.executar(job, "99999999", "rastreio-1"))
				.containsEntry("encontradoNoViaCep", false)
				.containsEntry("endereco", CepResponse.vazio());
		verify(execucaoService).etapaConcluida("rastreio-1", ResultadoExecucao.NAO_ENCONTRADO);
	}

	@Test
	void quandoViaCepFalha_propagaExcecaoParaOZeebeFazerRetry() {
		when(cepService.buscarNoViaCep("01001000")).thenThrow(new ResourceAccessException("timeout"));

		assertThatThrownBy(() -> worker.executar(job, "01001000", "rastreio-1")).isInstanceOf(ResourceAccessException.class);
		verify(execucaoService).etapaFalhou(eq("rastreio-1"), anyString(), anyBoolean());
	}
}
