package com.devops.api;

import org.springframework.stereotype.Service;

@Service
public class StatusService {

	public String message() {
		return "API Java 21 pronta para CI";
	}
}
