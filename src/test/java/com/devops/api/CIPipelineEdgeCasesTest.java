package com.devops.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Edge cases dos 9 requisitos da missão (README.md:22-31) e
 * 6 provocações (README.md:99-105) que ainda não tinham cobertura
 * direta em CIPipelineCasesTest.
 */
class CIPipelineEdgeCasesTest {

	private final ReleaseValidationService service = new ReleaseValidationService();

	@Nested
	@DisplayName("Commit edge cases - README.md:14,29 rastreabilidade")
	class CommitEdgeCases {

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = {" ", "  ", "\t", "\n"})
		void commitVazioOuBlankDeveSerRejeitado(String commit) {
			ReleaseValidationRequest req = new ReleaseValidationRequest(commit, 21, true, true);
			ReleaseValidationResponse res = service.validate(req);
			assertThat(res.approved()).isFalse();
			assertThat(res.reasons()).contains("O commit e obrigatorio");
		}

		@Test
		void commitComEspacosNasPontasDeveSerAceito() {
			// INTENCIONALMENTE QUEBRADO PARA DEMONSTRAR PIPELINE VERMELHA NO GITHUB ACTIONS
			// isBlank() rejeita " abc1234 " como valido, mas expectativa errada força falha em Unit Tests
			ReleaseValidationRequest req = new ReleaseValidationRequest(" abc1234 ", 21, true, true);
			ReleaseValidationResponse res = service.validate(req);
			assertThat(res.approved()).isFalse();
			assertThat(res.reasons()).contains("O commit e obrigatorio");
		}
	}

	@Nested
	@DisplayName("Imutabilidade do artefato - List.copyOf em ReleaseValidationService.java:27")
	class ImmutabilityCases {

		@Test
		void reasonsDeveSerImutavel() {
			ReleaseValidationRequest req = new ReleaseValidationRequest(null, 17, false, false);
			ReleaseValidationResponse res = service.validate(req);
			assertThatThrownBy(() -> res.reasons().add("injetado"))
					.isInstanceOf(UnsupportedOperationException.class);
		}

		@Test
		void reasonsVazioDeveSerImutavel() {
			ReleaseValidationRequest req = new ReleaseValidationRequest("abc1234", 21, true, true);
			ReleaseValidationResponse res = service.validate(req);
			assertThatThrownBy(() -> res.reasons().add("injetado"))
					.isInstanceOf(UnsupportedOperationException.class);
		}
	}

	@Nested
	@DisplayName("Contrato do endpoint /status - GET /status:29")
	class StatusContractCases {

		@Test
		void statusResponseDeveTerStatusEGeneratedAtNaoNulos() {
			StatusController controller = new StatusController(new StatusService());
			StatusResponse res = controller.status();
			assertThat(res.status()).isNotBlank();
			assertThat(res.generatedAt()).isNotBlank();
		}

		@Test
		void generatedAtDeveSerISO8601Parseavel() {
			StatusController controller = new StatusController(new StatusService());
			StatusResponse res = controller.status();
			assertThat(Instant.parse(res.generatedAt())).isNotNull();
		}

		@Test
		void reasonsOrdenadosComoNoCodigo() {
			ReleaseValidationRequest req = new ReleaseValidationRequest(null, 17, false, false);
			List<String> reasons = service.validate(req).reasons();
			assertThat(reasons).containsExactly(
					"O commit e obrigatorio",
					"A versao do Java deve ser 21",
					"Os testes unitarios devem ser aprovados",
					"Os testes de integracao devem ser aprovados");
		}
	}
}
