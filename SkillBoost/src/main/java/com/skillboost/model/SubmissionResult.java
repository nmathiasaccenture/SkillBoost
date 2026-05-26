package com.skillboost.model;

import java.util.List;

public record SubmissionResult(
        boolean compiled,
        String compileError,
        boolean allPassed,
        List<TestResult> results
) {
    public record TestResult(
            String input,
            String expected,
            String actual,
            boolean passed,
            String error
    ) {}
}
