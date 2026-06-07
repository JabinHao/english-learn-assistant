package com.ailearn.controller;

import com.ailearn.api.learning.CreateLearningArticleRequest;
import com.ailearn.api.learning.LearningArticleResponse;
import com.ailearn.api.learning.SelectCandidateResponse;
import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import com.ailearn.repository.ArticleParagraphRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.repository.VocabularyItemRepository;
import com.ailearn.service.learning.LearningWorkflowService;
import com.ailearn.service.learning.ManualLearningArticleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/learning-articles")
public class LearningArticleController {

    private final LearningArticleRepository learningArticleRepository;
    private final ArticleParagraphRepository articleParagraphRepository;
    private final VocabularyItemRepository vocabularyItemRepository;
    private final LearningWorkflowService learningWorkflowService;
    private final ManualLearningArticleService manualLearningArticleService;

    public LearningArticleController(
            LearningArticleRepository learningArticleRepository,
            ArticleParagraphRepository articleParagraphRepository,
            VocabularyItemRepository vocabularyItemRepository,
            LearningWorkflowService learningWorkflowService,
            ManualLearningArticleService manualLearningArticleService
    ) {
        this.learningArticleRepository = learningArticleRepository;
        this.articleParagraphRepository = articleParagraphRepository;
        this.vocabularyItemRepository = vocabularyItemRepository;
        this.learningWorkflowService = learningWorkflowService;
        this.manualLearningArticleService = manualLearningArticleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SelectCandidateResponse createLearningArticle(@Valid @RequestBody CreateLearningArticleRequest request) {
        return manualLearningArticleService.submitUrl(request.url());
    }

    @GetMapping("/{learningArticleId}")
    public LearningArticleResponse getLearningArticle(@PathVariable Long learningArticleId) {
        LearningArticleEntity learningArticle = learningArticleRepository.findById(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning article not found"));
        List<ArticleParagraphEntity> paragraphs = articleParagraphRepository.findByLearningArticleIdOrderByParagraphIndexAsc(learningArticleId);
        List<VocabularyItemEntity> vocabularyItems = vocabularyItemRepository.findByLearningArticleIdOrderByCreatedAtAsc(learningArticleId);

        return new LearningArticleResponse(
                learningArticle.getId(),
                learningArticle.getCandidateArticle().getId(),
                learningArticle.getStatus(),
                learningArticle.getTitle(),
                learningArticle.getUrl(),
                learningArticle.getSource(),
                learningArticle.getPublishedAt(),
                learningArticle.getArticleContent(),
                learningArticle.getSummary(),
                paragraphs.stream()
                        .map(paragraph -> new LearningArticleResponse.ParagraphResponse(
                                paragraph.getParagraphIndex(),
                                paragraph.getEnglishText(),
                                paragraph.getChineseText()
                        ))
                        .toList(),
                vocabularyItems.stream()
                        .map(this::toVocabularyResponse)
                        .toList()
        );
    }

    @PostMapping("/{learningArticleId}/vocabulary/{vocabularyItemId}/push")
    public LearningArticleResponse.VocabularyItemResponse pushVocabularyItem(
            @PathVariable Long learningArticleId,
            @PathVariable Long vocabularyItemId
    ) {
        VocabularyItemEntity item = learningWorkflowService.pushVocabularyItem(learningArticleId, vocabularyItemId);
        return toVocabularyResponse(item);
    }

    @DeleteMapping("/{learningArticleId}/vocabulary/{vocabularyItemId}/push")
    public LearningArticleResponse.VocabularyItemResponse removeVocabularyItem(
            @PathVariable Long learningArticleId,
            @PathVariable Long vocabularyItemId
    ) {
        VocabularyItemEntity item = learningWorkflowService.removeVocabularyItem(learningArticleId, vocabularyItemId);
        return toVocabularyResponse(item);
    }

    @DeleteMapping("/{learningArticleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLearningArticle(@PathVariable Long learningArticleId) {
        learningWorkflowService.deleteLearningArticle(learningArticleId);
    }

    private LearningArticleResponse.VocabularyItemResponse toVocabularyResponse(VocabularyItemEntity item) {
        return new LearningArticleResponse.VocabularyItemResponse(
                                item.getId(),
                                item.getWord(),
                                item.getLemma(),
                                item.getType(),
                                item.getIpa(),
                                item.getEnglishDefinition(),
                                item.getChineseDefinition(),
                                item.getSourceSentence(),
                                item.isEudicPushed()
        );
    }
}
