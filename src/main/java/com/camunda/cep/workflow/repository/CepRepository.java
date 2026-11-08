package com.camunda.cep.workflow.repository;

import com.camunda.cep.workflow.domain.Cep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CepRepository extends JpaRepository<Cep, String> {
}
