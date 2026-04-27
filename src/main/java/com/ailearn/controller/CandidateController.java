package com.ailearn.controller;

import com.ailearn.api.candidate.CandidateArticleResponse;
import com.ailearn.api.learning.SelectCandidateResponse;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.service.candidate.CandidateGenerationService;
import com.ailearn.service.learning.CandidateSelectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        candidateGenerationService.generateToday();
        return getTodayCandidates();
    }

    @PostMapping("/{candidateId}/select")
    public SelectCandidateResponse selectCandidate(@PathVariable Long candidateId) {
        return candidateSelectionService.selectCandidate(candidateId);
    }

    private CandidateArticleResponse toResponse(CandidateArticleEntity entity) {
        return new CandidateArticleResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getUrl(),
                entity.getSource(),
                entity.getPublishedAt(),
                entity.getSummary(),
                entity.getLlmScore(),
                entity.getLlmReason(),
                entity.isSelected()
        );
    }
}
