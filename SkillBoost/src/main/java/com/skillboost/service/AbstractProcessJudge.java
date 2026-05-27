package com.skillboost.service;

import com.skillboost.model.Exercise;
import com.skillboost.model.SubmissionResult;
import com.skillboost.model.SubmissionResult.TestResult;
import com.skillboost.service.sandbox.NoSandbox;
import com.skillboost.service.sandbox.Sandbox;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class AbstractProcessJudge {

    @Value("${skillboost.judge.timeout-seconds:5}")
    protected int timeoutSeconds;

    protected final Sandbox sandbox;

    protected AbstractProcessJudge(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    protected AbstractProcessJudge() {
        this(new NoSandbox("no Spring context"));
    }

    protected abstract String solutionFilename();

    protected abstract String runnerFilename();

    protected abstract List<String> executeCommand();

    /** Optional compile step. Return {@code null} on success, or compiler output on failure. */
    protected String compile(Path workDir) throws IOException {
        return null;
    }

    public SubmissionResult run(Exercise exercise, String userCode) throws IOException {
        Path workDir = Files.createTempDirectory("skillboost-");
        try {
            Files.writeString(workDir.resolve(solutionFilename()), userCode);
            Files.writeString(workDir.resolve(runnerFilename()), exercise.testHarness());

            String compileError = compile(workDir);
            if (compileError != null) {
                return new SubmissionResult(false, compileError, false, List.of());
            }

            List<TestResult> results = execute(workDir);
            boolean allPassed = !results.isEmpty() && results.stream().allMatch(TestResult::passed);
            return new SubmissionResult(true, null, allPassed, results);
        } finally {
            deleteRecursively(workDir);
        }
    }

    /**
     * Run a process whose stderr is merged into stdout. Returns {@code null} on success (exit 0),
     * or a user-facing error string on failure / timeout / interrupt.
     */
    protected String runProcessMerged(Path workDir, List<String> command, String label) throws IOException {
        Process p = new ProcessBuilder(sandbox.wrap(command, workDir))
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();
        String output;
        try {
            output = new String(p.getInputStream().readAllBytes());
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return label + " timed out";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return label + " interrupted";
        }
        return p.exitValue() == 0 ? null : output;
    }

    private List<TestResult> execute(Path workDir) throws IOException {
        Process p = new ProcessBuilder(sandbox.wrap(executeCommand(), workDir))
                .directory(workDir.toFile())
                .redirectErrorStream(false)
                .start();

        String stdout;
        String stderr;
        try {
            boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return List.of(new TestResult("(execution)", "", "", false,
                        "Execution timed out after " + timeoutSeconds + "s"));
            }
            stdout = new String(p.getInputStream().readAllBytes());
            stderr = new String(p.getErrorStream().readAllBytes());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of(new TestResult("(execution)", "", "", false, "Interrupted"));
        }

        List<TestResult> results = new ArrayList<>();
        for (String line : stdout.split("\\R")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\t", -1);
            switch (parts[0]) {
                case "PASS" -> results.add(new TestResult(
                        parts.length > 1 ? parts[1] : "", "", "", true, null));
                case "FAIL" -> results.add(new TestResult(
                        parts.length > 1 ? parts[1] : "",
                        parts.length > 2 ? parts[2] : "",
                        parts.length > 3 ? parts[3] : "",
                        false, null));
                case "ERROR" -> results.add(new TestResult(
                        parts.length > 1 ? parts[1] : "", "", "", false,
                        parts.length > 2 ? parts[2] : "unknown error"));
                default -> { /* ignore unrecognized lines */ }
            }
        }

        if (results.isEmpty() && !stderr.isBlank()) {
            results.add(new TestResult("(execution)", "", "", false, stderr.trim()));
        }
        return results;
    }

    private void deleteRecursively(Path path) {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
    }
}
