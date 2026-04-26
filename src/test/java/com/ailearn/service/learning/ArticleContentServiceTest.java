package com.ailearn.service.learning;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleContentServiceTest {

    @Test
    void extractArticleContent_shouldPreferArticleParagraphsAndNormalizeWhitespace() {
        ArticleContentService service = new ArticleContentService();

        String content = service.extractArticleContent("""
                <html>
                  <body>
                    <article>
                      <p>OpenAI released a new reasoning-focused model for developers working on agent systems.</p>
                      <p>This update improves tool use reliability and reduces latency for long context tasks.</p>
                    </article>
                    <footer>ignore me</footer>
                  </body>
                </html>
                """);

        assertThat(content).isEqualTo(
                "OpenAI released a new reasoning-focused model for developers working on agent systems.\n\n"
                        + "This update improves tool use reliability and reduces latency for long context tasks."
        );
    }
}
