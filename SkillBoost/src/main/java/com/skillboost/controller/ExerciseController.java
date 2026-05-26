package com.skillboost.controller;

import com.skillboost.model.Exercise;
import com.skillboost.service.ExerciseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public List<Exercise> list() {
        return exerciseService.list().stream()
                .map(Exercise::toPublicView)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Exercise> get(@PathVariable String id) {
        return exerciseService.findById(id)
                .map(Exercise::toPublicView)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
