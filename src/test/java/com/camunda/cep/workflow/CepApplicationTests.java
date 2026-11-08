package com.camunda.cep.workflow;

import io.camunda.client.CamundaClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Sobe o contexto Spring sem precisar do Camunda rodando no Docker.
 */
@SpringBootTest(properties = {
		"camunda.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:testdb"
})
class CepApplicationTests {

	@MockitoBean
	CamundaClient camundaClient;

	@Test
	void contextLoads() {
	}

}
