package com.devops.api;

import java.util.List;

public record ReleaseValidationResponse(boolean approved, List<String> reasons) {
}
