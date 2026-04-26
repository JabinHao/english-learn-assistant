package com.ailearn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "article_paragraph")
public class ArticleParagraphEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "learning_article_id", nullable = false)
    private LearningArticleEntity learningArticle;

    @Column(nullable = false)
    private Integer paragraphIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String englishText;

    @Column(columnDefinition = "TEXT")
    private String chineseText;

    public Long getId() {
        return id;
    }

    public LearningArticleEntity getLearningArticle() {
        return learningArticle;
    }

    public void setLearningArticle(LearningArticleEntity learningArticle) {
        this.learningArticle = learningArticle;
    }

    public Integer getParagraphIndex() {
        return paragraphIndex;
    }

    public void setParagraphIndex(Integer paragraphIndex) {
        this.paragraphIndex = paragraphIndex;
    }

    public String getEnglishText() {
        return englishText;
    }

    public void setEnglishText(String englishText) {
        this.englishText = englishText;
    }

    public String getChineseText() {
        return chineseText;
    }

    public void setChineseText(String chineseText) {
        this.chineseText = chineseText;
    }
}
