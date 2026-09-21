package com.devops.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

	private final StatusService statusService;

	public StatusController(StatusService statusService) {
		this.statusService = statusService;
	}

	@GetMapping("/status")
	public StatusResponse status() {
		return new StatusResponse(statusService.message(), Instant.now().toString());
	}
}
