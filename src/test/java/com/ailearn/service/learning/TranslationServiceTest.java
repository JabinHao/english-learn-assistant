package com.ailearn.service.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
