package com.camunda.cep.workflow.service;

import com.camunda.cep.workflow.domain.Execucao;
import com.camunda.cep.workflow.repository.ExecucaoRepository;
import com.camunda.cep.workflow.domain.enums.ResultadoExecucao;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mantem o estado de cada execucao (tabela cep_execucao). Nao conhece o Camunda.
 *
 * Rastreio e observabilidade, nao regra de negocio: se a gravacao falhar, so loga.
 * Ela nunca derruba o fluxo (senao um problema no rastreio viraria incident no processo).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecucaoService {

	private final ExecucaoRepository repository;
	private final Clock clock;

	public void iniciar(String rastreioId, String cep) {
		protegido(rastreioId, () -> repository.save(new Execucao(rastreioId, cep, agora())));
	}

	public void etapaIniciada(String rastreioId, long processInstanceKey, String etapa) {
		atualizar(rastreioId, e -> e.etapaIniciada(processInstanceKey, etapa, agora()));
	}

	/** @param resultado preenchido quando esta etapa encerra o processo; nulo quando o processo segue. */
	public void etapaConcluida(String rastreioId, ResultadoExecucao resultado) {
		atualizar(rastreioId, e -> e.etapaConcluida(resultado, agora()));
	}

	public void etapaFalhou(String rastreioId, String erro, boolean semRetries) {
		atualizar(rastreioId, e -> e.etapaFalhou(erro, semRetries, agora()));
	}

	public void naoIniciada(String rastreioId, String erro) {
		atualizar(rastreioId, e -> e.naoIniciada(erro, agora()));
	}

	public Optional<Execucao> porId(String rastreioId) {
		return repository.findById(rastreioId);
	}

	public List<Execucao> porCep(String cep) {
		return repository.findTop20ByCepOrderBySolicitadoEmDesc(cep);
	}

	/** Todas as execucoes que ainda nao terminaram, da mais antiga sem movimento para a mais recente. */
	public List<Execucao> pendentes() {
		return repository.findByFinalizadoEmIsNullOrderByAtualizadoEmAsc();
	}

	/** Execucoes nao finalizadas e sem nenhum movimento ha mais de {@code limite}. */
	public List<Execucao> paradasHaMaisDe(Duration limite) {
		return repository.findByFinalizadoEmIsNullAndAtualizadoEmBeforeOrderByAtualizadoEmAsc(agora().minus(limite));
	}

	public Instant agora() {
		return Instant.now(clock);
	}

	private void atualizar(String rastreioId, Consumer<Execucao> mudanca) {
		if (rastreioId == null) {
			return; // instancia criada antes de existir o rastreio
		}
		protegido(rastreioId, () -> repository.findById(rastreioId).ifPresentOrElse(
				e -> {
					mudanca.accept(e);
					repository.save(e);
				},
				() -> log.warn("[rastreio={}] Execucao nao encontrada na cep_execucao", rastreioId)));
	}

	private void protegido(String rastreioId, Runnable gravacao) {
		try {
			gravacao.run();
		}
		catch (RuntimeException e) {
			log.warn("[rastreio={}] Nao foi possivel gravar o estado da execucao: {}", rastreioId, e.getMessage());
		}
	}
}
