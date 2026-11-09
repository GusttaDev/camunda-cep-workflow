package com.camunda.cep.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cep")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // exigido pelo JPA
public class Cep {

	/** CEP normalizado: somente os 8 digitos. */
	@Id
	private String cep;
	private String logradouro;
	private String complemento;
	private String bairro;
	private String localidade;
	private String uf;
	private String ibge;
	private String ddd;

	// ---- auditoria (colunas aceitam nulo: registros antigos nao tem esses dados) ----

	/** Execucao que gravou o registro. Quando foi pedido, por onde passou etc.: veja cep_execucao. */
	private String rastreioId;

	/** Quando o registro foi gravado (ou regravado) na base. */
	@Column(secondPrecision = 0)
	private Instant salvoEm;

	public Cep(String cep, String logradouro, String complemento, String bairro, String localidade, String uf,
			String ibge, String ddd) {
		this.cep = cep;
		this.logradouro = logradouro;
		this.complemento = complemento;
		this.bairro = bairro;
		this.localidade = localidade;
		this.uf = uf;
		this.ibge = ibge;
		this.ddd = ddd;
	}

	public void registrarGravacao(String rastreioId, Instant salvoEm) {
		this.rastreioId = rastreioId;
		this.salvoEm = salvoEm;
	}
}
