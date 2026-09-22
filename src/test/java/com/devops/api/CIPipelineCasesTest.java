package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Casos de testes que espelham as verificacoes da pipeline CI
 * (.github/workflows/ci.yaml):
 * - compile bloqueante
 * - unit (Surefire) vs integration (Failsafe)
 * - javaVersion 21 obrigatoria
 * - commit obrigatorio
 * - unit/integration devem passar antes de package
 */
class CIPipelineCasesTest {

	private final ReleaseValidationService service = new ReleaseValidationService();
	private final StatusService statusService = new StatusService();

	@Nested
	@DisplayName("Caso 1: compile e mensagem de status - porta de entrada bloqueante")
	class StatusCases {

		@Test
		void deveRetornarMensagemCompilavel() {
			assertThat(statusService.message())
					.isEqualTo("API Java 21 pronta para CI");
		}

		@Test
		void mensagemNaoDeveSerVazia() {
			assertThat(statusService.message()).isNotBlank();
		}
	}

	@Nested
	@DisplayName("Caso 2: validacao de release - cenarios da pipeline")
	class ReleaseCases {

		@Test
		void pipelineVerde_quandoTudoAprovado() {
			ReleaseValidationRequest req = new ReleaseValidationRequest("abc1234", 21, true, true);
			ReleaseValidationResponse res = service.validate(req);

			assertThat(res.approved()).isTrue();
			assertThat(res.reasons()).isEmpty();
		}

		@Test
		void pipelineVermelha_quandoUnitFalha() {
			ReleaseValidationRequest req = new ReleaseValidationRequest("abc1234", 21, false, true);
			ReleaseValidationResponse res = service.validate(req);

			assertThat(res.approved()).isFalse();
			assertThat(res.reasons()).contains("Os testes unitarios devem ser aprovados");
		}

		@Test
		void pipelineVermelha_quandoIntegrationFalha() {
			ReleaseValidationRequest req = new ReleaseValidationRequest("abc1234", 21, true, false);
			ReleaseValidationResponse res = service.validate(req);

			assertThat(res.approved()).isFalse();
			assertThat(res.reasons()).contains("Os testes de integracao devem ser aprovados");
		}

		@Test
		void pipelineVermelha_quandoCommitAusente() {
			ReleaseValidationRequest req = new ReleaseValidationRequest(" ", 21, true, true);
			ReleaseValidationResponse res = service.validate(req);

			assertThat(res.approved()).isFalse();
			assertThat(res.reasons()).contains("O commit e obrigatorio");
		}

		@ParameterizedTest
		@ValueSource(ints = {8, 11, 17, 22, 23})
		void pipelineVermelha_quandoJavaVersionDiferenteDe21(int version) {
			ReleaseValidationRequest req = new ReleaseValidationRequest("abc1234", version, true, true);
			ReleaseValidationResponse res = service.validate(req);

			assertThat(res.approved()).isFalse();
			assertThat(res.reasons()).contains("A versao do Java deve ser 21");
		}

		@Test
		void pipelineVermelha_quandoTodosFalham() {
			ReleaseValidationRequest req = new ReleaseValidationRequest(null, 17, false, false);
			ReleaseValidationResponse res = service.validate(req);

			assertThat(res.approved()).isFalse();
			assertThat(res.reasons()).hasSize(4);
		}
	}

	@Nested
	@DisplayName("Caso 3: rastreabilidade - commit deve acompanhar artefato")
	class TraceabilityCases {

		@Test
		void commitNuloDeveSerRejeitado() {
			ReleaseValidationRequest req = new ReleaseValidationRequest(null, 21, true, true);
			assertThat(service.validate(req).approved()).isFalse();
		}

		@Test
		void commitValidoDeveSerAceito() {
			ReleaseValidationRequest req = new ReleaseValidationRequest("abc1234", 21, true, true);
			assertThat(service.validate(req).approved()).isTrue();
		}
	}
}
