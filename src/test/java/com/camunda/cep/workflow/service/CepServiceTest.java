package com.camunda.cep.workflow.service;

import static com.camunda.cep.workflow.CepFixtures.PRACA_DA_SE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camunda.cep.workflow.client.ViaCepClient;
import com.camunda.cep.workflow.domain.Cep;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.camunda.cep.workflow.repository.CepRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CepServiceTest {

	@Mock
	CepRepository repository;

	@Mock
	ViaCepClient viaCepClient;

	static final Instant AGORA = Instant.parse("2026-11-08T12:00:00Z");

	CepService service;

	@BeforeEach
	void setUp() {
		service = new CepService(repository, viaCepClient, Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	@Captor
	ArgumentCaptor<Cep> cepCaptor;

	@Test
	void buscarNaBase_converteEntidadeEmResponse() {
		when(repository.findById("01001000")).thenReturn(Optional.of(PRACA_DA_SE.toEntity()));

		assertThat(service.buscarNaBase("01001000")).contains(PRACA_DA_SE);
	}

	@Test
	void buscarNaBase_quandoNaoExiste_retornaVazio() {
		when(repository.findById("01001000")).thenReturn(Optional.empty());

		assertThat(service.buscarNaBase("01001000")).isEmpty();
	}

	@Test
	void buscarNoViaCep_delegaParaOClient() {
		when(viaCepClient.buscar("01001000")).thenReturn(Optional.of(PRACA_DA_SE));

		assertThat(service.buscarNoViaCep("01001000")).contains(PRACA_DA_SE);
	}

	@Test
	void salvar_persisteEntidade() {
		service.salvar(PRACA_DA_SE, "rastreio-1");

		verify(repository).save(cepCaptor.capture());
		Cep salvo = cepCaptor.getValue();
		assertThat(salvo.getCep()).isEqualTo("01001000");
		assertThat(salvo.getLogradouro()).isEqualTo("Praça da Sé");
		assertThat(salvo.getRastreioId()).isEqualTo("rastreio-1");
		assertThat(salvo.getSalvoEm()).isEqualTo(AGORA);
	}
}
