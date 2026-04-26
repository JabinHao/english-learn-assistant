package com.ailearn.repository;

import com.ailearn.entity.ArticleParagraphEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleParagraphRepository extends JpaRepository<ArticleParagraphEntity, Long> {

    List<ArticleParagraphEntity> findByLearningArticleIdOrderByParagraphIndexAsc(Long learningArticleId);
}
