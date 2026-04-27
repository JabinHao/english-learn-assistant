package com.ailearn.scheduler;

import com.ailearn.service.candidate.CandidateGenerationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CandidateGenerationScheduler {

    private final CandidateGenerationService candidateGenerationService;

    public CandidateGenerationScheduler(CandidateGenerationService candidateGenerationService) {
        this.candidateGenerationService = candidateGenerationService;
    }

    @Scheduled(cron = "${ailearn.candidate.cron}")
    public void generateDailyCandidates() {
        candidateGenerationService.generateToday();
    }
}
