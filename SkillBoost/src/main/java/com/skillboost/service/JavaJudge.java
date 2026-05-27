package com.skillboost.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class JavaJudge extends AbstractProcessJudge {

    @Override
    protected String solutionFilename() { return "Solution.java"; }

    @Override
    protected String runnerFilename() { return "Runner.java"; }

    @Override
    protected List<String> executeCommand() { return List.of("java", "-cp", ".", "Runner"); }

    @Override
    protected String compile(Path workDir) throws IOException {
        return runProcessMerged(workDir, List.of("javac", "Solution.java", "Runner.java"), "Compilation");
    }
}
