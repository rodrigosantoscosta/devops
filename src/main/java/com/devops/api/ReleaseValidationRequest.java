package com.devops.api;

public record ReleaseValidationRequest(
		String commit,
		int javaVersion,
		boolean unitTestsPassed,
		boolean integrationTestsPassed) {
}
