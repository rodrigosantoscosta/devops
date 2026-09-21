package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReleaseValidationServiceTest {

	private final ReleaseValidationService service = new ReleaseValidationService();

	@Test
	void deveAprovarReleaseValida() {
		ReleaseValidationRequest request = new ReleaseValidationRequest("abc1234", 21, true, true);

		ReleaseValidationResponse response = service.validate(request);

		assertThat(response.approved()).isTrue();
		assertThat(response.reasons()).isEmpty();
	}

	@Test
	void deveBloquearReleaseSemCommit() {
		ReleaseValidationRequest request = new ReleaseValidationRequest(" ", 21, true, true);

		ReleaseValidationResponse response = service.validate(request);

		assertThat(response.approved()).isFalse();
		assertThat(response.reasons()).containsExactly("O commit e obrigatorio");
	}

	@Test
	void deveBloquearReleaseComVersaoIncorretaDoJava() {
		ReleaseValidationRequest request = new ReleaseValidationRequest("abc1234", 17, true, true);

		ReleaseValidationResponse response = service.validate(request);

		assertThat(response.approved()).isFalse();
		assertThat(response.reasons()).containsExactly("A versao do Java deve ser 21");
	}

	@Test
	void deveBloquearReleaseComTestesUnitariosReprovados() {
		ReleaseValidationRequest request = new ReleaseValidationRequest("abc1234", 21, false, true);

		ReleaseValidationResponse response = service.validate(request);

		assertThat(response.approved()).isFalse();
		assertThat(response.reasons()).containsExactly("Os testes unitarios devem ser aprovados");
	}

	@Test
	void deveBloquearReleaseComTestesDeIntegracaoReprovados() {
		ReleaseValidationRequest request = new ReleaseValidationRequest("abc1234", 21, true, false);

		ReleaseValidationResponse response = service.validate(request);

		assertThat(response.approved()).isFalse();
		assertThat(response.reasons()).containsExactly("Os testes de integracao devem ser aprovados");
	}

	@Test
	void deveInformarTodosOsMotivosDeBloqueio() {
		ReleaseValidationRequest request = new ReleaseValidationRequest(null, 17, false, false);

		ReleaseValidationResponse response = service.validate(request);

		assertThat(response.approved()).isFalse();
		assertThat(response.reasons()).containsExactly(
				"O commit e obrigatorio",
				"A versao do Java deve ser 21",
				"Os testes unitarios devem ser aprovados",
				"Os testes de integracao devem ser aprovados");
	}
}
