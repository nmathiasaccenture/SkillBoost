package com.skillboost.service;

import com.skillboost.service.sandbox.Sandbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JavaScriptJudge extends AbstractProcessJudge {

    @Autowired
    public JavaScriptJudge(Sandbox sandbox) { super(sandbox); }

    public JavaScriptJudge() { super(); }

    @Override
    protected String solutionFilename() { return "solution.js"; }

    @Override
    protected String runnerFilename() { return "runner.js"; }

    @Override
    protected List<String> executeCommand() { return List.of("node", "runner.js"); }
}
