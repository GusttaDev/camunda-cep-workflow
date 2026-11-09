package com.camunda.cep.workflow.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.camunda.cep.workflow.domain.Execucao;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import com.camunda.cep.workflow.domain.enums.StatusExecucao;
import com.camunda.cep.workflow.service.ExecucaoService;
import io.camunda.client.CamundaClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Contra o H2 de verdade: valida o schema gerado (colunas nulas, enums) e as consultas.
 */
@SpringBootTest(properties = {
		"camunda.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:execucao"
})
class ExecucaoServiceIntegrationTest {

	@MockitoBean
	CamundaClient camundaClient;

	@Autowired
    ExecucaoService service;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void fluxoCompletoEConsultas() {
		String parada = UUID.randomUUID().toString();
		String concluida = UUID.randomUUID().toString();

		service.iniciar(parada, "01001000");
		service.etapaIniciada(parada, 1L, "Task_ConsultarViaCep");
		service.etapaFalhou(parada, "ConnectException: Connection refused", true);

		service.iniciar(concluida, "01001000");
		service.etapaIniciada(concluida, 2L, "Task_BuscarCepBase");
		service.etapaConcluida(concluida, ResultadoExecucao.BASE);

		assertThat(service.porId(parada)).hasValueSatisfying(e -> {
			assertThat(e.getStatus()).isEqualTo(StatusExecucao.INCIDENT);
			assertThat(e.getEtapaAtual()).isEqualTo("Task_ConsultarViaCep");
			assertThat(e.getUltimoErro()).contains("Connection refused");
		});
		assertThat(service.pendentes()).extracting(Execucao::getRastreioId)
				.contains(parada).doesNotContain(concluida);
		assertThat(service.paradasHaMaisDe(Duration.ofMillis(-1))).extracting(Execucao::getRastreioId)
				.contains(parada).doesNotContain(concluida);
		assertThat(service.paradasHaMaisDe(Duration.ofHours(1))).isEmpty();
		assertThat(service.porCep("01001000")).hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	void rastreioDesconhecidoOuNulo_naoQuebra() {
		service.etapaIniciada(null, 1L, "x");
		service.etapaIniciada("nao-existe", 1L, "x");
		assertThat(service.porId("nao-existe")).isEmpty();
	}

	@Test
	void datasGravadasSemFusoNoHorarioDeBrasilia() {
		String id = UUID.randomUUID().toString();
		service.iniciar(id, "01001000");
		Execucao execucao = service.porId(id).orElseThrow();

		String tipo = jdbc.queryForObject("SELECT DATA_TYPE || '(' || DATETIME_PRECISION || ')' FROM INFORMATION_SCHEMA.COLUMNS"
				+ " WHERE TABLE_NAME = 'CEP_EXECUCAO' AND COLUMN_NAME = 'SOLICITADO_EM'", String.class);
		LocalDateTime gravado = jdbc.queryForObject(
				"SELECT solicitado_em FROM cep_execucao WHERE rastreio_id = ?", LocalDateTime.class, id);

		assertThat(tipo).isEqualTo("TIMESTAMP(0)");
		assertThat(gravado.getNano()).isZero();
		assertThat(gravado).isEqualTo(LocalDateTime.ofInstant(execucao.getSolicitadoEm(), ZoneId.of("America/Sao_Paulo")));
	}
}
