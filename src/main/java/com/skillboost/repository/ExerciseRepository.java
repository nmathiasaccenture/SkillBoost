package com.skillboost.repository;

import com.skillboost.model.ExerciseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<ExerciseEntity, String> {
    List<ExerciseEntity> findAllByOrderByLanguageAscDifficultyAscIdAsc();
}
