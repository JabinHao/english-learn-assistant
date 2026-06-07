package com.ailearn.service.learning;

import com.ailearn.entity.CandidateArticleEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.repository.LearningArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ManualLearningArticleServiceTest {

    @Test
    void submitUrl_shouldStripFragmentBeforeLookupAndPersistence() {
        AtomicReference<String> lookupUrl = new AtomicReference<>();
        LearningArticleEntity existing = learningArticle();
        LearningArticleRepository repository = (LearningArticleRepository) Proxy.newProxyInstance(
                LearningArticleRepository.class.getClassLoader(),
                new Class[]{LearningArticleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByUrlOrderByCreatedAtDesc" -> {
                        lookupUrl.set((String) args[0]);
                        yield Optional.of(existing);
                    }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "LearningArticleRepositoryProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        ManualLearningArticleService service = new ManualLearningArticleService(
                null,
                null,
                repository,
                null,
                Clock.systemUTC()
        );

        service.submitUrl("https://medium.com/@author/article#id_token=secret-token");

        assertThat(lookupUrl).hasValue("https://medium.com/@author/article");
    }

    private LearningArticleEntity learningArticle() {
        CandidateArticleEntity candidateArticle = new CandidateArticleEntity();
        ReflectionTestUtils.setField(candidateArticle, "id", 7L);

        LearningArticleEntity article = new LearningArticleEntity();
        ReflectionTestUtils.setField(article, "id", 88L);
        article.setCandidateArticle(candidateArticle);
        article.setStatus("VOCAB_READY");
        article.setUrl("https://medium.com/@author/article");
        return article;
    }
}
