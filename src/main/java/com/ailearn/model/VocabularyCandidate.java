package com.ailearn.model;

public record VocabularyCandidate(
        String word,
        String lemma,
        String type,
        String ipa,
        String englishDefinition,
        String chineseDefinition,
        String sourceSentence
) {
}
