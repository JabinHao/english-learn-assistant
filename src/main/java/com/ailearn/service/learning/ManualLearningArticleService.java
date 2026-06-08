package com.ailearn.service.learning;

import com.ailearn.api.learning.SelectCandidateResponse;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.CandidateBatchEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.CandidateBatchRepository;
import com.ailearn.repository.LearningArticleRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ManualLearningArticleService {

    private static final LocalDate MANUAL_BATCH_DATE = LocalDate.of(1970, 1, 1);
    private static final String MANUAL_SOURCE = "Manual";
    private static final String MANUAL_BATCH_STATUS = "MANUAL";
    private static final String MANUAL_SUMMARY = "User submitted article.";

    private final CandidateBatchRepository candidateBatchRepository;
    private final CandidateArticleRepository candidateArticleRepository;
    private final LearningArticleRepository learningArticleRepository;
    private final LearningWorkflowService learningWorkflowService;
    private final Clock clock;

    public ManualLearningArticleService(
            CandidateBatchRepository candidateBatchRepository,
            CandidateArticleRepository candidateArticleRepository,
            LearningArticleRepository learningArticleRepository,
            LearningWorkflowService learningWorkflowService,
            Clock clock
    ) {
        this.candidateBatchRepository = candidateBatchRepository;
        this.candidateArticleRepository = candidateArticleRepository;
        this.learningArticleRepository = learningArticleRepository;
        this.learningWorkflowService = learningWorkflowService;
        this.clock = clock;
    }

    @Transactional
    public SelectCandidateResponse submitUrl(String url) {
        String normalizedUrl = normalizeUrl(url);
        return learningArticleRepository.findFirstByUrlOrderByCreatedAtDesc(normalizedUrl)
                .map(this::toResponse)
                .orElseGet(() -> createAndProcess(normalizedUrl));
    }

    private SelectCandidateResponse createAndProcess(String url) {
        CandidateBatchEntity batch = candidateBatchRepository.findByRunDate(MANUAL_BATCH_DATE)
                .orElseGet(this::createManualBatch);

        CandidateArticleEntity savedCandidate = candidateArticleRepository
                .findFirstByBatchRunDateAndUrlOrderByCreatedAtAsc(MANUAL_BATCH_DATE, url)
                .orElseGet(() -> createManualCandidate(batch, url));
        savedCandidate.setSelected(true);

        LearningArticleEntity savedLearningArticle = createLearningArticle(savedCandidate);

        LearningArticleEntity processed = learningWorkflowService.processLearningArticle(savedLearningArticle.getId());
        return toResponse(processed);
    }

    private CandidateArticleEntity createManualCandidate(CandidateBatchEntity batch, String url) {
        int nextRank = batch.getCandidateCount() + 1;
        batch.setCandidateCount(nextRank);
        candidateBatchRepository.save(batch);

        CandidateArticleEntity candidate = new CandidateArticleEntity();
        candidate.setBatch(batch);
        candidate.setTitle(titleFromUrl(url));
        candidate.setUrl(url);
        candidate.setSource(MANUAL_SOURCE);
        candidate.setPublishedAt(LocalDateTime.now(clock));
        candidate.setSummary(MANUAL_SUMMARY);
        candidate.setLlmReason("Submitted manually by URL.");
        candidate.setRankOrder(nextRank);
        candidate.setSelected(true);
        return candidateArticleRepository.save(candidate);
    }

    private LearningArticleEntity createLearningArticle(CandidateArticleEntity savedCandidate) {
        LearningArticleEntity learningArticle = new LearningArticleEntity();
        learningArticle.setCandidateArticle(savedCandidate);
        learningArticle.setStatus(CandidateSelectionService.STATUS_SELECTED);
        learningArticle.setTitle(savedCandidate.getTitle());
        learningArticle.setUrl(savedCandidate.getUrl());
        learningArticle.setSource(savedCandidate.getSource());
        learningArticle.setPublishedAt(savedCandidate.getPublishedAt());
        learningArticle.setSummary(savedCandidate.getSummary());
        learningArticle.setSelectedAt(LocalDateTime.now(clock));
        return learningArticleRepository.save(learningArticle);
    }

    private CandidateBatchEntity createManualBatch() {
        CandidateBatchEntity batch = new CandidateBatchEntity();
        batch.setRunDate(MANUAL_BATCH_DATE);
        batch.setStatus(MANUAL_BATCH_STATUS);
        batch.setSourceCount(1);
        batch.setCandidateCount(0);
        return candidateBatchRepository.save(batch);
    }

    private SelectCandidateResponse toResponse(LearningArticleEntity article) {
        return new SelectCandidateResponse(
                article.getId(),
                article.getCandidateArticle().getId(),
                article.getStatus()
        );
    }

    private String normalizeUrl(String url) {
        try {
            URI uri = new URI(url.strip());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article URL must use http or https");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article URL must include a host");
            }
            URI withoutFragment = new URI(
                    scheme.toLowerCase(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            );
            return withoutFragment.toString();
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article URL is invalid", exception);
        }
    }

    private String titleFromUrl(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        return host == null || host.isBlank() ? "Manual article" : "Manual article from " + host;
    }
}
