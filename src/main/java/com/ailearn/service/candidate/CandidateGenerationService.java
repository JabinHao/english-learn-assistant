package com.ailearn.service.candidate;

import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.CandidateBatchEntity;
import com.ailearn.model.FeedArticle;
import com.ailearn.model.RankedCandidate;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.CandidateBatchRepository;
import com.ailearn.service.rss.RssFetchService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CandidateGenerationService {

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
        CandidateBatchEntity batch = candidateBatchRepository.findByRunDate(runDate)
                .orElseGet(CandidateBatchEntity::new);
        batch.setRunDate(runDate);
        batch.setStatus(STATUS_RUNNING);
        batch.setSourceCount(0);
        batch.setCandidateCount(0);
        batch = candidateBatchRepository.save(batch);

        try {
            if (batch.getId() != null) {
                candidateArticleRepository.deleteByBatchId(batch.getId());
            }

            List<FeedArticle> fetchedArticles = rssFetchService.fetchAll();
            List<FeedArticle> filteredArticles = candidateCoarseFilter.filter(fetchedArticles);
            List<RankedCandidate> rankedCandidates = candidateRerankService.rerank(filteredArticles);

            List<CandidateArticleEntity> candidateEntities = toEntities(batch, rankedCandidates);
            if (!candidateEntities.isEmpty()) {
                candidateArticleRepository.saveAll(candidateEntities);
            }

            batch.setSourceCount(fetchedArticles.size());
            batch.setCandidateCount(candidateEntities.size());
            batch.setStatus(STATUS_COMPLETED);
            return candidateBatchRepository.save(batch);
        } catch (RuntimeException exception) {
            batch.setStatus(STATUS_FAILED);
            candidateBatchRepository.save(batch);
            throw exception;
        }
    }

    private List<CandidateArticleEntity> toEntities(CandidateBatchEntity batch, List<RankedCandidate> rankedCandidates) {
        List<CandidateArticleEntity> entities = new ArrayList<>();
        for (int index = 0; index < rankedCandidates.size(); index++) {
            RankedCandidate rankedCandidate = rankedCandidates.get(index);
            CandidateArticleEntity entity = new CandidateArticleEntity();
            entity.setBatch(batch);
            entity.setTitle(rankedCandidate.title());
            entity.setUrl(rankedCandidate.url());
            entity.setSource(rankedCandidate.source());
            entity.setPublishedAt(rankedCandidate.publishedAt());
            entity.setSummary(rankedCandidate.summary());
            entity.setCoarseFilterReason("passed coarse filter");
            entity.setLlmScore(rankedCandidate.score());
            entity.setLlmReason(rankedCandidate.reason());
            entity.setRankOrder(index + 1);
            entity.setSelected(false);
            entities.add(entity);
        }
        return entities;
    }
}
