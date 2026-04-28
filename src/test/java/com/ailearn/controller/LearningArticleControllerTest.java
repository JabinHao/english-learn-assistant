package com.ailearn.controller;

import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import com.ailearn.repository.ArticleParagraphRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.repository.VocabularyItemRepository;
import com.ailearn.service.learning.LearningWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningArticleControllerTest {

    @Test
    void getLearningArticle_shouldReturnArticleParagraphsAndVocabulary() throws Exception {
        LearningArticleEntity article = learningArticle();
        ArticleParagraphEntity paragraph = new ArticleParagraphEntity();
        paragraph.setParagraphIndex(1);
        paragraph.setEnglishText("English paragraph");
        paragraph.setChineseText("中文段落");

        VocabularyItemEntity vocabularyItem = new VocabularyItemEntity();
        ReflectionTestUtils.setField(vocabularyItem, "id", 5L);
        vocabularyItem.setLearningArticle(article);
        vocabularyItem.setWord("reasoning");
        vocabularyItem.setLemma("reasoning");
        vocabularyItem.setType("WORD");
        vocabularyItem.setIpa("/ˈriːzənɪŋ/");
        vocabularyItem.setChineseDefinition("推理");
        vocabularyItem.setEudicPushed(true);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LearningArticleController(
                        learningRepository(article),
                        paragraphRepository(List.of(paragraph)),
                        vocabularyRepository(List.of(vocabularyItem)),
                        workflowService()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/learning-articles/88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(88))
                .andExpect(jsonPath("$.candidateArticleId").value(7))
                .andExpect(jsonPath("$.paragraphs.length()").value(1))
                .andExpect(jsonPath("$.paragraphs[0].chineseText").value("中文段落"))
                .andExpect(jsonPath("$.vocabularyItems[0].id").value(5))
                .andExpect(jsonPath("$.vocabularyItems[0].word").value("reasoning"))
                .andExpect(jsonPath("$.vocabularyItems[0].ipa").value("/ˈriːzənɪŋ/"))
                .andExpect(jsonPath("$.vocabularyItems[0].eudicPushed").value(true));
    }

    @Test
    void pushVocabularyItem_shouldReturnUpdatedVocabularyItem() throws Exception {
        VocabularyItemEntity vocabularyItem = new VocabularyItemEntity();
        ReflectionTestUtils.setField(vocabularyItem, "id", 5L);
        vocabularyItem.setWord("reasoning");
        vocabularyItem.setLemma("reasoning");
        vocabularyItem.setType("WORD");
        vocabularyItem.setIpa("/ˈriːzənɪŋ/");
        vocabularyItem.setChineseDefinition("推理");
        vocabularyItem.setEudicPushed(true);

        LearningWorkflowService workflowService = new LearningWorkflowService(null, null, null, null, null, null, null, null, java.time.Clock.systemUTC()) {
            @Override
            public VocabularyItemEntity pushVocabularyItem(Long learningArticleId, Long vocabularyItemId) {
                return vocabularyItem;
            }
        };

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LearningArticleController(
                        learningRepository(learningArticle()),
                        paragraphRepository(List.of()),
                        vocabularyRepository(List.of()),
                        workflowService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/learning-articles/88/vocabulary/5/push"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.word").value("reasoning"))
                .andExpect(jsonPath("$.eudicPushed").value(true));
    }

    @Test
    void removeVocabularyItem_shouldReturnUpdatedVocabularyItem() throws Exception {
        VocabularyItemEntity vocabularyItem = new VocabularyItemEntity();
        ReflectionTestUtils.setField(vocabularyItem, "id", 5L);
        vocabularyItem.setWord("reasoning");
        vocabularyItem.setLemma("reasoning");
        vocabularyItem.setType("WORD");
        vocabularyItem.setIpa("/ˈriːzənɪŋ/");
        vocabularyItem.setChineseDefinition("推理");
        vocabularyItem.setEudicPushed(false);

        LearningWorkflowService workflowService = new LearningWorkflowService(null, null, null, null, null, null, null, null, java.time.Clock.systemUTC()) {
            @Override
            public VocabularyItemEntity removeVocabularyItem(Long learningArticleId, Long vocabularyItemId) {
                return vocabularyItem;
            }
        };

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LearningArticleController(
                        learningRepository(learningArticle()),
                        paragraphRepository(List.of()),
                        vocabularyRepository(List.of()),
                        workflowService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(delete("/api/learning-articles/88/vocabulary/5/push"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.word").value("reasoning"))
                .andExpect(jsonPath("$.eudicPushed").value(false));
    }

    private LearningArticleRepository learningRepository(LearningArticleEntity article) {
        return (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(article);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LearningArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private ArticleParagraphRepository paragraphRepository(List<ArticleParagraphEntity> paragraphs) {
        return (ArticleParagraphRepository) Proxy.newProxyInstance(
                ArticleParagraphRepository.class.getClassLoader(),
                new Class[]{ArticleParagraphRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByLearningArticleIdOrderByParagraphIndexAsc" -> paragraphs;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "ArticleParagraphRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private VocabularyItemRepository vocabularyRepository(List<VocabularyItemEntity> items) {
        return (VocabularyItemRepository) Proxy.newProxyInstance(
                VocabularyItemRepository.class.getClassLoader(),
                new Class[]{VocabularyItemRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByLearningArticleIdOrderByCreatedAtAsc" -> items;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "VocabularyItemRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private LearningArticleEntity learningArticle() {
        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidateArticle, "id", 7L);

        LearningArticleEntity article = new LearningArticleEntity();
        ReflectionTestUtils.setField(article, "id", 88L);
        article.setCandidateArticle(candidateArticle);
        article.setStatus("VOCAB_READY");
        article.setTitle("Selected article");
        article.setUrl("https://example.com/article");
        article.setSource("Test Feed");
        article.setPublishedAt(LocalDateTime.parse("2026-04-26T09:00:00"));
        article.setArticleContent("English paragraph");
        article.setSummary("Summary");
        return article;
    }

    private LearningWorkflowService workflowService() {
        return new LearningWorkflowService(null, null, null, null, null, null, null, null, java.time.Clock.systemUTC()) {
        };
    }
}
