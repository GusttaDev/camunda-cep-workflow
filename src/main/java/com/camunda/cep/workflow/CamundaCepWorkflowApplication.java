package com.camunda.cep.workflow;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ao subir, o camunda-spring-boot-starter faz o deploy do BPMN no Zeebe
 * e registra os metodos anotados com @JobWorker.
 */
@SpringBootApplication
@Deployment(resources = "classpath:bpmn/process-cep.bpmn")
public class CamundaCepWorkflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(CamundaCepWorkflowApplication.class, args);
	}

}
