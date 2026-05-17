package com.ailearn.service.learning;

import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearningArticleExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void export_shouldWriteStableSelfContainedHtmlArchive() throws Exception {
        LearningArticleEntity article = learningArticle(88L);
        ArticleParagraphEntity firstParagraph = paragraph(1, "AI & systems", "人工智能与系统");
        VocabularyItemEntity item = vocabularyItem("tradeoff", "PHRASE", "/ˈtreɪdˌɔːf/", "a balance", "权衡", "A tradeoff exists.");

        LearningArticleExportService service = new LearningArticleExportService(tempDir);

        Path exportedFile = service.export(article, List.of(firstParagraph), List.of(item));

        assertThat(exportedFile).isEqualTo(tempDir.resolve("88-selected-article.html"));
        assertThat(Files.readString(exportedFile))
                .contains("<!doctype html>")
                .contains("Selected article")
                .contains("https://example.com/article")
                .contains("AI &amp; systems")
                .contains("人工智能与系统")
                .contains("tradeoff")
                .contains("权衡");
    }

    private LearningArticleEntity learningArticle(Long id) {
        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidateArticle, "id", 7L);

        LearningArticleEntity article = new LearningArticleEntity();
        ReflectionTestUtils.setField(article, "id", id);
        article.setCandidateArticle(candidateArticle);
        article.setTitle("Selected article");
        article.setUrl("https://example.com/article");
        article.setSource("Test Feed");
        return article;
    }

    private ArticleParagraphEntity paragraph(int index, String english, String chinese) {
        ArticleParagraphEntity paragraph = new ArticleParagraphEntity();
        paragraph.setParagraphIndex(index);
        paragraph.setEnglishText(english);
        paragraph.setChineseText(chinese);
        return paragraph;
    }

    private VocabularyItemEntity vocabularyItem(
            String word,
            String type,
            String ipa,
            String englishDefinition,
            String chineseDefinition,
            String sourceSentence
    ) {
        VocabularyItemEntity item = new VocabularyItemEntity();
        item.setWord(word);
        item.setType(type);
        item.setIpa(ipa);
        item.setEnglishDefinition(englishDefinition);
        item.setChineseDefinition(chineseDefinition);
        item.setSourceSentence(sourceSentence);
        return item;
    }
}
