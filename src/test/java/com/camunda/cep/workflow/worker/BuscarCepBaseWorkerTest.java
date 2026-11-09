package com.camunda.cep.workflow.worker;

import static com.camunda.cep.workflow.CepFixtures.PRACA_DA_SE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class BuscarCepBaseWorkerTest {

	@Mock
	CepService cepService;

	@Mock
	ExecucaoService execucaoService;

	@Mock
	ActivatedJob job;

	BuscarCepBaseWorker worker;

	@BeforeEach
	void setUp() {
		worker = new BuscarCepBaseWorker(cepService, new RastreioJob(execucaoService));
	}

	@Test
	void quandoExisteNaBase_retornaEncontradoEEndereco() {
		when(cepService.buscarNaBase("01001000")).thenReturn(Optional.of(PRACA_DA_SE));

		assertThat(worker.executar(job, "01001000", "rastreio-1"))
				.containsEntry("encontradoNaBase", true)
				.containsEntry("endereco", PRACA_DA_SE);
		verify(execucaoService).etapaConcluida("rastreio-1", ResultadoExecucao.BASE);
	}

	@Test
	void quandoNaoExisteNaBase_retornaSomenteFlagFalse() {
		when(cepService.buscarNaBase("01001000")).thenReturn(Optional.empty());

		assertThat(worker.executar(job, "01001000", "rastreio-1"))
				.containsEntry("encontradoNaBase", false)
				.doesNotContainKey("endereco");
		verify(execucaoService).etapaConcluida("rastreio-1", null); // processo segue para o ViaCEP
	}
}
