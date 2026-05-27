package com.skillboost.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "exercises")
public class ExerciseEntity {

    @Id
    @Column(length = 128)
    private String id;

    @Column(nullable = false, length = 32)
    private String language;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false, length = 256)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String dataJson;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ExerciseEntity() {}

    public ExerciseEntity(String id, String language, int difficulty, String title, String dataJson) {
        this.id = id;
        this.language = language;
        this.difficulty = difficulty;
        this.title = title;
        this.dataJson = dataJson;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getLanguage() { return language; }
    public int getDifficulty() { return difficulty; }
    public String getTitle() { return title; }
    public String getDataJson() { return dataJson; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String language, int difficulty, String title, String dataJson) {
        this.language = language;
        this.difficulty = difficulty;
        this.title = title;
        this.dataJson = dataJson;
        this.updatedAt = Instant.now();
    }
}
