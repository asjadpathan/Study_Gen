package com.asjad.studygen.dto.assessment;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitAttemptRequest(
        @NotEmpty(message = "Answers cannot be empty")
        List<Integer> selectedOptions
) {}
