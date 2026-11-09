package com.camunda.cep.workflow.domain.enums;

/** Por qual evento de fim do process-cep a execucao terminou. */
public enum ResultadoExecucao {
	/** End_CepDaBase */
	BASE,
	/** End_CepSalvo */
	VIACEP,
	/** End_CepNaoEncontrado */
	NAO_ENCONTRADO
}
