package com.skillboost.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillboost.model.Exercise;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExerciseService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Exercise> exercises = new LinkedHashMap<>();

    @PostConstruct
    public void loadExercises() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] files = resolver.getResources("classpath:exercises/*.json");
        for (Resource file : files) {
            try (var in = file.getInputStream()) {
                Exercise ex = mapper.readValue(in, Exercise.class);
                exercises.put(ex.id(), ex);
            }
        }
    }

    public List<Exercise> list() {
        return List.copyOf(exercises.values());
    }

    public Optional<Exercise> findById(String id) {
        return Optional.ofNullable(exercises.get(id));
    }
}
