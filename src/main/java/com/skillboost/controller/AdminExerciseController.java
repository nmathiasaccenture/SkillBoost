package com.skillboost.controller;

import com.skillboost.model.Exercise;
import com.skillboost.service.ExerciseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/exercises")
public class AdminExerciseController {

    private static final List<String> SUPPORTED_LANGUAGES = List.of("java", "javascript", "python");

    private final ExerciseService exerciseService;

    public AdminExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    public record ExerciseRequest(
            @NotBlank String id,
            @NotBlank String language,
            int difficulty,
            @NotBlank String title,
            String description,
            String hint,
            @NotBlank String buggyCode,
            @NotBlank String solutionCode,
            @NotBlank String testHarness,
            List<Exercise.TestCase> tests) {

        Exercise toExercise() {
            return new Exercise(id, language, difficulty, title, description, hint,
                    buggyCode, solutionCode, testHarness,
                    tests == null ? List.of() : tests);
        }
    }

    @GetMapping
    public List<Exercise> list() {
        return exerciseService.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Exercise> get(@PathVariable String id) {
        return exerciseService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ExerciseRequest req) {
        String err = validateLanguage(req.language());
        if (err != null) return ResponseEntity.badRequest().body(Map.of("error", err));
        if (exerciseService.findById(req.id()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Exercise id already exists"));
        }
        Exercise saved = exerciseService.save(req.toExercise());
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody ExerciseRequest req) {
        String err = validateLanguage(req.language());
        if (err != null) return ResponseEntity.badRequest().body(Map.of("error", err));
        if (!id.equals(req.id())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Path id must match body id"));
        }
        return exerciseService.update(id, req.toExercise())
                .map(ex -> ResponseEntity.ok((Object) ex))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return exerciseService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private static String validateLanguage(String language) {
        return SUPPORTED_LANGUAGES.contains(language)
                ? null
                : "Language must be one of " + SUPPORTED_LANGUAGES;
    }
}
