package com.ailearn.service.candidate;

import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.CandidateBatchEntity;
import com.ailearn.model.FeedArticle;
import com.ailearn.model.RankedCandidate;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.CandidateBatchRepository;
import com.ailearn.service.rss.RssFetchService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CandidateGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CandidateGenerationService.class);

    static final String STATUS_RUNNING = "RUNNING";
    static final String STATUS_COMPLETED = "COMPLETED";
    static final String STATUS_FAILED = "FAILED";

    private final RssFetchService rssFetchService;
    private final CandidateCoarseFilter candidateCoarseFilter;
    private final CandidateRerankService candidateRerankService;
    private final CandidateBatchRepository candidateBatchRepository;
    private final CandidateArticleRepository candidateArticleRepository;
    private final Clock clock;

    public CandidateGenerationService(
            RssFetchService rssFetchService,
            CandidateCoarseFilter candidateCoarseFilter,
            CandidateRerankService candidateRerankService,
            CandidateBatchRepository candidateBatchRepository,
            CandidateArticleRepository candidateArticleRepository,
            Clock clock
    ) {
        this.rssFetchService = rssFetchService;
        this.candidateCoarseFilter = candidateCoarseFilter;
        this.candidateRerankService = candidateRerankService;
        this.candidateBatchRepository = candidateBatchRepository;
        this.candidateArticleRepository = candidateArticleRepository;
        this.clock = clock;
    }

    @Transactional
    public CandidateBatchEntity generateToday() {
        LocalDate runDate = LocalDate.now(clock);
        log.info("candidate.generation.start runDate={}", runDate);
        CandidateBatchEntity batch = candidateBatchRepository.findByRunDate(runDate)
                .orElseGet(CandidateBatchEntity::new);
        batch.setRunDate(runDate);
        batch.setStatus(STATUS_RUNNING);
        batch.setSourceCount(0);
        batch.setCandidateCount(0);
        batch = candidateBatchRepository.save(batch);

        try {
            List<CandidateArticleEntity> preservedArticles = List.of();
            if (batch.getId() != null) {
                candidateArticleRepository.deleteUnreferencedByBatchId(batch.getId());
                preservedArticles = candidateArticleRepository.findByBatchIdOrderByRankOrderAscCreatedAtAsc(batch.getId());
            }

            List<FeedArticle> fetchedArticles = rssFetchService.fetchAll();
            List<FeedArticle> filteredArticles = candidateCoarseFilter.filter(fetchedArticles);
            List<RankedCandidate> rankedCandidates = candidateRerankService.rerank(filteredArticles);
            List<RankedCandidate> newRankedCandidates = excludeExistingUrls(rankedCandidates, preservedArticles);

            List<CandidateArticleEntity> candidateEntities = toEntities(batch, newRankedCandidates, preservedArticles.size() + 1);
            if (!candidateEntities.isEmpty()) {
                candidateArticleRepository.saveAll(candidateEntities);
            }

            batch.setSourceCount(fetchedArticles.size());
            batch.setCandidateCount(preservedArticles.size() + candidateEntities.size());
            batch.setStatus(STATUS_COMPLETED);
            log.info(
                    "candidate.generation.completed runDate={} sourceCount={} filteredCount={} preservedCount={} candidateCount={}",
                    runDate,
                    fetchedArticles.size(),
                    filteredArticles.size(),
                    preservedArticles.size(),
                    preservedArticles.size() + candidateEntities.size()
            );
            return candidateBatchRepository.save(batch);
        } catch (RuntimeException exception) {
            batch.setStatus(STATUS_FAILED);
            candidateBatchRepository.save(batch);
            log.error("candidate.generation.failed runDate={} error={}", runDate, exception.getMessage(), exception);
            throw exception;
        }
    }

    private List<RankedCandidate> excludeExistingUrls(
            List<RankedCandidate> rankedCandidates,
            List<CandidateArticleEntity> preservedArticles
    ) {
        Set<String> existingUrls = new HashSet<>();
        for (CandidateArticleEntity preservedArticle : preservedArticles) {
            existingUrls.add(preservedArticle.getUrl());
        }

        return rankedCandidates.stream()
                .filter(candidate -> !existingUrls.contains(candidate.url()))
                .toList();
    }

    private List<CandidateArticleEntity> toEntities(
            CandidateBatchEntity batch,
            List<RankedCandidate> rankedCandidates,
            int startingRankOrder
    ) {
        List<CandidateArticleEntity> entities = new ArrayList<>();
        for (int index = 0; index < rankedCandidates.size(); index++) {
            RankedCandidate rankedCandidate = rankedCandidates.get(index);
            CandidateArticleEntity entity = new CandidateArticleEntity();
            entity.setBatch(batch);
            entity.setTitle(rankedCandidate.title());
            entity.setChineseTitle(rankedCandidate.chineseTitle());
            entity.setUrl(rankedCandidate.url());
            entity.setSource(rankedCandidate.source());
            entity.setPublishedAt(rankedCandidate.publishedAt());
            entity.setSummary(rankedCandidate.summary());
            entity.setChineseSummary(rankedCandidate.chineseSummary());
            entity.setCoarseFilterReason("passed coarse filter");
            entity.setLlmScore(rankedCandidate.score());
            entity.setLlmReason(rankedCandidate.reason());
            entity.setRankOrder(startingRankOrder + index);
            entity.setSelected(false);
            entities.add(entity);
        }
        return entities;
    }
}
