package com.camunda.cep.workflow.domain.enums;

/**
 * Onde a mensagem esta agora. Junto com etapa_atual, responde "em qual caixinha e fazendo o que".
 */
public enum StatusExecucao {

	/** Esperando um worker pegar a proxima caixinha (ou a primeira, se etapa_atual for nula). */
	AGUARDANDO,

	/** Um worker esta executando etapa_atual. */
	EXECUTANDO,

	/** etapa_atual falhou; o Zeebe ainda vai tentar de novo. Motivo em ultimo_erro. */
	FALHANDO,

	/** etapa_atual falhou sem retries: incident aberto, precisa de Retry no Operate. */
	INCIDENT,

	/** O processo chegou a um evento de fim. */
	CONCLUIDO,

	/** O Zeebe recusou a criacao da instancia (ex.: Camunda fora do ar). Nada rodou. */
	NAO_INICIADO;

	public boolean finalizado() {
		return this == CONCLUIDO || this == NAO_INICIADO;
	}
}
