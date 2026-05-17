package com.ailearn.repository;

import com.ailearn.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    Optional<ChatSessionEntity> findFirstByLearningArticleIdOrderByCreatedAtAsc(Long learningArticleId);

    Optional<ChatSessionEntity> findFirstByLearningArticleIdOrderByCreatedAtDesc(Long learningArticleId);

    Optional<ChatSessionEntity> findByIdAndLearningArticleId(Long id, Long learningArticleId);

    List<ChatSessionEntity> findByLearningArticleIdOrderByCreatedAtAsc(Long learningArticleId);

    List<ChatSessionEntity> findByLearningArticleIdOrderByCreatedAtDesc(Long learningArticleId);
}
