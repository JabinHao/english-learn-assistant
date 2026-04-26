package com.ailearn.service.tutor;

import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import com.ailearn.repository.ArticleParagraphRepository;
import com.ailearn.repository.LearningArticleRepository;
import com.ailearn.repository.VocabularyItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleTutorContextServiceTest {

    @Test
    void buildContext_shouldIncludeSummaryParagraphsAndVocabulary() {
        LearningArticleEntity article = learningArticle();
        ArticleParagraphEntity paragraph = new ArticleParagraphEntity();
        paragraph.setParagraphIndex(1);
        paragraph.setEnglishText("English paragraph");
        paragraph.setChineseText("中文段落");

        VocabularyItemEntity item = new VocabularyItemEntity();
        item.setWord("reasoning");
        item.setType("WORD");
        item.setChineseDefinition("推理");
        item.setSourceSentence("English paragraph");

        ArticleTutorContextService service = new ArticleTutorContextService(
                learningRepository(article),
                paragraphRepository(List.of(paragraph)),
                vocabularyRepository(List.of(item))
        );

        String context = service.buildContext(88L);

        assertThat(context).contains("Article Title: Selected article");
        assertThat(context).contains("Summary: Summary");
        assertThat(context).contains("EN: English paragraph");
        assertThat(context).contains("ZH: 中文段落");
        assertThat(context).contains("- reasoning [WORD] : 推理");
    }

    private LearningArticleRepository learningRepository(LearningArticleEntity article) {
        return (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(article);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
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
        article.setStatus("EUDIC_PUSHED");
        article.setTitle("Selected article");
        article.setSource("Test Feed");
        article.setSummary("Summary");
        return article;
    }
}
