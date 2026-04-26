package com.ailearn.controller;

import com.ailearn.api.learning.SelectCandidateResponse;
import com.ailearn.repository.CandidateArticleRepository;
import com.ailearn.service.learning.CandidateSelectionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateSelectionControllerTest {

    @Test
    void selectCandidate_shouldReturnLearningArticleReference() throws Exception {
        CandidateSelectionService selectionService = new CandidateSelectionService(
                repository(),
                null,
                Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC)
        ) {
            @Override
            public SelectCandidateResponse selectCandidate(Long candidateArticleId) {
                return new SelectCandidateResponse(101L, candidateArticleId, "SELECTED");
            }
        };

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CandidateController(
                        repository(),
                        selectionService,
                        Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/candidates/55/select"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learningArticleId").value(101))
                .andExpect(jsonPath("$.candidateArticleId").value(55))
                .andExpect(jsonPath("$.status").value("SELECTED"));
    }

    @Test
    void selectCandidate_shouldReturnNotFoundWhenServiceRejects() throws Exception {
        CandidateSelectionService selectionService = new CandidateSelectionService(
                repository(),
                null,
                Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC)
        ) {
            @Override
            public SelectCandidateResponse selectCandidate(Long candidateArticleId) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Candidate article not found");
            }
        };

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CandidateController(
                        repository(),
                        selectionService,
                        Clock.fixed(Instant.parse("2026-04-26T00:00:00Z"), ZoneOffset.UTC)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/candidates/999/select"))
                .andExpect(status().isNotFound());
    }

    private CandidateArticleRepository repository() {
        return (CandidateArticleRepository) Proxy.newProxyInstance(
                CandidateArticleRepository.class.getClassLoader(),
                new Class[]{CandidateArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByBatchRunDateOrderByRankOrderAscCreatedAtAsc" -> List.of();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "CandidateArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
