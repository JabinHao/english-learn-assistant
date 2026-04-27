package com.ailearn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ailearn")
public class AppConfig {

    private Candidate candidate = new Candidate();
    private Cors cors = new Cors();
    private Eudic eudic = new Eudic();
    private Rss rss = new Rss();
    private Filters filters = new Filters();

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public Eudic getEudic() {
        return eudic;
    }

    public void setEudic(Eudic eudic) {
        this.eudic = eudic;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Rss getRss() {
        return rss;
    }

    public void setRss(Rss rss) {
        this.rss = rss;
    }

    public Filters getFilters() {
        return filters;
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }

    @Bean
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    public static class Candidate {
        private String cron = "0 0 8 * * *";
        private int lookbackHours = 36;
        private int maxCandidates = 5;
        private double minScore = 6.5d;

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getLookbackHours() {
            return lookbackHours;
        }

        public void setLookbackHours(int lookbackHours) {
            this.lookbackHours = lookbackHours;
        }

        public int getMaxCandidates() {
            return maxCandidates;
        }

        public void setMaxCandidates(int maxCandidates) {
            this.maxCandidates = maxCandidates;
        }

        public double getMinScore() {
            return minScore;
        }

        public void setMinScore(double minScore) {
            this.minScore = minScore;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Eudic {
        private String baseUrl = "https://api.frdic.com";
        private String authToken = "";
        private String studyListName = "ai-learn";
        private String language = "en";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getStudyListName() {
            return studyListName;
        }

        public void setStudyListName(String studyListName) {
            this.studyListName = studyListName;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    public static class Rss {
        private List<Feed> feeds = new ArrayList<>();

        public List<Feed> getFeeds() {
            return feeds;
        }

        public void setFeeds(List<Feed> feeds) {
            this.feeds = feeds;
        }
    }

    public static class Feed {
        private String name;
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Filters {
        private List<String> keywords = new ArrayList<>();
        private int minTextLength = 120;

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        public int getMinTextLength() {
            return minTextLength;
        }

        public void setMinTextLength(int minTextLength) {
            this.minTextLength = minTextLength;
        }
    }
}
