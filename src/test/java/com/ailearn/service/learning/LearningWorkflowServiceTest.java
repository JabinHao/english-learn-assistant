package com.ailearn.service.learning;

import com.ailearn.client.EudicClient;
import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import com.ailearn.model.VocabularyCandidate;
import com.ailearn.repository.ArticleParagraphRepository;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.repository.VocabularyItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningWorkflowServiceTest {

    @Test
    void processLearningArticle_shouldPersistContentParagraphsVocabularyWithoutAutoPushingToEudic() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-26T01:00:00Z"), ZoneOffset.UTC);
        LearningArticleEntity article = learningArticle(88L, 7L);
        AtomicReference<LearningArticleEntity> savedArticle = new AtomicReference<>();
        AtomicReference<List<ArticleParagraphEntity>> savedParagraphs = new AtomicReference<>(List.of());
        AtomicReference<List<VocabularyItemEntity>> savedVocabulary = new AtomicReference<>(List.of());
        AtomicBoolean exported = new AtomicBoolean(false);

        LearningWorkflowService service = new LearningWorkflowService(
                learningArticleRepository(article, savedArticle),
                new ArticleContentService() {
                    @Override
                    public FetchedArticle fetchArticle(String url) {
                        return new FetchedArticle(null, "Paragraph one.\n\nParagraph two.");
                    }
                },
                new ParagraphSplitService(),
                new TranslationService(null, prompt -> "", "") {
                    @Override
                    public List<String> translate(List<String> paragraphs) {
                        return List.of("第一段。", "第二段。");
                    }
                },
                new VocabularyExtractionService(null, prompt -> "", "") {
                    @Override
                    public List<VocabularyCandidate> extract(List<String> paragraphs) {
                        return List.of(new VocabularyCandidate("reasoning", "reasoning", "WORD", "", "thinking carefully", "推理", "Paragraph one."));
                    }
                },
                paragraphRepository(savedParagraphs),
                vocabularyRepository(savedVocabulary),
                new EudicClient(null),
                null,
                new LearningArticleExportService() {
                    @Override
                    public java.nio.file.Path export(
                            LearningArticleEntity article,
                            List<ArticleParagraphEntity> paragraphs,
                            List<VocabularyItemEntity> vocabularyItems
                    ) {
                        exported.set(true);
                        return java.nio.file.Path.of("data/exports/articles/88-selected-article.html");
                    }
                },
                clock
        );

        LearningArticleEntity result = service.processLearningArticle(88L);

        assertThat(savedParagraphs.get()).hasSize(2);
        assertThat(savedParagraphs.get().getFirst().getChineseText()).isEqualTo("第一段。");
        assertThat(savedVocabulary.get()).hasSize(1);
        assertThat(savedVocabulary.get().getFirst().isEudicPushed()).isFalse();
        assertThat(result.getStatus()).isEqualTo("VOCAB_READY");
        assertThat(result.getTranslatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 26, 1, 0));
        assertThat(result.getVocabularyExtractedAt()).isEqualTo(LocalDateTime.of(2026, 4, 26, 1, 0));
        assertThat(result.getEudicPushedAt()).isNull();
        assertThat(exported).isTrue();
    }

    @Test
    void processLearningArticle_shouldRemainVocabReadyWhenStaticExportFails() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-26T01:00:00Z"), ZoneOffset.UTC);
        LearningArticleEntity article = learningArticle(88L, 7L);

        LearningWorkflowService service = new LearningWorkflowService(
                learningArticleRepository(article, new AtomicReference<>()),
                new ArticleContentService() {
                    @Override
                    public FetchedArticle fetchArticle(String url) {
                        return new FetchedArticle(null, "Paragraph one.");
                    }
                },
                new ParagraphSplitService(),
                new TranslationService(null, prompt -> "", "") {
                    @Override
                    public List<String> translate(List<String> paragraphs) {
                        return List.of("第一段。");
                    }
                },
                new VocabularyExtractionService(null, prompt -> "", "") {
                    @Override
                    public List<VocabularyCandidate> extract(List<String> paragraphs) {
                        return List.of();
                    }
                },
                paragraphRepository(new AtomicReference<>(List.of())),
                vocabularyRepository(new AtomicReference<>(List.of())),
                new EudicClient(null),
                null,
                new LearningArticleExportService() {
                    @Override
                    public java.nio.file.Path export(
                            LearningArticleEntity article,
                            List<ArticleParagraphEntity> paragraphs,
                            List<VocabularyItemEntity> vocabularyItems
                    ) throws java.io.IOException {
                        throw new java.io.IOException("disk full");
                    }
                },
                clock
        );

        LearningArticleEntity result = service.processLearningArticle(88L);

        assertThat(result.getStatus()).isEqualTo("VOCAB_READY");
    }

    @Test
    void processLearningArticle_shouldPopulateManualArticleChineseMetadataFromFetchedArticle() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-26T01:00:00Z"), ZoneOffset.UTC);
        LearningArticleEntity article = learningArticle(88L, 7L);
        article.setSource("Manual");
        article.getCandidateArticle().setSource("Manual");
        AtomicReference<CandidateArticleEntity> savedCandidate = new AtomicReference<>();
        AtomicReference<LearningArticleEntity> savedArticle = new AtomicReference<>();

        LearningWorkflowService service = new LearningWorkflowService(
                learningArticleRepository(article, savedArticle),
                new ArticleContentService() {
                    @Override
                    public FetchedArticle fetchArticle(String url) {
                        return new FetchedArticle(
                                "Spring Isn’t Dead",
                                "For the last three years, Java teams were told to use Python for LLMs.\n\nThat take aged badly."
                        );
                    }
                },
                new ParagraphSplitService(),
                new TranslationService(null, prompt -> "", "") {
                    @Override
                    public List<String> translate(List<String> paragraphs) {
                        if (paragraphs.equals(List.of("Spring Isn’t Dead"))) {
                            return List.of("Spring 并未过时");
                        }
                        return List.of("过去三年来，Java 团队一直被建议用 Python 做 LLM。", "这种说法已经站不住脚。");
                    }
                },
                new VocabularyExtractionService(null, prompt -> "", "") {
                    @Override
                    public List<VocabularyCandidate> extract(List<String> paragraphs) {
                        return List.of();
                    }
                },
                paragraphRepository(new AtomicReference<>(List.of())),
                vocabularyRepository(new AtomicReference<>(List.of())),
                new EudicClient(null),
                candidateArticleRepository(savedCandidate),
                null,
                clock
        );

        LearningArticleEntity result = service.processLearningArticle(88L);

        assertThat(result.getTitle()).isEqualTo("Spring Isn’t Dead");
        assertThat(result.getSummary()).isEqualTo("For the last three years, Java teams were told to use Python for LLMs.");
        assertThat(savedCandidate.get().getChineseTitle()).isEqualTo("Spring 并未过时");
        assertThat(savedCandidate.get().getChineseSummary()).isEqualTo("过去三年来，Java 团队一直被建议用 Python 做 LLM。");
    }

    @Test
    void pushVocabularyItem_shouldMapEudicClientFailureToBadGateway() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-26T01:00:00Z"), ZoneOffset.UTC);
        LearningArticleEntity article = learningArticle(88L, 7L);
        VocabularyItemEntity vocabularyItem = vocabularyItem(article, 13L);
        LearningWorkflowService service = new LearningWorkflowService(
                learningArticleRepository(article, new AtomicReference<>()),
                null,
                null,
                null,
                null,
                null,
                vocabularyRepository(vocabularyItem),
                new EudicClient(null) {
                    @Override
                    public boolean isConfigured() {
                        return true;
                    }

                    @Override
                    public String ensureStudyList() {
                        throw new IllegalStateException("Eudic category response is invalid");
                    }
                },
                null,
                null,
                clock
        );

        assertThatThrownBy(() -> service.pushVocabularyItem(88L, 13L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode().value()).isEqualTo(502))
                .hasMessageContaining("Failed to sync vocabulary item to Eudic");
    }

    @Test
    void deleteLearningArticle_shouldDeleteArticleAndClearCandidateSelection() {
        LearningArticleEntity article = learningArticle(88L, 7L);
        article.getCandidateArticle().setSelected(true);
        AtomicReference<LearningArticleEntity> deletedArticle = new AtomicReference<>();
        AtomicReference<CandidateArticleEntity> savedCandidate = new AtomicReference<>();

        LearningWorkflowService service = new LearningWorkflowService(
                learningArticleRepository(article, new AtomicReference<>(), deletedArticle),
                null,
                null,
                null,
                null,
                null,
                null,
                new EudicClient(null),
                candidateArticleRepository(savedCandidate),
                null,
                java.time.Clock.systemUTC()
        );

        service.deleteLearningArticle(88L);

        assertThat(deletedArticle.get()).isSameAs(article);
        assertThat(savedCandidate.get()).isSameAs(article.getCandidateArticle());
        assertThat(savedCandidate.get().isSelected()).isFalse();
    }

    private LearningArticleRepository learningArticleRepository(
            LearningArticleEntity article,
            AtomicReference<LearningArticleEntity> savedArticle
    ) {
        return (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(article);
                    case "save" -> {
                        LearningArticleEntity value = (LearningArticleEntity) args[0];
                        savedArticle.set(value);
                        yield value;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LearningArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private LearningArticleRepository learningArticleRepository(
            LearningArticleEntity article,
            AtomicReference<LearningArticleEntity> savedArticle,
            AtomicReference<LearningArticleEntity> deletedArticle
    ) {
        return (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(article);
                    case "save" -> {
                        LearningArticleEntity value = (LearningArticleEntity) args[0];
                        savedArticle.set(value);
                        yield value;
                    }
                    case "delete" -> {
                        deletedArticle.set((LearningArticleEntity) args[0]);
                        yield null;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LearningArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private CandidateArticleRepository candidateArticleRepository(AtomicReference<CandidateArticleEntity> savedCandidate) {
        return (CandidateArticleRepository) Proxy.newProxyInstance(
                CandidateArticleRepository.class.getClassLoader(),
                new Class[]{CandidateArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        CandidateArticleEntity value = (CandidateArticleEntity) args[0];
                        savedCandidate.set(value);
                        yield value;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "CandidateArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private ArticleParagraphRepository paragraphRepository(AtomicReference<List<ArticleParagraphEntity>> savedParagraphs) {
        return (ArticleParagraphRepository) Proxy.newProxyInstance(
                ArticleParagraphRepository.class.getClassLoader(),
                new Class[]{ArticleParagraphRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "deleteByLearningArticleId" -> null;
                    case "saveAll" -> {
                        @SuppressWarnings("unchecked")
                        List<ArticleParagraphEntity> values = new ArrayList<>((List<ArticleParagraphEntity>) args[0]);
                        savedParagraphs.set(values);
                        yield values;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "ArticleParagraphRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private VocabularyItemRepository vocabularyRepository(AtomicReference<List<VocabularyItemEntity>> savedVocabulary) {
        return (VocabularyItemRepository) Proxy.newProxyInstance(
                VocabularyItemRepository.class.getClassLoader(),
                new Class[]{VocabularyItemRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "deleteByLearningArticleId" -> null;
                    case "saveAll" -> {
                        @SuppressWarnings("unchecked")
                        List<VocabularyItemEntity> values = new ArrayList<>((List<VocabularyItemEntity>) args[0]);
                        savedVocabulary.set(values);
                        yield values;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "VocabularyItemRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private VocabularyItemRepository vocabularyRepository(VocabularyItemEntity item) {
        return (VocabularyItemRepository) Proxy.newProxyInstance(
                VocabularyItemRepository.class.getClassLoader(),
                new Class[]{VocabularyItemRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(item);
                    case "findByLearningArticleIdOrderByCreatedAtAsc" -> List.of(item);
                    case "save" -> args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "VocabularyItemRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private LearningArticleEntity learningArticle(Long learningArticleId, Long candidateArticleId) {
        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidateArticle, "id", candidateArticleId);

        LearningArticleEntity learningArticle = new LearningArticleEntity();
        ReflectionTestUtils.setField(learningArticle, "id", learningArticleId);
        learningArticle.setCandidateArticle(candidateArticle);
        learningArticle.setStatus("SELECTED");
        learningArticle.setTitle("Selected article");
        learningArticle.setUrl("https://example.com/article");
        learningArticle.setSource("Test Feed");
        return learningArticle;
    }

    private VocabularyItemEntity vocabularyItem(LearningArticleEntity article, Long vocabularyItemId) {
        VocabularyItemEntity item = new VocabularyItemEntity();
        ReflectionTestUtils.setField(item, "id", vocabularyItemId);
        item.setLearningArticle(article);
        item.setWord("reasoning");
        item.setType("WORD");
        item.setEudicPushed(false);
        return item;
    }
}
