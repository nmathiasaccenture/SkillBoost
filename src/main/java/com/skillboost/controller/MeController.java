package com.skillboost.controller;

import com.skillboost.model.AppUser;
import com.skillboost.model.ProgressEntity;
import com.skillboost.repository.ProgressRepository;
import com.skillboost.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository users;
    private final ProgressRepository progress;

    public MeController(UserRepository users, ProgressRepository progress) {
        this.users = users;
        this.progress = progress;
    }

    public record ProgressView(String exerciseId, boolean solved, Instant firstSolvedAt, int attempts) {
        static ProgressView from(ProgressEntity p) {
            return new ProgressView(p.getExerciseId(), p.isSolved(), p.getFirstSolvedAt(), p.getAttempts());
        }
    }

    @GetMapping
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        AppUser user = users.findByUsername(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "role", user.getRole().name(),
                "createdAt", user.getCreatedAt().toString()));
    }

    @GetMapping("/progress")
    public ResponseEntity<List<ProgressView>> progress(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        AppUser user = users.findByUsername(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();
        List<ProgressView> view = progress.findAllByUserId(user.getId()).stream()
                .map(ProgressView::from)
                .toList();
        return ResponseEntity.ok(view);
    }
}
