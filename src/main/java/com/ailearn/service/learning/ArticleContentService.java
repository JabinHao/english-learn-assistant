package com.ailearn.service.learning;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleContentService {

    private static final String READER_BASE_URL = "https://r.jina.ai/";
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;

    public ArticleContentService() {
        this(HttpClient.newHttpClient());
    }

    ArticleContentService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public FetchedArticle fetchArticle(String url) {
        try {
            HttpResponse<String> response = fetch(url);
            if (isSuccessful(response)) {
                return extractArticle(response.body());
            }

            HttpResponse<String> readerResponse = fetch(READER_BASE_URL + url);
            if (isSuccessful(readerResponse)) {
                return extractArticle(readerResponse.body());
            }

            throw new IllegalStateException("Failed to fetch article content: HTTP " + response.statusCode());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to fetch article content", exception);
        }
    }

    public String fetchArticleContent(String url) {
        return fetchArticle(url).content();
    }

    private HttpResponse<String> fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(FETCH_TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (compatible; AIEnglishTutor/1.0)")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private boolean isSuccessful(HttpResponse<String> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private FetchedArticle extractArticle(String body) {
        String trimmed = body == null ? "" : body.strip();
        if (trimmed.startsWith("<")) {
            return extractHtmlArticle(trimmed);
        }
        return extractPlainTextArticle(trimmed);
    }

    private FetchedArticle extractHtmlArticle(String html) {
        Document document = Jsoup.parse(html);
        String title = firstNonBlank(
                document.title(),
                textOf(document.selectFirst("article h1, main h1, h1"))
        );
        return new FetchedArticle(title, extractArticleContent(document));
    }

    private FetchedArticle extractPlainTextArticle(String text) {
        String title = null;
        List<String> paragraphs = new ArrayList<>();
        for (String block : text.split("\\R\\s*\\R")) {
            String normalized = block.trim().replaceAll("\\s+", " ");
            if (normalized.startsWith("Title:")) {
                title = normalized.substring("Title:".length()).trim();
                continue;
            }
            if (normalized.length() < 40 || normalized.startsWith("URL Source:")) {
                continue;
            }
            paragraphs.add(normalized);
        }

        if (paragraphs.isEmpty()) {
            String normalized = text.trim().replaceAll("\\s+", " ");
            if (!normalized.isBlank()) {
                paragraphs.add(normalized);
            }
        }

        return new FetchedArticle(title, String.join("\n\n", paragraphs));
    }

    private String textOf(Element element) {
        return element == null ? null : element.text().trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    public record FetchedArticle(String title, String content) {
    }

    public String extractContent(String body) {
        return extractArticle(body).content();
    }

    public String extractArticleContent(String html) {
        return extractArticleContent(Jsoup.parse(html));
    }

    private String extractArticleContent(Document document) {
        Element root = findRoot(document);
        List<String> paragraphs = new ArrayList<>();
        for (Element paragraph : root.select("p")) {
            String text = paragraph.text().trim().replaceAll("\\s+", " ");
            if (text.length() >= 40) {
                paragraphs.add(text);
            }
        }

        if (paragraphs.isEmpty()) {
            String bodyText = root.text().trim().replaceAll("\\s+", " ");
            if (!bodyText.isBlank()) {
                paragraphs.add(bodyText);
            }
        }

        return String.join("\n\n", paragraphs);
    }

    private Element findRoot(Document document) {
        for (String selector : List.of("article", "main", "[role=main]")) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                return element;
            }
        }
        return document.body();
    }
}
