package com.ailearn.controller;

import com.ailearn.api.candidate.CandidateArticleResponse;
import com.ailearn.api.learning.SelectCandidateResponse;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.service.candidate.CandidateGenerationService;
import com.ailearn.service.learning.CandidateSelectionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateArticleRepository candidateArticleRepository;
    private final CandidateGenerationService candidateGenerationService;
    private final CandidateSelectionService candidateSelectionService;
    private final Clock clock;

    public CandidateController(
            CandidateArticleRepository candidateArticleRepository,
            CandidateGenerationService candidateGenerationService,
            CandidateSelectionService candidateSelectionService,
            Clock clock
    ) {
        this.candidateArticleRepository = candidateArticleRepository;
        this.candidateGenerationService = candidateGenerationService;
        this.candidateSelectionService = candidateSelectionService;
        this.clock = clock;
    }

    @GetMapping("/today")
    public List<CandidateArticleResponse> getTodayCandidates() {
        LocalDate runDate = LocalDate.now(clock);
        return candidateArticleRepository.findByBatchRunDateOrderByRankOrderAscCreatedAtAsc(runDate).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/generate")
    public List<CandidateArticleResponse> generateTodayCandidates() {
        try {
            candidateGenerationService.generateToday();
            return getTodayCandidates();
        } catch (RuntimeException exception) {
            if (causedBy(exception, HttpTimeoutException.class)) {
                throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Candidate generation timed out while calling the LLM provider",
                        exception
                );
            }
            throw exception;
        }
    }

    @PostMapping("/{candidateId}/select")
    public SelectCandidateResponse selectCandidate(@PathVariable Long candidateId) {
        return candidateSelectionService.selectCandidate(candidateId);
    }

    private CandidateArticleResponse toResponse(CandidateArticleEntity entity) {
        return new CandidateArticleResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getChineseTitle(),
                entity.getUrl(),
                entity.getSource(),
                entity.getPublishedAt(),
                entity.getSummary(),
                entity.getChineseSummary(),
                entity.getLlmScore(),
                entity.getLlmReason(),
                entity.isSelected()
        );
    }

    private boolean causedBy(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
