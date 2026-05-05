package com.ailearn.service.candidate;

import com.ailearn.config.AppConfig;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.CandidateBatchEntity;
import com.ailearn.model.FeedArticle;
import com.ailearn.model.RankedCandidate;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.CandidateBatchRepository;
import com.ailearn.service.rss.RssFetchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateGenerationServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void generateToday_shouldPersistCompletedBatchAndRankedArticles() {
        AtomicReference<CandidateBatchEntity> savedBatch = new AtomicReference<>();
        AtomicReference<List<CandidateArticleEntity>> savedArticles = new AtomicReference<>(List.of());
        AtomicLong idSequence = new AtomicLong(1L);

        CandidateBatchRepository batchRepository = batchRepositoryProxy(savedBatch, idSequence, Optional.empty());
        CandidateArticleRepository articleRepository = articleRepositoryProxy(
                savedArticles,
                new AtomicReference<>(null),
                new AtomicReference<>(List.of())
        );

        CandidateGenerationService service = new CandidateGenerationService(
                rssService(List.of(
                        article("A", "https://example.com/a"),
                        article("B", "https://example.com/b")
                )),
                coarseFilter(List.of(article("A", "https://example.com/a"))),
                rerankService(List.of(ranked("A", "https://example.com/a", 8.7d))),
                batchRepository,
                articleRepository,
                clock
        );

        CandidateBatchEntity batch = service.generateToday();

        assertThat(batch.getRunDate()).isEqualTo(LocalDate.of(2026, 4, 26));
        assertThat(batch.getStatus()).isEqualTo(CandidateGenerationService.STATUS_COMPLETED);
        assertThat(batch.getSourceCount()).isEqualTo(2);
        assertThat(batch.getCandidateCount()).isEqualTo(1);
        assertThat(savedArticles.get()).hasSize(1);
        assertThat(savedArticles.get().getFirst().getRankOrder()).isEqualTo(1);
        assertThat(savedArticles.get().getFirst().getLlmScore()).isEqualTo(8.7d);
        assertThat(savedArticles.get().getFirst().getChineseTitle()).isEqualTo("中文 A");
        assertThat(savedArticles.get().getFirst().getChineseSummary()).isEqualTo("中文摘要 A");
        assertThat(savedArticles.get().getFirst().getBatch()).isSameAs(savedBatch.get());
    }

    @Test
    void generateToday_shouldMarkBatchFailedWhenPipelineThrows() {
        AtomicReference<CandidateBatchEntity> savedBatch = new AtomicReference<>();
        AtomicReference<Long> deletedBatchId = new AtomicReference<>();
        AtomicLong idSequence = new AtomicLong(42L);

        CandidateBatchEntity existingBatch = new CandidateBatchEntity();
        ReflectionTestUtils.setField(existingBatch, "id", 42L);
        existingBatch.setRunDate(LocalDate.of(2026, 4, 26));

        CandidateBatchRepository batchRepository = batchRepositoryProxy(savedBatch, idSequence, Optional.of(existingBatch));
        CandidateArticleRepository articleRepository = articleRepositoryProxy(
                new AtomicReference<>(List.of()),
                deletedBatchId,
                new AtomicReference<>(List.of())
        );

        CandidateGenerationService service = new CandidateGenerationService(
                failingRssService(new IllegalStateException("feed unavailable")),
                coarseFilter(List.of()),
                rerankService(List.of()),
                batchRepository,
                articleRepository,
                clock
        );

        assertThatThrownBy(service::generateToday)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("feed unavailable");

        assertThat(deletedBatchId.get()).isEqualTo(42L);
        assertThat(savedBatch.get().getStatus()).isEqualTo(CandidateGenerationService.STATUS_FAILED);
    }

    @Test
    void generateToday_shouldPreserveReferencedCandidatesAndSkipDuplicateUrls() {
        AtomicReference<CandidateBatchEntity> savedBatch = new AtomicReference<>();
        AtomicReference<List<CandidateArticleEntity>> savedArticles = new AtomicReference<>(List.of());
        AtomicReference<Long> deletedBatchId = new AtomicReference<>();
        AtomicLong idSequence = new AtomicLong(42L);

        CandidateBatchEntity existingBatch = new CandidateBatchEntity();
        ReflectionTestUtils.setField(existingBatch, "id", 42L);
        existingBatch.setRunDate(LocalDate.of(2026, 4, 26));

        CandidateArticleEntity preserved = new CandidateArticleEntity();
        ReflectionTestUtils.setField(preserved, "id", 7L);
        preserved.setBatch(existingBatch);
        preserved.setTitle("Kept");
        preserved.setUrl("https://example.com/kept");
        preserved.setRankOrder(1);

        CandidateBatchRepository batchRepository = batchRepositoryProxy(savedBatch, idSequence, Optional.of(existingBatch));
        CandidateArticleRepository articleRepository = articleRepositoryProxy(
                savedArticles,
                deletedBatchId,
                new AtomicReference<>(List.of(preserved))
        );

        CandidateGenerationService service = new CandidateGenerationService(
                rssService(List.of(
                        article("Kept", "https://example.com/kept"),
                        article("Fresh", "https://example.com/fresh")
                )),
                coarseFilter(List.of(
                        article("Kept", "https://example.com/kept"),
                        article("Fresh", "https://example.com/fresh")
                )),
                rerankService(List.of(
                        ranked("Kept", "https://example.com/kept", 9.1d),
                        ranked("Fresh", "https://example.com/fresh", 8.6d)
                )),
                batchRepository,
                articleRepository,
                clock
        );

        CandidateBatchEntity batch = service.generateToday();

        assertThat(deletedBatchId.get()).isEqualTo(42L);
        assertThat(savedArticles.get()).hasSize(1);
        assertThat(savedArticles.get().getFirst().getUrl()).isEqualTo("https://example.com/fresh");
        assertThat(savedArticles.get().getFirst().getRankOrder()).isEqualTo(2);
        assertThat(batch.getCandidateCount()).isEqualTo(2);
    }

    private CandidateBatchRepository batchRepositoryProxy(
            AtomicReference<CandidateBatchEntity> savedBatch,
            AtomicLong idSequence,
            Optional<CandidateBatchEntity> existingBatch
    ) {
        return (CandidateBatchRepository) Proxy.newProxyInstance(
                CandidateBatchRepository.class.getClassLoader(),
                new Class[]{CandidateBatchRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByRunDate" -> existingBatch;
                    case "save" -> {
                        CandidateBatchEntity batch = (CandidateBatchEntity) args[0];
                        if (batch.getId() == null) {
                            ReflectionTestUtils.setField(batch, "id", idSequence.getAndIncrement());
                        }
                        savedBatch.set(batch);
                        yield batch;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "CandidateBatchRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private CandidateArticleRepository articleRepositoryProxy(
            AtomicReference<List<CandidateArticleEntity>> savedArticles,
            AtomicReference<Long> deletedBatchId,
            AtomicReference<List<CandidateArticleEntity>> existingArticles
    ) {
        return (CandidateArticleRepository) Proxy.newProxyInstance(
                CandidateArticleRepository.class.getClassLoader(),
                new Class[]{CandidateArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "saveAll" -> {
                        @SuppressWarnings("unchecked")
                        List<CandidateArticleEntity> articles = new ArrayList<>((List<CandidateArticleEntity>) args[0]);
                        savedArticles.set(articles);
                        yield articles;
                    }
                    case "deleteUnreferencedByBatchId" -> {
                        deletedBatchId.set((Long) args[0]);
                        yield null;
                    }
                    case "findByBatchIdOrderByRankOrderAscCreatedAtAsc" -> existingArticles.get();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "CandidateArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private RssFetchService rssService(List<FeedArticle> articles) {
        return new RssFetchService(new AppConfig()) {
            @Override
            public List<FeedArticle> fetchAll() {
                return articles;
            }
        };
    }

    private RssFetchService failingRssService(RuntimeException exception) {
        return new RssFetchService(new AppConfig()) {
            @Override
            public List<FeedArticle> fetchAll() {
                throw exception;
            }
        };
    }

    private CandidateCoarseFilter coarseFilter(List<FeedArticle> filteredArticles) {
        return new CandidateCoarseFilter(new AppConfig(), Clock.systemUTC()) {
            @Override
            public List<FeedArticle> filter(List<FeedArticle> articles) {
                return filteredArticles;
            }
        };
    }

    private CandidateRerankService rerankService(List<RankedCandidate> rankedCandidates) {
        return new CandidateRerankService(new AppConfig(), new ObjectMapper(), prompt -> "", "Prompt {{articles}}") {
            @Override
            public List<RankedCandidate> rerank(List<FeedArticle> articles) {
                return rankedCandidates;
            }
        };
    }

    private FeedArticle article(String title, String url) {
        return new FeedArticle(title, url, "Test", "Summary", LocalDateTime.parse("2026-04-26T09:00:00"));
    }

    private RankedCandidate ranked(String title, String url, double score) {
        return new RankedCandidate(
                title,
                "中文 " + title,
                url,
                "Test",
                "Summary",
                "中文摘要 " + title,
                LocalDateTime.parse("2026-04-26T09:00:00"),
                score,
                "Strong fit"
        );
    }
}
