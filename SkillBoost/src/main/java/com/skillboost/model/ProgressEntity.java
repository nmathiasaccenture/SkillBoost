package com.skillboost.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "exercise_id"}))
public class ProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exercise_id", nullable = false, length = 128)
    private String exerciseId;

    @Column(nullable = false)
    private boolean solved;

    private Instant firstSolvedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ProgressEntity() {}

    public ProgressEntity(Long userId, String exerciseId) {
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.solved = false;
        this.attempts = 0;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getExerciseId() { return exerciseId; }
    public boolean isSolved() { return solved; }
    public Instant getFirstSolvedAt() { return firstSolvedAt; }
    public int getAttempts() { return attempts; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void recordAttempt(boolean allPassed) {
        this.attempts++;
        if (allPassed && !this.solved) {
            this.solved = true;
            this.firstSolvedAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }
}
