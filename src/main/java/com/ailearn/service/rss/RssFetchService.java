package com.ailearn.service.rss;

import com.ailearn.config.AppConfig;
import com.ailearn.model.FeedArticle;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class RssFetchService {

    private static final Pattern VOID_HTML_TAG_PATTERN = Pattern.compile(
            "<(br|hr|img|meta|input)(\\s[^>/]*)?>",
            Pattern.CASE_INSENSITIVE
    );

    private final AppConfig appConfig;
    private final HttpClient httpClient;

    @Autowired
    public RssFetchService(AppConfig appConfig) {
        this(appConfig, HttpClient.newHttpClient());
    }

    RssFetchService(AppConfig appConfig, HttpClient httpClient) {
        this.appConfig = appConfig;
        this.httpClient = httpClient;
    }

    public List<FeedArticle> fetchAll() {
        List<FeedArticle> articles = new ArrayList<>();
        for (AppConfig.Feed feed : appConfig.getRss().getFeeds()) {
            articles.addAll(fetchFeed(feed));
        }
        return articles;
    }

    public List<FeedArticle> fetchFeed(AppConfig.Feed feed) {
        try {
            String xml = fetch(feed.getUrl());
            return parse(xml, feed.getName());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to fetch RSS feed: " + feed.getName(), exception);
        }
    }

    public List<FeedArticle> parse(String xml, String sourceName) {
        try {
            SyndFeedInput input = new SyndFeedInput();
            String sanitizedXml = sanitizeMalformedFeedXml(xml);
            SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(sanitizedXml.getBytes(StandardCharsets.UTF_8))));

            List<FeedArticle> articles = new ArrayList<>();
            for (SyndEntry entry : feed.getEntries()) {
                articles.add(new FeedArticle(
                        normalize(entry.getTitle()),
                        normalize(entry.getLink()),
                        sourceName,
                        normalizeSummary(entry),
                        publishedAt(entry)
                ));
            }
            return articles;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse RSS XML", exception);
        }
    }

    private String sanitizeMalformedFeedXml(String xml) {
        return VOID_HTML_TAG_PATTERN.matcher(xml).replaceAll(matchResult -> {
            String tag = matchResult.group();
            if (tag.endsWith("/>")) {
                return tag;
            }
            return tag.substring(0, tag.length() - 1) + "/>";
        });
    }

    private String fetch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String normalizeSummary(SyndEntry entry) {
        if (entry.getDescription() == null || entry.getDescription().getValue() == null) {
            return "";
        }
        return normalize(Jsoup.parse(entry.getDescription().getValue()).text());
    }

    private LocalDateTime publishedAt(SyndEntry entry) {
        if (entry.getPublishedDate() == null) {
            return null;
        }
        return entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
