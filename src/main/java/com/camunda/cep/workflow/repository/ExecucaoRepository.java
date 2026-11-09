package com.camunda.cep.workflow.repository;

import com.camunda.cep.workflow.domain.Execucao;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecucaoRepository extends JpaRepository<Execucao, String> {

	List<Execucao> findTop20ByCepOrderBySolicitadoEmDesc(String cep);

	List<Execucao> findByFinalizadoEmIsNullOrderByAtualizadoEmAsc();

	List<Execucao> findByFinalizadoEmIsNullAndAtualizadoEmBeforeOrderByAtualizadoEmAsc(Instant limite);
}
