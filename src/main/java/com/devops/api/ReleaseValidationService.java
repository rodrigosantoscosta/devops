package com.devops.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ReleaseValidationService {

	public ReleaseValidationResponse validate(ReleaseValidationRequest request) {
		List<String> reasons = new ArrayList<>();

		if (request.commit() == null || request.commit().isBlank()) {
			reasons.add("O commit e obrigatorio");
		}
		if (request.javaVersion() != 21) {
			reasons.add("A versao do Java deve ser 21");
		}
		if (!request.unitTestsPassed()) {
			reasons.add("Os testes unitarios devem ser aprovados");
		}
		if (!request.integrationTestsPassed()) {
			reasons.add("Os testes de integracao devem ser aprovados");
		}

		return new ReleaseValidationResponse(reasons.isEmpty(), List.copyOf(reasons));
	}
}
