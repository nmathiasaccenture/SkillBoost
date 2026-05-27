package com.skillboost.controller;

import com.skillboost.model.AppUser;
import com.skillboost.model.ProgressEntity;
import com.skillboost.model.Submission;
import com.skillboost.model.SubmissionResult;
import com.skillboost.repository.ProgressRepository;
import com.skillboost.repository.UserRepository;
import com.skillboost.service.AbstractProcessJudge;
import com.skillboost.service.ExerciseService;
import com.skillboost.service.JavaJudge;
import com.skillboost.service.JavaScriptJudge;
import com.skillboost.service.PythonJudge;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);

    private final ExerciseService exerciseService;
    private final Map<String, AbstractProcessJudge> judges;
    private final UserRepository users;
    private final ProgressRepository progressRepo;

    public SubmissionController(ExerciseService exerciseService,
                                JavaJudge javaJudge,
                                PythonJudge pythonJudge,
                                JavaScriptJudge javaScriptJudge,
                                UserRepository users,
                                ProgressRepository progressRepo) {
        this.exerciseService = exerciseService;
        this.judges = Map.of(
                "java", javaJudge,
                "python", pythonJudge,
                "javascript", javaScriptJudge
        );
        this.users = users;
        this.progressRepo = progressRepo;
    }

    @PostMapping
    public ResponseEntity<SubmissionResult> submit(@Valid @RequestBody Submission submission,
                                                   Authentication auth) throws IOException {
        var exercise = exerciseService.findById(submission.exerciseId()).orElse(null);
        if (exercise == null) {
            return ResponseEntity.notFound().build();
        }
        AbstractProcessJudge judge = judges.get(exercise.language());
        if (judge == null) {
            return ResponseEntity.badRequest().body(new SubmissionResult(false,
                    "Language '" + exercise.language() + "' is not yet supported.", false, java.util.List.of()));
        }
        SubmissionResult result = judge.run(exercise, submission.code());
        if (auth != null && auth.isAuthenticated()) {
            recordProgress(auth.getName(), exercise.id(), result.allPassed());
        }
        return ResponseEntity.ok(result);
    }

    @Transactional
    void recordProgress(String username, String exerciseId, boolean allPassed) {
        try {
            AppUser user = users.findByUsername(username).orElse(null);
            if (user == null) return;
            ProgressEntity p = progressRepo.findByUserIdAndExerciseId(user.getId(), exerciseId)
                    .orElseGet(() -> new ProgressEntity(user.getId(), exerciseId));
            p.recordAttempt(allPassed);
            progressRepo.save(p);
        } catch (Exception ex) {
            // Don't let progress-tracking failures fail the submission response.
            log.warn("Failed to record progress for user '{}' on exercise '{}'", username, exerciseId, ex);
        }
    }
}
