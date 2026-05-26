package com.skillboost.controller;

import com.skillboost.model.Submission;
import com.skillboost.model.SubmissionResult;
import com.skillboost.service.ExerciseService;
import com.skillboost.service.JavaJudge;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final ExerciseService exerciseService;
    private final JavaJudge javaJudge;

    public SubmissionController(ExerciseService exerciseService, JavaJudge javaJudge) {
        this.exerciseService = exerciseService;
        this.javaJudge = javaJudge;
    }

    @PostMapping
    public ResponseEntity<SubmissionResult> submit(@Valid @RequestBody Submission submission) throws IOException {
        var exercise = exerciseService.findById(submission.exerciseId()).orElse(null);
        if (exercise == null) {
            return ResponseEntity.notFound().build();
        }
        if (!"java".equals(exercise.language())) {
            return ResponseEntity.badRequest().body(new SubmissionResult(false,
                    "Language '" + exercise.language() + "' is not yet supported.", false, java.util.List.of()));
        }
        return ResponseEntity.ok(javaJudge.run(exercise, submission.code()));
    }
}
