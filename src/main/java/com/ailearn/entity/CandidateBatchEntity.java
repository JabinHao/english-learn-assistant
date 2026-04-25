package com.ailearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_batch")
public class CandidateBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate runDate;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private Integer sourceCount = 0;

    @Column(nullable = false)
    private Integer candidateCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(Integer sourceCount) {
        this.sourceCount = sourceCount;
    }

    public Integer getCandidateCount() {
        return candidateCount;
    }

    public void setCandidateCount(Integer candidateCount) {
        this.candidateCount = candidateCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
