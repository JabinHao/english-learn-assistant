package com.ailearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_article")
public class CandidateArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private CandidateBatchEntity batch;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String chineseTitle;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false, length = 200)
    private String source;

    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String chineseSummary;

    @Column(columnDefinition = "TEXT")
    private String coarseFilterReason;

    private Double llmScore;

    @Column(columnDefinition = "TEXT")
    private String llmReason;

    private Integer rankOrder;

    @Column(nullable = false)
    private boolean selected;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public CandidateBatchEntity getBatch() {
        return batch;
    }

    public void setBatch(CandidateBatchEntity batch) {
        this.batch = batch;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChineseTitle() {
        return chineseTitle;
    }

    public void setChineseTitle(String chineseTitle) {
        this.chineseTitle = chineseTitle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getChineseSummary() {
        return chineseSummary;
    }

    public void setChineseSummary(String chineseSummary) {
        this.chineseSummary = chineseSummary;
    }

    public String getCoarseFilterReason() {
        return coarseFilterReason;
    }

    public void setCoarseFilterReason(String coarseFilterReason) {
        this.coarseFilterReason = coarseFilterReason;
    }

    public Double getLlmScore() {
        return llmScore;
    }

    public void setLlmScore(Double llmScore) {
        this.llmScore = llmScore;
    }

    public String getLlmReason() {
        return llmReason;
    }

    public void setLlmReason(String llmReason) {
        this.llmReason = llmReason;
    }

    public Integer getRankOrder() {
        return rankOrder;
    }

    public void setRankOrder(Integer rankOrder) {
        this.rankOrder = rankOrder;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
