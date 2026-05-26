package com.skillboost.service;

import com.skillboost.model.Exercise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseServiceTest {

    private ExerciseService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ExerciseService();
        service.loadExercises();
    }

    @Test
    void loadsAllExercisesFromClasspath() {
        List<Exercise> exercises = service.list();

        assertThat(exercises).isNotEmpty();
        assertThat(exercises).extracting(Exercise::id)
                .contains("java-sum-array", "java-reverse-string",
                        "java-find-max", "java-factorial", "java-count-vowels");
    }

    @Test
    void listReturnsImmutableCopy() {
        List<Exercise> exercises = service.list();

        assertThat(exercises.getClass().getName())
                .doesNotContain("LinkedHashMap");
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> exercises.add(null));
    }

    @Test
    void findByIdReturnsExerciseWhenPresent() {
        Optional<Exercise> found = service.findById("java-sum-array");

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
    void findByIdReturnsEmptyWhenMissing() {
        assertThat(service.findById("does-not-exist")).isEmpty();
    }
}
