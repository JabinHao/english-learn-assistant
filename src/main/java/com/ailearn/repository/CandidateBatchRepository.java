package com.ailearn.repository;

import com.ailearn.entity.CandidateBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CandidateBatchRepository extends JpaRepository<CandidateBatchEntity, Long> {

    Optional<CandidateBatchEntity> findByRunDate(LocalDate runDate);
}
