package com.skillboost.service;

import com.skillboost.service.sandbox.Sandbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PythonJudge extends AbstractProcessJudge {

    @Autowired
    public PythonJudge(Sandbox sandbox) { super(sandbox); }

    public PythonJudge() { super(); }

    @Override
    protected String solutionFilename() { return "solution.py"; }

    @Override
    protected String runnerFilename() { return "runner.py"; }

    @Override
    protected List<String> executeCommand() { return List.of("python", "runner.py"); }
}
