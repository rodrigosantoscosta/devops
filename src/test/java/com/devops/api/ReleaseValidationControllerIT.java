package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReleaseValidationControllerIT {

	@LocalServerPort
	private int port;

	@Test
	void deveAprovarReleaseValida() throws Exception {
		String json = """
				{
				  "commit": "abc1234",
				  "javaVersion": 21,
				  "unitTestsPassed": true,
				  "integrationTestsPassed": true
				}
				""";
		HttpRequest request = HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/releases/validate"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"approved\":true");
		assertThat(response.body()).contains("\"reasons\":[]");
	}

	@Test
	void deveBloquearReleaseQuandoTesteDeIntegracaoFalhar() throws Exception {
		String json = """
				{
				  "commit": "abc1234",
				  "javaVersion": 21,
				  "unitTestsPassed": true,
				  "integrationTestsPassed": false
				}
				""";
		HttpRequest request = HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/releases/validate"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"approved\":false");
		assertThat(response.body()).contains("Os testes de integracao devem ser aprovados");
	}

	@Test
	void deveBloquearReleaseQuandoCommitForNulo() throws Exception {
		String json = """
				{
				  "commit": null,
				  "javaVersion": 21,
				  "unitTestsPassed": true,
				  "integrationTestsPassed": true
				}
				""";
		HttpRequest request = HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/releases/validate"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"approved\":false");
		assertThat(response.body()).contains("O commit e obrigatorio");
	}

	@Test
	void deveBloquearReleaseQuandoJavaVersionForIncorreta() throws Exception {
		String json = """
				{
				  "commit": "abc1234",
				  "javaVersion": 17,
				  "unitTestsPassed": true,
				  "integrationTestsPassed": true
				}
				""";
		HttpRequest request = HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/releases/validate"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"approved\":false");
		assertThat(response.body()).contains("A versao do Java deve ser 21");
	}

	@Test
	void deveBloquearReleaseQuandoTestesUnitariosFalhar() throws Exception {
		String json = """
				{
				  "commit": "abc1234",
				  "javaVersion": 21,
				  "unitTestsPassed": false,
				  "integrationTestsPassed": true
				}
				""";
		HttpRequest request = HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/releases/validate"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();

		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"approved\":false");
		assertThat(response.body()).contains("Os testes unitarios devem ser aprovados");
	}
}
