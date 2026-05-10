package com.ailearn.service.learning;

import com.ailearn.model.VocabularyCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VocabularyExtractionServiceTest {

    @Test
    void extract_shouldReuseCachedVocabularyForSamePrompt() {
        AtomicInteger calls = new AtomicInteger();
        VocabularyExtractionService service = new VocabularyExtractionService(
                new ObjectMapper(),
                new com.ailearn.observability.LlmTraceLogger(new com.ailearn.config.AppConfig()),
                prompt -> {
                    calls.incrementAndGet();
                    return """
                            {
                              "items": [
                                {
                                  "word": "cache hit",
                                  "lemma": "cache hit",
                                  "type": "PHRASE",
                                  "ipa": "",
                                  "englishDefinition": "A reused result.",
                                  "chineseDefinition": "复用结果。",
                                  "sourceSentence": "The cache hit avoided another request."
                                }
                              ]
                            }
                            """;
                },
                "Prompt {{paragraphs}}",
                cacheService()
        );

        assertThat(service.extract(List.of("The cache hit avoided another request."))).hasSize(1);
        assertThat(service.extract(List.of("The cache hit avoided another request."))).hasSize(1);
        assertThat(calls.get()).isEqualTo(1);
    }

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

    private LlmPipelineCacheService cacheService() {
        ConcurrentHashMap<String, com.ailearn.entity.LlmPipelineCacheEntry> cache = new ConcurrentHashMap<>();
        com.ailearn.repository.LlmPipelineCacheRepository repository = (com.ailearn.repository.LlmPipelineCacheRepository) Proxy.newProxyInstance(
                com.ailearn.repository.LlmPipelineCacheRepository.class.getClassLoader(),
                new Class[]{com.ailearn.repository.LlmPipelineCacheRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByCacheKey" -> Optional.ofNullable(cache.get(args[0]));
                    case "save" -> {
                        com.ailearn.entity.LlmPipelineCacheEntry entry = (com.ailearn.entity.LlmPipelineCacheEntry) args[0];
                        cache.put(entry.getCacheKey(), entry);
                        yield entry;
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LlmPipelineCacheRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        return new LlmPipelineCacheService(new ObjectMapper(), repository);
    }
}
