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

    public String fetchArticleContent(String url) {
        try {
            HttpResponse<String> response = fetch(url);
            if (isSuccessful(response)) {
                return extractContent(response.body());
            }

            HttpResponse<String> readerResponse = fetch(READER_BASE_URL + url);
            if (isSuccessful(readerResponse)) {
                return extractContent(readerResponse.body());
            }

            throw new IllegalStateException("Failed to fetch article content: HTTP " + response.statusCode());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to fetch article content", exception);
        }
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

    private String extractContent(String body) {
        String trimmed = body == null ? "" : body.strip();
        if (trimmed.startsWith("<")) {
            return extractArticleContent(trimmed);
        }
        return extractPlainTextContent(trimmed);
    }

    public String extractArticleContent(String html) {
        Document document = Jsoup.parse(html);
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

    private String extractPlainTextContent(String text) {
        List<String> paragraphs = new ArrayList<>();
        for (String block : text.split("\\R\\s*\\R")) {
            String normalized = block.trim().replaceAll("\\s+", " ");
            if (normalized.length() < 40) {
                continue;
            }
            if (normalized.startsWith("Title:") || normalized.startsWith("URL Source:")) {
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
