package com.ailearn.api.learning;

import java.time.LocalDateTime;
import java.util.List;

public record LearningArticleResponse(
        Long id,
        Long candidateArticleId,
        String status,
        String title,
        String url,
        String source,
        LocalDateTime publishedAt,
        String articleContent,
        String summary,
        List<ParagraphResponse> paragraphs,
        List<VocabularyItemResponse> vocabularyItems
) {
    public record ParagraphResponse(
            Integer paragraphIndex,
            String englishText,
            String chineseText
    ) {
    }

    public record VocabularyItemResponse(
            Long id,
            String word,
            String lemma,
            String type,
            String ipa,
            String englishDefinition,
            String chineseDefinition,
            String sourceSentence,
            boolean eudicPushed
    ) {
    }
}
