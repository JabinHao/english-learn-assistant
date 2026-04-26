package com.ailearn.service.learning;

import com.ailearn.model.VocabularyCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VocabularyExtractionServiceTest {

    @Test
    void extract_shouldParseStructuredVocabularyItems() {
        VocabularyExtractionService service = new VocabularyExtractionService(
                new ObjectMapper(),
                prompt -> """
                        {
                          "items": [
                            {
                              "word": "reasoning model",
                              "lemma": "reasoning model",
                              "type": "PHRASE",
                              "ipa": "",
                              "englishDefinition": "A model optimized for deliberate multi-step thinking.",
                              "chineseDefinition": "针对多步推理优化的模型。",
                              "sourceSentence": "The reasoning model improved accuracy."
                            }
                          ]
                        }
                        """,
                "Prompt {{paragraphs}}"
        );

        List<VocabularyCandidate> result = service.extract(List.of("The reasoning model improved accuracy."));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().word()).isEqualTo("reasoning model");
        assertThat(result.getFirst().type()).isEqualTo("PHRASE");
    }

    @Test
    void parseResponse_shouldFailWhenItemsArrayMissing() {
        VocabularyExtractionService service = new VocabularyExtractionService(new ObjectMapper(), prompt -> "", "Prompt {{paragraphs}}");

        assertThatThrownBy(() -> service.parseResponse("{\"translations\":[]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("items array");
    }
}
