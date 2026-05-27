package com.skillboost.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PythonJudge extends AbstractProcessJudge {

    @Override
    protected String solutionFilename() { return "solution.py"; }

    @Override
    protected String runnerFilename() { return "runner.py"; }

    @Override
    protected List<String> executeCommand() { return List.of("python", "runner.py"); }
}
