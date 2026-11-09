package com.camunda.cep.workflow.domain;

import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import com.camunda.cep.workflow.domain.enums.StatusExecucao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Uma linha por requisicao ao endpoint, atualizada conforme a mensagem anda pelo processo.
 * Guarda o ESTADO ATUAL, nao o historico: o historico detalhado ja existe no Operate
 * (pelo processInstanceKey) e nos logs (pelo rastreioId).
 *
 * Todas as colunas aceitam nulo: o ddl-auto=update nao consegue adicionar coluna NOT NULL
 * em tabela que ja tem linhas.
 */
@Entity
@Table(name = "cep_execucao", indexes = {
		@Index(name = "idx_execucao_cep", columnList = "cep"),
		@Index(name = "idx_execucao_pendente", columnList = "finalizadoEm, atualizadoEm")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // exigido pelo JPA
public class Execucao {

	static final int TAMANHO_ERRO = 500;

	@Id
	private String rastreioId;

	private String cep;

	/** Para abrir a instancia no Operate. Preenchido quando o primeiro worker pega um job. */
	private Long processInstanceKey;

	@Enumerated(EnumType.STRING)
	private StatusExecucao status;

	/** Id da caixinha no BPMN (ex.: Task_ConsultarViaCep). */
	private String etapaAtual;

	/** Quantas vezes etapa_atual foi tentada (volta para 1 quando muda de caixinha). */
	private Integer tentativas;

	/**
	 * Causa raiz da ultima falha, curta. NAO e apagada quando a execucao se recupera:
	 * depois que tudo voltou a funcionar, ainda da para saber por que ficou parada.
	 */
	@Column(length = TAMANHO_ERRO)
	private String ultimoErro;

	@Enumerated(EnumType.STRING)
	private ResultadoExecucao resultado;

	/** Quando o endpoint recebeu a requisicao. */
	@Column(secondPrecision = 0)
	private Instant solicitadoEm;

	/** Ultimo movimento. Muito antigo + nao finalizado = mensagem parada. */
	@Column(secondPrecision = 0)
	private Instant atualizadoEm;

	@Column(secondPrecision = 0)
	private Instant finalizadoEm;

	Execucao(String rastreioId, String cep, Instant agora) {
		this.rastreioId = rastreioId;
		this.cep = cep;
		this.status = StatusExecucao.AGUARDANDO;
		this.solicitadoEm = agora;
		this.atualizadoEm = agora;
	}

	void etapaIniciada(long processInstanceKey, String etapa, Instant agora) {
		this.processInstanceKey = processInstanceKey;
		this.tentativas = etapa.equals(this.etapaAtual) && this.tentativas != null ? this.tentativas + 1 : 1;
		this.etapaAtual = etapa;
		this.status = StatusExecucao.EXECUTANDO;
		this.atualizadoEm = agora;
		this.finalizadoEm = null; // se uma caixinha comecou, a execucao nao esta finalizada
	}

	void etapaConcluida(ResultadoExecucao resultado, Instant agora) {
		this.atualizadoEm = agora;
		if (resultado == null) {
			this.status = StatusExecucao.AGUARDANDO;
			return;
		}
		this.status = StatusExecucao.CONCLUIDO;
		this.resultado = resultado;
		this.finalizadoEm = agora;
	}

	void etapaFalhou(String erro, boolean semRetries, Instant agora) {
		this.status = semRetries ? StatusExecucao.INCIDENT : StatusExecucao.FALHANDO;
		this.ultimoErro = cortar(erro);
		this.atualizadoEm = agora;
	}

	void naoIniciada(String erro, Instant agora) {
		this.status = StatusExecucao.NAO_INICIADO;
		this.ultimoErro = cortar(erro);
		this.atualizadoEm = agora;
		this.finalizadoEm = agora;
	}

	private static String cortar(String texto) {
		return texto != null && texto.length() > TAMANHO_ERRO ? texto.substring(0, TAMANHO_ERRO) : texto;
	}
}
