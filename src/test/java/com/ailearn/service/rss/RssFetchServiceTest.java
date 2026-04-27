package com.ailearn.service.rss;

import com.ailearn.config.AppConfig;
import com.ailearn.model.FeedArticle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RssFetchServiceTest {

    private final RssFetchService rssFetchService = new RssFetchService(new AppConfig());

    @Test
    void parse_shouldExtractArticlesFromRssXml() throws IOException {
        String xml = loadFixture("sample-rss.xml");

        List<FeedArticle> articles = rssFetchService.parse(xml, "Test Feed");

        assertThat(articles).hasSize(2);
        assertThat(articles.getFirst().title()).isEqualTo("LLM orchestration in practice");
        assertThat(articles.getFirst().url()).isEqualTo("https://example.com/llm-orchestration");
        assertThat(articles.getFirst().source()).isEqualTo("Test Feed");
        assertThat(articles.getFirst().summary()).isEqualTo("A practical guide to agent orchestration patterns.");
    }

    @Test
    void parse_shouldNormalizeWhitespaceAndHtmlInSummary() throws IOException {
        String xml = loadFixture("sample-rss.xml");

        List<FeedArticle> articles = rssFetchService.parse(xml, "Test Feed");

        assertThat(articles.get(1).title()).isEqualTo("How inference latency shapes product design");
        assertThat(articles.get(1).summary()).isEqualTo("Latency drives different UX tradeoffs in AI products.");
    }

    @Test
    void parse_shouldHandleMalformedVoidHtmlTagsInsideFeedContent() throws IOException {
        String xml = loadFixture("sample-rss-malformed-void-tags.xml");

        List<FeedArticle> articles = rssFetchService.parse(xml, "Test Feed");

        assertThat(articles).hasSize(1);
        assertThat(articles.getFirst().title()).isEqualTo("Malformed feed content");
        assertThat(articles.getFirst().summary()).isEqualTo("Line one Line two");
    }

    private String loadFixture(String name) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            assert stream != null;
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
