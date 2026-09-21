package com.devops.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/releases")
public class ReleaseValidationController {

	private final ReleaseValidationService service;

	public ReleaseValidationController(ReleaseValidationService service) {
		this.service = service;
	}

	@PostMapping("/validate")
	public ReleaseValidationResponse validate(@RequestBody ReleaseValidationRequest request) {
		return service.validate(request);
	}
}
