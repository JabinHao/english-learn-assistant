package com.ailearn.service.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslationServiceTest {

    @Test
    void translate_shouldReturnTranslationsInParagraphOrder() {
        TranslationService service = new TranslationService(
                new ObjectMapper(),
                prompt -> """
                        {
                          "translations": [
                            {"index": 2, "chineseText": "第二段"},
                            {"index": 1, "chineseText": "第一段"}
                          ]
                        }
                        """,
                "Prompt {{paragraphs}}"
        );

        assertThat(service.translate(List.of("Paragraph one", "Paragraph two")))
                .containsExactly("第一段", "第二段");
    }

    @Test
    void parseResponse_shouldFailWhenArrayMissing() {
        TranslationService service = new TranslationService(new ObjectMapper(), prompt -> "", "Prompt {{paragraphs}}");

        assertThatThrownBy(() -> service.parseResponse("{\"items\":[]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("translations array");
    }

    @Test
    void translate_shouldSplitLargeRequestsIntoBatches() {
        AtomicInteger calls = new AtomicInteger();
        List<String> prompts = new ArrayList<>();
        TranslationService service = new TranslationService(
                new ObjectMapper(),
                prompt -> {
                    prompts.add(prompt);
                    int call = calls.incrementAndGet();
                    return """
                            {
                              "translations": [
                                {"index": 1, "chineseText": "第%s批"}
                              ]
                            }
                            """.formatted(call);
                },
                "Prompt {{paragraphs}}"
        );

        assertThat(service.translate(List.of(longParagraph("one"), longParagraph("two"), longParagraph("three"))))
                .containsExactly("第1批", "第2批", "第3批");
        assertThat(calls.get()).isEqualTo(3);
        assertThat(prompts).allMatch(prompt -> prompt.length() < 5_000);
    }

    private String longParagraph(String suffix) {
        return ("This is a long paragraph about AI agent optimization and translation batching " + suffix + ". ").repeat(55);
    }
}
