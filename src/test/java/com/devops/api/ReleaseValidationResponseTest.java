package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReleaseValidationResponseTest {

	@Test
	void devePreservarOsDadosRecebidos() {
		ReleaseValidationResponse response = new ReleaseValidationResponse(true, List.of());

		assertThat(response.approved()).isTrue();
		assertThat(response.reasons()).isEmpty();
	}

	@Test
	void deveSerComparavelPorValor() {
		ReleaseValidationResponse primeiro = new ReleaseValidationResponse(false, List.of("motivo"));
		ReleaseValidationResponse segundo = new ReleaseValidationResponse(false, List.of("motivo"));

		assertThat(primeiro).isEqualTo(segundo);
		assertThat(primeiro).hasSameHashCodeAs(segundo);
	}

	@Test
	void devePreservarMotivosDeBloqueio() {
		List<String> motivos = List.of("O commit e obrigatorio", "A versao do Java deve ser 21");
		ReleaseValidationResponse response = new ReleaseValidationResponse(false, motivos);

		assertThat(response.approved()).isFalse();
		assertThat(response.reasons()).containsExactly("O commit e obrigatorio", "A versao do Java deve ser 21");
	}
}
