package com.camunda.cep.workflow.service;

import com.camunda.cep.workflow.api.dto.CepResponse;
import com.camunda.cep.workflow.client.ViaCepClient;
import com.camunda.cep.workflow.domain.Cep;
import com.camunda.cep.workflow.repository.CepRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Regras de negocio do CEP. Nao conhece o Camunda: os workers apenas delegam para ca.
 */
@Service
@RequiredArgsConstructor
public class CepService {

	private final CepRepository repository;
	private final ViaCepClient viaCepClient;
	private final Clock clock;

	public Optional<CepResponse> buscarNaBase(String cep) {
		return repository.findById(cep).map(CepResponse::from);
	}

	public Optional<CepResponse> buscarNoViaCep(String cep) {
		return viaCepClient.buscar(cep);
	}

	/**
	 * Idempotente: o CEP e a chave, salvar duas vezes so sobrescreve o mesmo registro.
	 * Grava junto quando foi salvo e por qual execucao (detalhes em cep_execucao).
	 */
	public void salvar(CepResponse endereco, String rastreioId) {
		Cep entidade = endereco.toEntity();
		entidade.registrarGravacao(rastreioId, Instant.now(clock));
		repository.save(entidade);
	}
}
