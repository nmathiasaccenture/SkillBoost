package com.skillboost.service;

import com.skillboost.model.Exercise;
import com.skillboost.model.SubmissionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JavaJudgeTest {

    private JavaJudge judge;
    private Exercise sumArray;

    @BeforeEach
    void setUp() throws Exception {
        judge = new JavaJudge();
        ReflectionTestUtils.setField(judge, "timeoutSeconds", 15);

        ExerciseService exercises = new ExerciseService();
        exercises.loadExercises();
        Optional<Exercise> ex = exercises.findById("java-sum-array");
        assertThat(ex).isPresent();
        sumArray = ex.get();
    }

    @Test
    void runReportsAllTestsPassingForCorrectSolution() throws Exception {
        SubmissionResult result = judge.run(sumArray, sumArray.solutionCode());

        assertThat(result.compiled()).isTrue();
        assertThat(result.compileError()).isNull();
        assertThat(result.allPassed()).isTrue();
        assertThat(result.results()).isNotEmpty();
        assertThat(result.results()).allSatisfy(tr -> assertThat(tr.passed()).isTrue());
    }

    @Test
    void runReportsFailuresForBuggyCode() throws Exception {
        SubmissionResult result = judge.run(sumArray, sumArray.buggyCode());

        assertThat(result.compiled()).isTrue();
        assertThat(result.compileError()).isNull();
        assertThat(result.allPassed()).isFalse();
        assertThat(result.results()).anySatisfy(tr -> {
            assertThat(tr.passed()).isFalse();
            assertThat(tr.expected()).isNotBlank();
            assertThat(tr.actual()).isNotBlank();
        });
    }

    @Test
    void runReportsCompileErrorForBrokenCode() throws Exception {
        String broken = "public class Solution { public static int sum(int[] nums) { return ; } }";

        SubmissionResult result = judge.run(sumArray, broken);

        assertThat(result.compiled()).isFalse();
        assertThat(result.compileError()).isNotBlank();
        assertThat(result.allPassed()).isFalse();
        assertThat(result.results()).isEmpty();
    }

    @Test
    void runCapturesRuntimeErrorsAsFailedTests() throws Exception {
        String throwsCode = """
                public class Solution {
                    public static int sum(int[] nums) {
                        throw new RuntimeException("boom");
                    }
                }
                """;

        SubmissionResult result = judge.run(sumArray, throwsCode);

        assertThat(result.compiled()).isTrue();
        assertThat(result.allPassed()).isFalse();
        assertThat(result.results()).isNotEmpty();
        assertThat(result.results()).allSatisfy(tr -> assertThat(tr.passed()).isFalse());
        assertThat(result.results()).anySatisfy(tr ->
                assertThat(tr.error()).contains("RuntimeException"));
    }

    @Test
    void runTimesOutWhenExecutionExceedsLimit() throws Exception {
        ReflectionTestUtils.setField(judge, "timeoutSeconds", 1);

        String slowCode = """
                public class Solution {
                    public static int sum(int[] nums) {
                        while (true) { /* spin */ }
                    }
                }
                """;

        SubmissionResult result = judge.run(sumArray, slowCode);

        assertThat(result.compiled()).isTrue();
        assertThat(result.allPassed()).isFalse();
        assertThat(result.results()).isNotEmpty();
        List<SubmissionResult.TestResult> errors = result.results().stream()
                .filter(tr -> tr.error() != null && tr.error().contains("timed out"))
                .toList();
        assertThat(errors).isNotEmpty();
    }
}
