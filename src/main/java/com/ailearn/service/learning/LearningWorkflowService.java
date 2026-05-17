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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LearningWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(LearningWorkflowService.class);

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
    private final LearningArticleExportService learningArticleExportService;
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
            LearningArticleExportService learningArticleExportService,
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
        this.learningArticleExportService = learningArticleExportService;
        this.clock = clock;
    }

    @Transactional
    public LearningArticleEntity processLearningArticle(Long learningArticleId) {
        LearningArticleEntity learningArticle = learningArticleRepository.findById(learningArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning article not found"));

        try {
            log.info("learning.workflow.start learningArticleId={} url={}", learningArticleId, learningArticle.getUrl());
            String articleContent = articleContentService.fetchArticleContent(learningArticle.getUrl());
            learningArticle.setArticleContent(articleContent);
            learningArticle.setStatus(STATUS_CONTENT_READY);
            learningArticleRepository.save(learningArticle);

            List<String> paragraphs = paragraphSplitService.split(articleContent);
            List<String> translations = translationService.translate(paragraphs);
            log.info("learning.workflow.translated learningArticleId={} paragraphCount={}", learningArticleId, paragraphs.size());

            articleParagraphRepository.deleteByLearningArticleId(learningArticleId);
            List<ArticleParagraphEntity> paragraphEntities = articleParagraphRepository.saveAll(
                    toParagraphEntities(learningArticle, paragraphs, translations)
            );

            learningArticle.setStatus(STATUS_TRANSLATED);
            learningArticle.setTranslatedAt(LocalDateTime.now(clock));
            learningArticleRepository.save(learningArticle);

            List<VocabularyCandidate> vocabularyCandidates = vocabularyExtractionService.extract(paragraphs);
            vocabularyItemRepository.deleteByLearningArticleId(learningArticleId);
            List<VocabularyItemEntity> items = vocabularyItemRepository.saveAll(toVocabularyEntities(learningArticle, vocabularyCandidates));
            log.info("learning.workflow.vocabulary_extracted learningArticleId={} vocabularyCount={}", learningArticleId, items.size());

            learningArticle.setStatus(STATUS_VOCAB_READY);
            learningArticle.setVocabularyExtractedAt(LocalDateTime.now(clock));
            learningArticleRepository.save(learningArticle);

            try {
                if (learningArticleExportService != null) {
                    learningArticleExportService.export(
                            learningArticle,
                            paragraphEntities,
                            items
                    );
                }
            } catch (Exception exception) {
                log.warn("learning.export.failed learningArticleId={} error={}", learningArticleId, exception.getMessage(), exception);
            }

            log.info("learning.workflow.completed learningArticleId={} status={}", learningArticleId, learningArticle.getStatus());
            return learningArticleRepository.save(learningArticle);
        } catch (RuntimeException exception) {
            learningArticle.setStatus(STATUS_FAILED);
            learningArticleRepository.save(learningArticle);
            log.error("learning.workflow.failed learningArticleId={} error={}", learningArticleId, exception.getMessage(), exception);
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

        boolean pushed;
        try {
            String studyListId = eudicClient.ensureStudyList();
            pushed = eudicClient.pushWord(studyListId, item);
            if (pushed) {
                eudicClient.pushNote(studyListId, item);
            }
        } catch (Exception exception) {
            log.error("learning.workflow.eudic_push_failed learningArticleId={} vocabularyItemId={} word={} error={}",
                    learningArticleId, vocabularyItemId, item.getWord(), exception.getMessage(), exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to sync vocabulary item to Eudic", exception);
        }
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

    @Transactional
    public VocabularyItemEntity removeVocabularyItem(Long learningArticleId, Long vocabularyItemId) {
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

        boolean removed;
        try {
            String studyListId = eudicClient.ensureStudyList();
            removed = eudicClient.deleteWord(studyListId, item);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to sync vocabulary item to Eudic", exception);
        }
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to remove vocabulary item from Eudic");
        }

        item.setEudicPushed(false);
        item.setEudicPushedAt(null);
        VocabularyItemEntity savedItem = vocabularyItemRepository.save(item);
        learningArticle.setStatus(STATUS_VOCAB_READY);
        learningArticle.setEudicPushedAt(null);
        learningArticleRepository.save(learningArticle);
        log.info(
                "learning.workflow.eudic_remove learningArticleId={} vocabularyItemId={} word={}",
                learningArticleId,
                vocabularyItemId,
                item.getWord()
        );
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

}
