package com.ailearn.controller;

import com.ailearn.api.candidate.CandidateArticleResponse;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.repository.CandidateArticleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateArticleRepository candidateArticleRepository;
    private final Clock clock;

    public CandidateController(CandidateArticleRepository candidateArticleRepository, Clock clock) {
        this.candidateArticleRepository = candidateArticleRepository;
        this.clock = clock;
    }

    @GetMapping("/today")
    public List<CandidateArticleResponse> getTodayCandidates() {
        LocalDate runDate = LocalDate.now(clock);
        return candidateArticleRepository.findByBatchRunDateOrderByRankOrderAscCreatedAtAsc(runDate).stream()
                .map(this::toResponse)
                .toList();
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
