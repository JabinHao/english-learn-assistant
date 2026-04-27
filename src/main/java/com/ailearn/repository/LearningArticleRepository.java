package com.ailearn.repository;

import com.ailearn.entity.LearningArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningArticleRepository extends JpaRepository<LearningArticleEntity, Long> {

    Optional<LearningArticleEntity> findByCandidateArticleId(Long candidateArticleId);
}
