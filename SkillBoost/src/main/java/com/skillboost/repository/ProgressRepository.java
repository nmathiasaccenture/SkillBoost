package com.skillboost.repository;

import com.skillboost.model.ProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<ProgressEntity, Long> {
    Optional<ProgressEntity> findByUserIdAndExerciseId(Long userId, String exerciseId);
    List<ProgressEntity> findAllByUserId(Long userId);
}
