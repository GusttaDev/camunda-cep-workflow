package com.camunda.cep.workflow.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
class ExecucaoConfig {

	/**
	 * Injetavel para os testes poderem fixar o horario.
	 * Anda de segundo em segundo: as datas gravadas nao tem fracao de segundo.
	 */
	@Bean
	Clock clock() {
		return Clock.tick(Clock.systemUTC(), Duration.ofSeconds(1));
	}
}
