package com.camunda.cep.workflow.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.camunda.cep.workflow.domain.Execucao;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import com.camunda.cep.workflow.domain.enums.StatusExecucao;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Transicoes de estado de uma execucao, no cenario "ViaCEP caiu, alguem deu Retry, voltou". */
class ExecucaoTest {

	static final Instant T0 = Instant.parse("2026-11-08T12:00:00Z");

	@Test
	void falhaIncidentERecuperacao() {
		Execucao e = new Execucao("r1", "01001000", T0);
		assertThat(e.getStatus()).isEqualTo(StatusExecucao.AGUARDANDO);
		assertThat(e.getEtapaAtual()).isNull();

		e.etapaIniciada(123L, "Task_BuscarCepBase", T0.plusMillis(10));
		e.etapaConcluida(null, T0.plusMillis(20));
		assertThat(e.getStatus()).isEqualTo(StatusExecucao.AGUARDANDO);
		assertThat(e.getTentativas()).isEqualTo(1);

		e.etapaIniciada(123L, "Task_ConsultarViaCep", T0.plusSeconds(1));
		e.etapaFalhou("ConnectException: Connection refused", false, T0.plusSeconds(2));
		assertThat(e.getStatus()).isEqualTo(StatusExecucao.FALHANDO);

		e.etapaIniciada(123L, "Task_ConsultarViaCep", T0.plusSeconds(3));
		e.etapaIniciada(123L, "Task_ConsultarViaCep", T0.plusSeconds(5));
		e.etapaFalhou("ConnectException: Connection refused", true, T0.plusSeconds(6));
		assertThat(e.getStatus()).isEqualTo(StatusExecucao.INCIDENT);
		assertThat(e.getTentativas()).isEqualTo(3);
		assertThat(e.getEtapaAtual()).isEqualTo("Task_ConsultarViaCep");
		assertThat(e.getFinalizadoEm()).isNull();

		// 30 min depois: Retry no Operate
		e.etapaIniciada(123L, "Task_ConsultarViaCep", T0.plusSeconds(1800));
		e.etapaConcluida(null, T0.plusSeconds(1801));
		e.etapaIniciada(123L, "Task_SalvarCep", T0.plusSeconds(1802));
		e.etapaConcluida(ResultadoExecucao.VIACEP, T0.plusSeconds(1803));

		assertThat(e.getStatus()).isEqualTo(StatusExecucao.CONCLUIDO);
		assertThat(e.getResultado()).isEqualTo(ResultadoExecucao.VIACEP);
		assertThat(e.getTentativas()).isEqualTo(1);
		assertThat(e.getFinalizadoEm()).isEqualTo(T0.plusSeconds(1803));
		// o motivo da parada continua la depois da recuperacao
		assertThat(e.getUltimoErro()).isEqualTo("ConnectException: Connection refused");
	}

	@Test
	void erroLongoECortado() {
		Execucao e = new Execucao("r1", "01001000", T0);
		e.etapaFalhou("x".repeat(5000), false, T0);
		assertThat(e.getUltimoErro()).hasSize(Execucao.TAMANHO_ERRO);
	}
}
