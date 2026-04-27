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
@Table(name = "vocabulary_item")
public class VocabularyItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "learning_article_id", nullable = false)
    private LearningArticleEntity learningArticle;

    @Column(nullable = false, length = 255)
    private String word;

    @Column(length = 255)
    private String lemma;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(length = 128)
    private String ipa;

    @Column(columnDefinition = "TEXT")
    private String englishDefinition;

    @Column(columnDefinition = "TEXT")
    private String chineseDefinition;

    @Column(columnDefinition = "TEXT")
    private String sourceSentence;

    @Column(nullable = false)
    private boolean eudicPushed;

    private LocalDateTime eudicPushedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public LearningArticleEntity getLearningArticle() {
        return learningArticle;
    }

    public void setLearningArticle(LearningArticleEntity learningArticle) {
        this.learningArticle = learningArticle;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIpa() {
        return ipa;
    }

    public void setIpa(String ipa) {
        this.ipa = ipa;
    }

    public String getEnglishDefinition() {
        return englishDefinition;
    }

    public void setEnglishDefinition(String englishDefinition) {
        this.englishDefinition = englishDefinition;
    }

    public String getChineseDefinition() {
        return chineseDefinition;
    }

    public void setChineseDefinition(String chineseDefinition) {
        this.chineseDefinition = chineseDefinition;
    }

    public String getSourceSentence() {
        return sourceSentence;
    }

    public void setSourceSentence(String sourceSentence) {
        this.sourceSentence = sourceSentence;
    }

    public boolean isEudicPushed() {
        return eudicPushed;
    }

    public void setEudicPushed(boolean eudicPushed) {
        this.eudicPushed = eudicPushed;
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
