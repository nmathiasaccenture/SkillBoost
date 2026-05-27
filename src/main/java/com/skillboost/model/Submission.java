package com.skillboost.model;

import jakarta.validation.constraints.NotBlank;

public record Submission(
        @NotBlank String exerciseId,
        @NotBlank String code
) {}
