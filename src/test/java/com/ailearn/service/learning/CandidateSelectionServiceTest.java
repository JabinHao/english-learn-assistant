package com.ailearn.service.learning;

import com.ailearn.api.learning.SelectCandidateResponse;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.CandidateBatchEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.LearningArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateSelectionServiceTest {

    @Test
    void selectCandidate_shouldCreateLearningArticleAndMarkCandidateSelected() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC);
        AtomicReference<CandidateArticleEntity> savedCandidate = new AtomicReference<>();
        AtomicReference<Long> clearedBatchId = new AtomicReference<>();
        AtomicReference<LearningArticleEntity> savedLearningArticle = new AtomicReference<>();
        AtomicLong learningIdSequence = new AtomicLong(101L);

        CandidateBatchEntity batch = new CandidateBatchEntity();
        ReflectionTestUtils.setField(batch, "id", 22L);
        CandidateArticleEntity candidate = candidate(7L, batch);

        CandidateArticleRepository candidateRepository = candidateRepository(candidate, savedCandidate, clearedBatchId);
        LearningArticleRepository learningRepository = learningRepository(Optional.empty(), savedLearningArticle, learningIdSequence);
        LearningWorkflowService workflowService = workflowService();

        CandidateSelectionService service = new CandidateSelectionService(candidateRepository, learningRepository, workflowService, clock);
        SelectCandidateResponse response = service.selectCandidate(7L);

        assertThat(clearedBatchId.get()).isEqualTo(22L);
        assertThat(savedCandidate.get().isSelected()).isTrue();
        assertThat(savedLearningArticle.get().getStatus()).isEqualTo("SELECTED");
        assertThat(savedLearningArticle.get().getSelectedAt()).isEqualTo(LocalDateTime.of(2026, 4, 26, 0, 0));
        assertThat(response.learningArticleId()).isEqualTo(101L);
        assertThat(response.candidateArticleId()).isEqualTo(7L);
    }

    @Test
    void selectCandidate_shouldFailWhenCandidateDoesNotExist() {
        CandidateArticleRepository candidateRepository = candidateRepository(null, new AtomicReference<>(), new AtomicReference<>());
        LearningArticleRepository learningRepository = learningRepository(Optional.empty(), new AtomicReference<>(), new AtomicLong(1L));
        LearningWorkflowService workflowService = workflowService();

        CandidateSelectionService service = new CandidateSelectionService(
                candidateRepository,
                learningRepository,
                workflowService,
                Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> service.selectCandidate(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private CandidateArticleRepository candidateRepository(
            CandidateArticleEntity candidate,
            AtomicReference<CandidateArticleEntity> savedCandidate,
            AtomicReference<Long> clearedBatchId
    ) {
        return (CandidateArticleRepository) Proxy.newProxyInstance(
                CandidateArticleRepository.class.getClassLoader(),
                new Class[]{CandidateArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.ofNullable(candidate);
                    case "clearSelectedByBatchId" -> {
                        clearedBatchId.set((Long) args[0]);
                        yield null;
                    }
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

    private LearningArticleRepository learningRepository(
            Optional<LearningArticleEntity> existing,
            AtomicReference<LearningArticleEntity> savedLearningArticle,
            AtomicLong idSequence
    ) {
        return (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByCandidateArticleId" -> existing;
                    case "save" -> {
                        LearningArticleEntity value = (LearningArticleEntity) args[0];
                        if (value.getId() == null) {
                            ReflectionTestUtils.setField(value, "id", idSequence.getAndIncrement());
                        }
                        savedLearningArticle.set(value);
                        yield value;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LearningArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private CandidateArticleEntity candidate(Long id, CandidateBatchEntity batch) {
        CandidateArticleEntity candidate = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidate, "id", id);
        candidate.setBatch(batch);
        candidate.setTitle("Selected article");
        candidate.setUrl("https://example.com/selected");
        candidate.setSource("Test Feed");
        candidate.setPublishedAt(LocalDateTime.of(2026, 4, 26, 8, 30));
        candidate.setSummary("A useful AI article");
        return candidate;
    }

    private LearningWorkflowService workflowService() {
        return new LearningWorkflowService(null, null, null, null, null, null, null, null, Clock.systemUTC()) {
            @Override
            public LearningArticleEntity processLearningArticle(Long learningArticleId) {
                return null;
            }
        };
    }
}
