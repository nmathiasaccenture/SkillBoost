package com.skillboost.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillboost.model.Exercise;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonExerciseLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Exercise> loadAll() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] files = resolver.getResources("classpath:exercises/*.json");
        List<Exercise> list = new ArrayList<>();
        for (Resource file : files) {
            try (var in = file.getInputStream()) {
                list.add(mapper.readValue(in, Exercise.class));
            }
        }
        return list;
    }
}
