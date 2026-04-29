package com.ailearn.controller;

import com.ailearn.api.learning.LearningHistoryItemResponse;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.LearningArticleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning-history")
public class LearningHistoryController {

    private final LearningArticleRepository learningArticleRepository;

    public LearningHistoryController(LearningArticleRepository learningArticleRepository) {
        this.learningArticleRepository = learningArticleRepository;
    }

    @GetMapping
    public List<LearningHistoryItemResponse> getLearningHistory() {
        return learningArticleRepository.findAllByOrderBySelectedAtDescCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private LearningHistoryItemResponse toResponse(LearningArticleEntity article) {
        return new LearningHistoryItemResponse(
                article.getId(),
                article.getTitle(),
                article.getSource(),
                article.getPublishedAt(),
                article.getSelectedAt(),
                article.getStatus()
        );
    }
}
