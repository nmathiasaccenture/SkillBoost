package com.skillboost.controller;

import com.skillboost.model.Submission;
import com.skillboost.model.SubmissionResult;
import com.skillboost.service.AbstractProcessJudge;
import com.skillboost.service.ExerciseService;
import com.skillboost.service.JavaJudge;
import com.skillboost.service.JavaScriptJudge;
import com.skillboost.service.PythonJudge;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final ExerciseService exerciseService;
    private final Map<String, AbstractProcessJudge> judges;

    public SubmissionController(ExerciseService exerciseService,
                                JavaJudge javaJudge,
                                PythonJudge pythonJudge,
                                JavaScriptJudge javaScriptJudge) {
        this.exerciseService = exerciseService;
        this.judges = Map.of(
                "java", javaJudge,
                "python", pythonJudge,
                "javascript", javaScriptJudge
        );
    }

    @PostMapping
    public ResponseEntity<SubmissionResult> submit(@Valid @RequestBody Submission submission) throws IOException {
        var exercise = exerciseService.findById(submission.exerciseId()).orElse(null);
        if (exercise == null) {
            return ResponseEntity.notFound().build();
        }
        AbstractProcessJudge judge = judges.get(exercise.language());
        if (judge == null) {
            return ResponseEntity.badRequest().body(new SubmissionResult(false,
                    "Language '" + exercise.language() + "' is not yet supported.", false, java.util.List.of()));
        }
        return ResponseEntity.ok(judge.run(exercise, submission.code()));
    }
}
