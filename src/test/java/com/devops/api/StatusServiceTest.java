package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatusServiceTest {

	@Test
	void deveRetornarMensagemDeStatus() {
		StatusService service = new StatusService();

		assertThat(service.message()).isEqualTo("mensagem errada para demonstrar CI vermelha");
	}
}
