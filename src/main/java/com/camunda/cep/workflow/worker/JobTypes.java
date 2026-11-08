package com.camunda.cep.workflow.worker;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Tipos de job usados nas service tasks do process-cep.bpmn (zeebe:taskDefinition type="...").
 * Devem ser identicos aos do BPMN: e por esse texto que o Zeebe liga a caixinha ao worker.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JobTypes {

	public static final String BUSCAR_CEP_BASE = "buscar-cep-base";
	public static final String CONSULTAR_VIACEP = "consultar-viacep";
	public static final String SALVAR_CEP = "salvar-cep";
}
