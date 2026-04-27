package com.ailearn.service.learning;

import com.ailearn.client.EudicClient;
import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import com.ailearn.model.VocabularyCandidate;
import com.ailearn.repository.ArticleParagraphRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.repository.VocabularyItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LearningWorkflowService {

    public static final String STATUS_CONTENT_READY = "CONTENT_READY";
    public static final String STATUS_TRANSLATED = "TRANSLATED";
    public static final String STATUS_VOCAB_READY = "VOCAB_READY";
    public static final String STATUS_EUDIC_PUSHED = "EUDIC_PUSHED";
    public static final String STATUS_FAILED = "FAILED";

    private final LearningArticleRepository learningArticleRepository;
    private final ArticleContentService articleContentService;
    private final ParagraphSplitService paragraphSplitService;
    private final TranslationService translationService;
    private final VocabularyExtractionService vocabularyExtractionService;
    private final ArticleParagraphRepository articleParagraphRepository;
    private final VocabularyItemRepository vocabularyItemRepository;
    private final EudicClient eudicClient;
    private final Clock clock;

    public LearningWorkflowService(
            LearningArticleRepository learningArticleRepository,
            ArticleContentService articleContentService,
            ParagraphSplitService paragraphSplitService,
            TranslationService translationService,
            VocabularyExtractionService vocabularyExtractionService,
            ArticleParagraphRepository articleParagraphRepository,
            VocabularyItemRepository vocabularyItemRepository,
            EudicClient eudicClient,
            Clock clock
    ) {
        this.learningArticleRepository = learningArticleRepository;
        this.articleContentService = articleContentService;
        this.paragraphSplitService = paragraphSplitService;
        this.translationService = translationService;
        this.vocabularyExtractionService = vocabularyExtractionService;
        this.articleParagraphRepository = articleParagraphRepository;
        this.vocabularyItemRepository = vocabularyItemRepository;
        this.eudicClient = eudicClient;
        this.clock = clock;
    }

    @Transactional
    public LearningArticleEntity processLearningArticle(Long learningArticleId) {
        LearningArticleEntity learningArticle = learningArticleRepository.findById(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning article not found"));

        try {
            String articleContent = articleContentService.fetchArticleContent(learningArticle.getUrl());
            learningArticle.setArticleContent(articleContent);
            learningArticle.setStatus(STATUS_CONTENT_READY);
            learningArticleRepository.save(learningArticle);

            List<String> paragraphs = paragraphSplitService.split(articleContent);
            List<String> translations = translationService.translate(paragraphs);

            articleParagraphRepository.deleteByLearningArticleId(learningArticleId);
            articleParagraphRepository.saveAll(toParagraphEntities(learningArticle, paragraphs, translations));

            learningArticle.setStatus(STATUS_TRANSLATED);
            learningArticle.setTranslatedAt(LocalDateTime.now(clock));
            learningArticleRepository.save(learningArticle);

            List<VocabularyCandidate> vocabularyCandidates = vocabularyExtractionService.extract(paragraphs);
            vocabularyItemRepository.deleteByLearningArticleId(learningArticleId);
            List<VocabularyItemEntity> items = vocabularyItemRepository.saveAll(toVocabularyEntities(learningArticle, vocabularyCandidates));

            learningArticle.setStatus(STATUS_VOCAB_READY);
            learningArticle.setVocabularyExtractedAt(LocalDateTime.now(clock));
            learningArticleRepository.save(learningArticle);

            pushVocabularyToEudic(learningArticle, items);
            return learningArticleRepository.save(learningArticle);
        } catch (RuntimeException exception) {
            learningArticle.setStatus(STATUS_FAILED);
            learningArticleRepository.save(learningArticle);
            throw exception;
        }
    }

    @Transactional
    public VocabularyItemEntity pushVocabularyItem(Long learningArticleId, Long vocabularyItemId) {
        LearningArticleEntity learningArticle = learningArticleRepository.findById(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning article not found"));
        VocabularyItemEntity item = vocabularyItemRepository.findById(vocabularyItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary item not found"));

        if (!item.getLearningArticle().getId().equals(learningArticleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vocabulary item not found");
        }
        if (!eudicClient.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Eudic is not configured");
        }

        String studyListId = eudicClient.ensureStudyList();
        boolean pushed = eudicClient.pushWord(studyListId, item);
        if (!pushed) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to push vocabulary item to Eudic");
        }

        item.setEudicPushed(true);
        item.setEudicPushedAt(LocalDateTime.now(clock));
        VocabularyItemEntity savedItem = vocabularyItemRepository.save(item);

        boolean allPushed = vocabularyItemRepository.findByLearningArticleIdOrderByCreatedAtAsc(learningArticleId).stream()
                .allMatch(VocabularyItemEntity::isEudicPushed);
        if (allPushed) {
            learningArticle.setStatus(STATUS_EUDIC_PUSHED);
            learningArticle.setEudicPushedAt(LocalDateTime.now(clock));
            learningArticleRepository.save(learningArticle);
        }

        return savedItem;
    }

    private List<ArticleParagraphEntity> toParagraphEntities(
            LearningArticleEntity learningArticle,
            List<String> paragraphs,
            List<String> translations
    ) {
        List<ArticleParagraphEntity> entities = new ArrayList<>();
        for (int index = 0; index < paragraphs.size(); index++) {
            ArticleParagraphEntity entity = new ArticleParagraphEntity();
            entity.setLearningArticle(learningArticle);
            entity.setParagraphIndex(index + 1);
            entity.setEnglishText(paragraphs.get(index));
            entity.setChineseText(index < translations.size() ? translations.get(index) : null);
            entities.add(entity);
        }
        return entities;
    }

    private List<VocabularyItemEntity> toVocabularyEntities(
            LearningArticleEntity learningArticle,
            List<VocabularyCandidate> vocabularyCandidates
    ) {
        List<VocabularyItemEntity> entities = new ArrayList<>();
        for (VocabularyCandidate candidate : vocabularyCandidates) {
            VocabularyItemEntity entity = new VocabularyItemEntity();
            entity.setLearningArticle(learningArticle);
            entity.setWord(candidate.word());
            entity.setLemma(candidate.lemma());
            entity.setType(candidate.type());
            entity.setIpa(candidate.ipa());
            entity.setEnglishDefinition(candidate.englishDefinition());
            entity.setChineseDefinition(candidate.chineseDefinition());
            entity.setSourceSentence(candidate.sourceSentence());
            entity.setEudicPushed(false);
            entities.add(entity);
        }
        return entities;
    }

    private void pushVocabularyToEudic(LearningArticleEntity learningArticle, List<VocabularyItemEntity> items) {
        if (!eudicClient.isConfigured() || items.isEmpty()) {
            return;
        }

        String studyListId = eudicClient.ensureStudyList();
        boolean allPushed = true;
        for (VocabularyItemEntity item : items) {
            boolean pushed = eudicClient.pushWord(studyListId, item);
            item.setEudicPushed(pushed);
            if (pushed) {
                item.setEudicPushedAt(LocalDateTime.now(clock));
            } else {
                allPushed = false;
            }
        }
        vocabularyItemRepository.saveAll(items);

        if (allPushed) {
            learningArticle.setStatus(STATUS_EUDIC_PUSHED);
            learningArticle.setEudicPushedAt(LocalDateTime.now(clock));
        }
    }
}
