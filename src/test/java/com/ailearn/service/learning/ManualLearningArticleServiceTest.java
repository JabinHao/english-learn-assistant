package com.ailearn.service.learning;

import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.CandidateBatchEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.CandidateBatchRepository;
import com.ailearn.repository.LearningArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ManualLearningArticleServiceTest {

    @Test
    void submitUrl_shouldStripFragmentBeforeLookupAndPersistence() {
        AtomicReference<String> lookupUrl = new AtomicReference<>();
        LearningArticleEntity existing = learningArticle();
        LearningArticleRepository repository = (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByUrlOrderByCreatedAtDesc" -> {
                        lookupUrl.set((String) args[0]);
                        yield Optional.of(existing);
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LearningArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        ManualLearningArticleService service = new ManualLearningArticleService(
                null,
                null,
                repository,
                null,
                Clock.systemUTC()
        );

        service.submitUrl("https://medium.com/@author/article#id_token=secret-token");

        assertThat(lookupUrl).hasValue("https://medium.com/@author/article");
    }

    @Test
    void submitUrl_shouldReuseExistingManualCandidateWhenLearningArticleWasDeleted() {
        String url = "https://medium.com/@author/article";
        CandidateBatchEntity batch = manualBatch();
        CandidateArticleEntity existingCandidate = candidateArticle(batch, url);
        AtomicReference<LearningArticleEntity> savedArticle = new AtomicReference<>();

        CandidateBatchRepository batchRepository = (CandidateBatchRepository) Proxy.newProxyInstance(
                CandidateBatchRepository.class.getClassLoader(),
                new Class[]{CandidateBatchRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByRunDate" -> Optional.of(batch);
                    case "save" -> args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "CandidateBatchRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        CandidateArticleRepository candidateRepository = (CandidateArticleRepository) Proxy.newProxyInstance(
                CandidateArticleRepository.class.getClassLoader(),
                new Class[]{CandidateArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByBatchRunDateAndUrlOrderByCreatedAtAsc" -> Optional.of(existingCandidate);
                    case "save" -> throw new AssertionError("should reuse the existing manual candidate");
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "CandidateArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        LearningArticleRepository learningRepository = (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByUrlOrderByCreatedAtDesc" -> Optional.empty();
                    case "save" -> {
                        LearningArticleEntity article = (LearningArticleEntity) args[0];
                        ReflectionTestUtils.setField(article, "id", 88L);
                        savedArticle.set(article);
                        yield article;
                    }
                    case "findById" -> Optional.ofNullable(savedArticle.get());
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LearningArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        LearningWorkflowService workflowService = new LearningWorkflowService(
                learningRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                candidateRepository,
                null,
                Clock.fixed(Instant.parse("2026-06-07T00:00:00Z"), ZoneOffset.UTC)
        ) {
            @Override
            public LearningArticleEntity processLearningArticle(Long learningArticleId) {
                return savedArticle.get();
            }
        };
        ManualLearningArticleService service = new ManualLearningArticleService(
                batchRepository,
                candidateRepository,
                learningRepository,
                workflowService,
                Clock.fixed(Instant.parse("2026-06-07T00:00:00Z"), ZoneOffset.UTC)
        );

        service.submitUrl(url);

        assertThat(savedArticle.get().getCandidateArticle()).isSameAs(existingCandidate);
        assertThat(existingCandidate.isSelected()).isTrue();
        assertThat(batch.getCandidateCount()).isEqualTo(1);
    }

    private LearningArticleEntity learningArticle() {
        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidateArticle, "id", 7L);

        LearningArticleEntity article = new LearningArticleEntity();
        ReflectionTestUtils.setField(article, "id", 88L);
        article.setCandidateArticle(candidateArticle);
        article.setStatus("VOCAB_READY");
        article.setUrl("https://medium.com/@author/article");
        return article;
    }

    private CandidateBatchEntity manualBatch() {
        CandidateBatchEntity batch = new CandidateBatchEntity();
        ReflectionTestUtils.setField(batch, "id", 7L);
        batch.setRunDate(LocalDate.of(1970, 1, 1));
        batch.setStatus("MANUAL");
        batch.setSourceCount(1);
        batch.setCandidateCount(1);
        return batch;
    }

    private CandidateArticleEntity candidateArticle(CandidateBatchEntity batch, String url) {
        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidateArticle, "id", 13L);
        candidateArticle.setBatch(batch);
        candidateArticle.setTitle("Existing manual article");
        candidateArticle.setUrl(url);
        candidateArticle.setSource("Manual");
        candidateArticle.setSummary("User submitted article.");
        candidateArticle.setRankOrder(1);
        candidateArticle.setSelected(false);
        return candidateArticle;
    }
}
