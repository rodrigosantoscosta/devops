package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class StatusControllerTest {

	@Test
	void deveRetornarMensagemFornecidaPeloServico() {
		StatusService service = new StatusService() {
			@Override
			public String message() {
				return "mensagem controlada pelo teste";
			}
		};
		StatusController controller = new StatusController(service);

		StatusResponse response = controller.status();

		assertThat(response.status()).isEqualTo("mensagem controlada pelo teste");
	}

	@Test
	void deveGerarUmTimestampValidoEmFormatoISO8601() {
		StatusController controller = new StatusController(new StatusService());

		StatusResponse response = controller.status();

		assertThatCode(() -> Instant.parse(response.generatedAt())).doesNotThrowAnyException();
	}

	@Test
	void deveGerarTimestampProximoDoMomentoDaConsulta() {
		StatusController controller = new StatusController(new StatusService());
		Instant antes = Instant.now();

		StatusResponse response = controller.status();

		Instant gerado = Instant.parse(response.generatedAt());
		Instant depois = Instant.now();
		assertThat(gerado).isBetween(antes, depois);
	}

}
