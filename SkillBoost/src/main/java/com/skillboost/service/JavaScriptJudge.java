package com.skillboost.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JavaScriptJudge extends AbstractProcessJudge {

    @Override
    protected String solutionFilename() { return "solution.js"; }

    @Override
    protected String runnerFilename() { return "runner.js"; }

    @Override
    protected List<String> executeCommand() { return List.of("node", "runner.js"); }
}
