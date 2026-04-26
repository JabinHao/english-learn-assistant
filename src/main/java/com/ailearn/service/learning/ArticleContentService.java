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
import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleContentService {

    private final HttpClient httpClient;

    public ArticleContentService() {
        this(HttpClient.newHttpClient());
    }

    ArticleContentService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String fetchArticleContent(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return extractArticleContent(response.body());
        } catch (IOException | InterruptedException exception) {
            throw new IllegalStateException("Failed to fetch article content", exception);
        }
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
