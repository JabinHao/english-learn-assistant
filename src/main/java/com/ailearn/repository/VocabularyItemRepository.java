package com.ailearn.repository;

import com.ailearn.entity.VocabularyItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VocabularyItemRepository extends JpaRepository<VocabularyItemEntity, Long> {

    List<VocabularyItemEntity> findByLearningArticleIdOrderByCreatedAtAsc(Long learningArticleId);

    void deleteByLearningArticleId(Long learningArticleId);
}
