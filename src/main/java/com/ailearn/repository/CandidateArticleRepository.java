package com.ailearn.repository;

import com.ailearn.entity.CandidateArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateArticleRepository extends JpaRepository<CandidateArticleEntity, Long> {

    List<CandidateArticleEntity> findByBatchRunDateOrderByRankOrderAscCreatedAtAsc(java.time.LocalDate runDate);
}
