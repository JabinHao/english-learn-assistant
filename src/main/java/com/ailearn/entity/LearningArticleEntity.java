package com.ailearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_article")
public class LearningArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "candidate_article_id", nullable = false, unique = true)
    private CandidateArticleEntity candidateArticle;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false, length = 200)
    private String source;

    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String articleContent;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private LocalDateTime selectedAt;

    private LocalDateTime translatedAt;

    private LocalDateTime vocabularyExtractedAt;

    private LocalDateTime eudicPushedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public CandidateArticleEntity getCandidateArticle() {
        return candidateArticle;
    }

    public void setCandidateArticle(CandidateArticleEntity candidateArticle) {
        this.candidateArticle = candidateArticle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getArticleContent() {
        return articleContent;
    }

    public void setArticleContent(String articleContent) {
        this.articleContent = articleContent;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(LocalDateTime selectedAt) {
        this.selectedAt = selectedAt;
    }

    public LocalDateTime getTranslatedAt() {
        return translatedAt;
    }

    public void setTranslatedAt(LocalDateTime translatedAt) {
        this.translatedAt = translatedAt;
    }

    public LocalDateTime getVocabularyExtractedAt() {
        return vocabularyExtractedAt;
    }

    public void setVocabularyExtractedAt(LocalDateTime vocabularyExtractedAt) {
        this.vocabularyExtractedAt = vocabularyExtractedAt;
    }

    public LocalDateTime getEudicPushedAt() {
        return eudicPushedAt;
    }

    public void setEudicPushedAt(LocalDateTime eudicPushedAt) {
        this.eudicPushedAt = eudicPushedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
