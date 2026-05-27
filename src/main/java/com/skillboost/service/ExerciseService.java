package com.skillboost.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillboost.model.Exercise;
import com.skillboost.model.ExerciseEntity;
import com.skillboost.repository.ExerciseRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExerciseRepository repository;
    private final JsonExerciseLoader loader;

    public ExerciseService(ExerciseRepository repository, JsonExerciseLoader loader) {
        this.repository = repository;
        this.loader = loader;
    }

    @PostConstruct
    @Transactional
    public void seedFromClasspathIfEmpty() throws java.io.IOException {
        if (repository.count() > 0) {
            return;
        }
        log.info("Exercise table is empty; seeding from classpath JSON files.");
        for (Exercise ex : loader.loadAll()) {
            repository.save(toEntity(ex));
        }
    }

    public List<Exercise> list() {
        return repository.findAllByOrderByLanguageAscDifficultyAscIdAsc()
                .stream()
                .map(this::toExercise)
                .toList();
    }

    public Optional<Exercise> findById(String id) {
        return repository.findById(id).map(this::toExercise);
    }

    @Transactional
    public Exercise save(Exercise exercise) {
        ExerciseEntity entity = toEntity(exercise);
        repository.save(entity);
        return exercise;
    }

    @Transactional
    public Optional<Exercise> update(String id, Exercise exercise) {
        return repository.findById(id).map(entity -> {
            String json = serialize(exercise);
            entity.update(exercise.language(), exercise.difficulty(), exercise.title(), json);
            return exercise;
        });
    }

    @Transactional
    public boolean delete(String id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private ExerciseEntity toEntity(Exercise ex) {
        return new ExerciseEntity(ex.id(), ex.language(), ex.difficulty(), ex.title(), serialize(ex));
    }

    private Exercise toExercise(ExerciseEntity entity) {
        try {
            return mapper.readValue(entity.getDataJson(), Exercise.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Stored exercise JSON for id '" + entity.getId() + "' is invalid", e);
        }
    }

    private String serialize(Exercise ex) {
        try {
            return mapper.writeValueAsString(ex);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Exercise cannot be serialized to JSON", e);
        }
    }
}
