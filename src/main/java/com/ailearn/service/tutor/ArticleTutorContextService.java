package com.ailearn.service.tutor;

import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import com.ailearn.repository.ArticleParagraphRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.repository.VocabularyItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ArticleTutorContextService {

    private final LearningArticleRepository learningArticleRepository;
    private final ArticleParagraphRepository articleParagraphRepository;
    private final VocabularyItemRepository vocabularyItemRepository;

    public ArticleTutorContextService(
            LearningArticleRepository learningArticleRepository,
            ArticleParagraphRepository articleParagraphRepository,
            VocabularyItemRepository vocabularyItemRepository
    ) {
        this.learningArticleRepository = learningArticleRepository;
        this.articleParagraphRepository = articleParagraphRepository;
        this.vocabularyItemRepository = vocabularyItemRepository;
    }

    public String buildContext(Long learningArticleId) {
        LearningArticleEntity article = learningArticleRepository.findById(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning article not found"));
        List<ArticleParagraphEntity> paragraphs = articleParagraphRepository.findByLearningArticleIdOrderByParagraphIndexAsc(learningArticleId);
        List<VocabularyItemEntity> vocabularyItems = vocabularyItemRepository.findByLearningArticleIdOrderByCreatedAtAsc(learningArticleId);

        StringBuilder builder = new StringBuilder();
        builder.append("Article Title: ").append(article.getTitle()).append("\n")
                .append("Source: ").append(article.getSource()).append("\n")
                .append("Status: ").append(article.getStatus()).append("\n");

        if (article.getSummary() != null && !article.getSummary().isBlank()) {
            builder.append("Summary: ").append(article.getSummary()).append("\n");
        }

        builder.append("\nParagraphs:\n");
        for (ArticleParagraphEntity paragraph : paragraphs) {
            builder.append(paragraph.getParagraphIndex())
                    .append(". EN: ").append(paragraph.getEnglishText()).append("\n");
            if (paragraph.getChineseText() != null && !paragraph.getChineseText().isBlank()) {
                builder.append("   ZH: ").append(paragraph.getChineseText()).append("\n");
            }
        }

        builder.append("\nVocabulary:\n");
        for (VocabularyItemEntity item : vocabularyItems) {
            builder.append("- ").append(item.getWord())
                    .append(" [").append(item.getType()).append("]");
            if (item.getChineseDefinition() != null && !item.getChineseDefinition().isBlank()) {
                builder.append(" : ").append(item.getChineseDefinition());
            }
            if (item.getSourceSentence() != null && !item.getSourceSentence().isBlank()) {
                builder.append(" | source: ").append(item.getSourceSentence());
            }
            builder.append("\n");
        }

        return builder.toString().trim();
    }
}
