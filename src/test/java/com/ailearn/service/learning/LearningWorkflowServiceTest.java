package com.ailearn.service.learning;

import com.ailearn.client.EudicClient;
import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import com.ailearn.model.VocabularyCandidate;
import com.ailearn.repository.ArticleParagraphRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.repository.VocabularyItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LearningWorkflowServiceTest {

    @Test
    void processLearningArticle_shouldPersistContentParagraphsVocabularyWithoutAutoPushingToEudic() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-26T01:00:00Z"), ZoneOffset.UTC);
        LearningArticleEntity article = learningArticle(88L, 7L);
        AtomicReference<LearningArticleEntity> savedArticle = new AtomicReference<>();
        AtomicReference<List<ArticleParagraphEntity>> savedParagraphs = new AtomicReference<>(List.of());
        AtomicReference<List<VocabularyItemEntity>> savedVocabulary = new AtomicReference<>(List.of());

        LearningWorkflowService service = new LearningWorkflowService(
                learningArticleRepository(article, savedArticle),
                new ArticleContentService() {
                    @Override
                    public String fetchArticleContent(String url) {
                        return "Paragraph one.\n\nParagraph two.";
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
}
