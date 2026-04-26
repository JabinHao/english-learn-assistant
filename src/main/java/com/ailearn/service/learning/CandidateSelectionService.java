package com.ailearn.service.learning;

import com.ailearn.api.learning.SelectCandidateResponse;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.repository.LearningArticleRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class CandidateSelectionService {

    public static final String STATUS_SELECTED = "SELECTED";

    private final CandidateArticleRepository candidateArticleRepository;
    private final LearningArticleRepository learningArticleRepository;
    private final LearningWorkflowService learningWorkflowService;
    private final Clock clock;

    public CandidateSelectionService(
            CandidateArticleRepository candidateArticleRepository,
            LearningArticleRepository learningArticleRepository,
            LearningWorkflowService learningWorkflowService,
            Clock clock
    ) {
        this.candidateArticleRepository = candidateArticleRepository;
        this.learningArticleRepository = learningArticleRepository;
        this.learningWorkflowService = learningWorkflowService;
        this.clock = clock;
    }

    @Transactional
    public SelectCandidateResponse selectCandidate(Long candidateArticleId) {
        CandidateArticleEntity candidate = candidateArticleRepository.findById(candidateArticleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate article not found"));

        candidateArticleRepository.clearSelectedByBatchId(candidate.getBatch().getId());
        candidate.setSelected(true);
        candidateArticleRepository.save(candidate);

        LearningArticleEntity learningArticle = learningArticleRepository.findByCandidateArticleId(candidateArticleId)
                .orElseGet(() -> createLearningArticle(candidate));

        if (learningArticle.getId() == null) {
            learningArticle = learningArticleRepository.save(learningArticle);
        }

        learningWorkflowService.processLearningArticle(learningArticle.getId());

        return new SelectCandidateResponse(
                learningArticle.getId(),
                candidate.getId(),
                learningArticle.getStatus()
        );
    }

    private LearningArticleEntity createLearningArticle(CandidateArticleEntity candidate) {
        LearningArticleEntity learningArticle = new LearningArticleEntity();
        learningArticle.setCandidateArticle(candidate);
        learningArticle.setStatus(STATUS_SELECTED);
        learningArticle.setTitle(candidate.getTitle());
        learningArticle.setUrl(candidate.getUrl());
        learningArticle.setSource(candidate.getSource());
        learningArticle.setPublishedAt(candidate.getPublishedAt());
        learningArticle.setSummary(candidate.getSummary());
        learningArticle.setSelectedAt(LocalDateTime.now(clock));
        return learningArticle;
    }
}
