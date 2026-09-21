package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReleaseValidationRequestTest {

	@Test
	void devePreservarOsDadosRecebidos() {
		ReleaseValidationRequest request = new ReleaseValidationRequest("abc1234", 21, true, true);

		assertThat(request.commit()).isEqualTo("abc1234");
		assertThat(request.javaVersion()).isEqualTo(21);
		assertThat(request.unitTestsPassed()).isTrue();
		assertThat(request.integrationTestsPassed()).isTrue();
	}

	@Test
	void deveSerComparavelPorValor() {
		ReleaseValidationRequest primeiro = new ReleaseValidationRequest("abc1234", 21, true, true);
		ReleaseValidationRequest segundo = new ReleaseValidationRequest("abc1234", 21, true, true);

		assertThat(primeiro).isEqualTo(segundo);
		assertThat(primeiro).hasSameHashCodeAs(segundo);
	}

	@Test
	void devePermitirCommitNuloParaValidacaoPosterior() {
		ReleaseValidationRequest request = new ReleaseValidationRequest(null, 21, true, true);

		assertThat(request.commit()).isNull();
		assertThat(request.javaVersion()).isEqualTo(21);
	}
}
