package com.camunda.cep.workflow.worker;

import static com.camunda.cep.workflow.CepFixtures.PRACA_DA_SE;
import static org.mockito.Mockito.verify;

import com.camunda.cep.workflow.service.ExecucaoService;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import com.camunda.cep.workflow.service.CepService;
import io.camunda.client.api.response.ActivatedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalvarCepWorkerTest {

	@Mock
	CepService cepService;

	@Mock
	ExecucaoService execucaoService;

	@Mock
	ActivatedJob job;

	SalvarCepWorker worker;

	@BeforeEach
	void setUp() {
		worker = new SalvarCepWorker(cepService, new RastreioJob(execucaoService));
	}

	@Test
	void delegaParaOServicoEFinalizaComoViaCep() {
		worker.executar(job, PRACA_DA_SE, "rastreio-1");

		verify(cepService).salvar(PRACA_DA_SE, "rastreio-1");
		verify(execucaoService).etapaConcluida("rastreio-1", ResultadoExecucao.VIACEP);
	}
}
