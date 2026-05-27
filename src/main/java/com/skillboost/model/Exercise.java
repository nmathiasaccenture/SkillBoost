package com.skillboost.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public record Exercise(
        String id,
        String language,
        int difficulty,
        String title,
        String description,
        String hint,
        String buggyCode,
        String solutionCode,
        String testHarness,
        List<TestCase> tests
) {
    public record TestCase(String input, String expected) {}

    @JsonIgnore
    public Exercise toPublicView() {
        return new Exercise(id, language, difficulty, title, description,
                hint, buggyCode, null, null, tests);
    }
}
