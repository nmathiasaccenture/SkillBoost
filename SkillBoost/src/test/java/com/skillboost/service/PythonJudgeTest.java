package com.skillboost.service;

import com.skillboost.model.Exercise;
import com.skillboost.model.SubmissionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PythonJudgeTest {

    private PythonJudge judge;

    @BeforeEach
    void setUp() {
        judge = new PythonJudge();
        ReflectionTestUtils.setField(judge, "timeoutSeconds", 15);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pythonExercises")
    void solutionCodePassesEveryExercise(Exercise exercise) throws Exception {
        SubmissionResult result = judge.run(exercise, exercise.solutionCode());

        assertThat(result.compiled())
                .as("solution should not produce a compile/load error for %s", exercise.id())
                .isTrue();
        assertThat(result.allPassed())
                .as("solution should pass all tests for %s", exercise.id())
                .isTrue();
        assertThat(result.results())
                .as("solution should produce at least one test result for %s", exercise.id())
                .isNotEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pythonExercises")
    void buggyCodeFailsAtLeastOneTestForEveryExercise(Exercise exercise) throws Exception {
        SubmissionResult result = judge.run(exercise, exercise.buggyCode());

        assertThat(result.allPassed())
                .as("buggy code should fail at least one test for %s", exercise.id())
                .isFalse();
    }

    static Stream<Arguments> pythonExercises() throws Exception {
        ExerciseService service = new ExerciseService();
        service.loadExercises();
        return service.list().stream()
                .filter(ex -> "python".equals(ex.language()))
                .map(ex -> Arguments.of(Named.of(ex.id(), ex)));
    }
}
