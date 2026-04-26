package com.ailearn.repository;

import com.ailearn.entity.CandidateArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CandidateArticleRepository extends JpaRepository<CandidateArticleEntity, Long> {

    List<CandidateArticleEntity> findByBatchRunDateOrderByRankOrderAscCreatedAtAsc(java.time.LocalDate runDate);

    void deleteByBatchId(Long batchId);

    @Modifying
    @Query("update CandidateArticleEntity c set c.selected = false where c.batch.id = :batchId")
    void clearSelectedByBatchId(Long batchId);
}
