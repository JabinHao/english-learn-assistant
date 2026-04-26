package com.ailearn.client;

import com.ailearn.config.AppConfig;
import com.ailearn.entity.VocabularyItemEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class EudicClientTest {

    @Test
    void ensureStudyList_shouldCreateWhenMissing() {
        FakeHttpClient httpClient = new FakeHttpClient(
                new FakeHttpResponse(200, "[]"),
                new FakeHttpResponse(200, "{\"id\":\"list-123\"}")
        );
        EudicClient client = new EudicClient(config(), httpClient, new ObjectMapper());

        assertThat(client.ensureStudyList()).isEqualTo("list-123");
        assertThat(httpClient.requests()).hasSize(2);
        assertThat(httpClient.requests().get(1).uri().toString()).endsWith("/api/open/v1/studylist/category");
        assertThat(httpClient.requests().get(1).headers().firstValue("Content-Type")).contains("application/json");
    }

    @Test
    void pushWord_shouldSendWordAndSentence() {
        FakeHttpClient httpClient = new FakeHttpClient(new FakeHttpResponse(201, "{\"ok\":true}"));
        EudicClient client = new EudicClient(config(), httpClient, new ObjectMapper());
        VocabularyItemEntity item = new VocabularyItemEntity();
        item.setWord("reasoning");
        item.setSourceSentence("The reasoning model improved accuracy.");

        assertThat(client.pushWord("list-123", item)).isTrue();
        assertThat(httpClient.requests()).hasSize(1);
        assertThat(httpClient.requests().get(0).uri().toString()).endsWith("/api/open/v1/studylist/word");
    }

    private AppConfig config() {
        AppConfig appConfig = new AppConfig();
        AppConfig.Eudic eudic = new AppConfig.Eudic();
        eudic.setBaseUrl("https://api.example.test");
        eudic.setAuthToken("test-token");
        eudic.setStudyListName("ai-learn");
        eudic.setLanguage("en");
        appConfig.setEudic(eudic);
        return appConfig;
    }

    private static final class FakeHttpClient extends HttpClient {
        private final Queue<HttpResponse<String>> responses;
        private final java.util.List<HttpRequest> requests = new java.util.ArrayList<>();

        private FakeHttpClient(HttpResponse<String>... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        java.util.List<HttpRequest> requests() {
            return requests;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            requests.add(request);
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) responses.remove();
            return response;
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    private record FakeHttpResponse(int statusCode, String body) implements HttpResponse<String> {

        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder(URI.create("https://api.example.test")).build();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public URI uri() {
            return URI.create("https://api.example.test");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
