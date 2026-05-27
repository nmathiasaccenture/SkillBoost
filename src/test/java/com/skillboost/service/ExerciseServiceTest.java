package com.skillboost.service;

import com.skillboost.model.Exercise;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseServiceTest {

    private final JsonExerciseLoader loader = new JsonExerciseLoader();

    @Test
    void loadsAllExercisesFromClasspath() throws Exception {
        List<Exercise> exercises = loader.loadAll();

        assertThat(exercises).isNotEmpty();
        assertThat(exercises).extracting(Exercise::id)
                .contains("java-sum-array", "java-reverse-string",
                        "java-find-max", "java-factorial", "java-count-vowels");
    }

    @Test
    void findByIdReturnsExerciseWhenPresent() throws Exception {
        Optional<Exercise> found = loader.loadAll().stream()
                .filter(e -> "java-sum-array".equals(e.id()))
                .findFirst();

        assertThat(found).isPresent();
        Exercise ex = found.get();
        assertThat(ex.id()).isEqualTo("java-sum-array");
        assertThat(ex.language()).isEqualTo("java");
        assertThat(ex.title()).isNotBlank();
        assertThat(ex.buggyCode()).isNotBlank();
        assertThat(ex.solutionCode()).isNotBlank();
        assertThat(ex.testHarness()).isNotBlank();
        assertThat(ex.tests()).isNotEmpty();
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() throws Exception {
        Optional<Exercise> found = loader.loadAll().stream()
                .filter(e -> "does-not-exist".equals(e.id()))
                .findFirst();

        assertThat(found).isEmpty();
    }
}
