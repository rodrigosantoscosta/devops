package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatusResponseTest {

	@Test
	void devePreservarOsDadosRecebidos() {
		StatusResponse response = new StatusResponse("ok", "2026-09-21T12:00:00Z");

		assertThat(response.status()).isEqualTo("ok");
		assertThat(response.generatedAt()).isEqualTo("2026-09-21T12:00:00Z");
	}

	@Test
	void deveSerComparavelPorValor() {
		StatusResponse primeiro = new StatusResponse("ok", "2026-09-21T12:00:00Z");
		StatusResponse segundo = new StatusResponse("ok", "2026-09-21T12:00:00Z");

		assertThat(primeiro).isEqualTo(segundo);
		assertThat(primeiro).hasSameHashCodeAs(segundo);
	}
}
