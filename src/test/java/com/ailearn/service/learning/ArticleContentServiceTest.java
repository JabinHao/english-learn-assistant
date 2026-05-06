package com.ailearn.service.learning;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
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
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void extractArticleContent_shouldPreferWordPressPostContent() {
        ArticleContentService service = new ArticleContentService();

        String content = service.extractArticleContent("""
                <html>
                  <body>
                    <p>Navigation paragraph that should not be selected even though it is long enough to pass filtering.</p>
                    <div class="wp-block-post-content">
                      <p>OpenAI agent execution layers add persistent memory and resumable workflows for enterprise systems.</p>
                    </div>
                  </body>
                </html>
                """);

        assertThat(content).isEqualTo(
                "OpenAI agent execution layers add persistent memory and resumable workflows for enterprise systems."
        );
    }


    @Test
    void fetchArticleContent_shouldRetryWithAdditionalTrustedRootsWhenSslHandshakeFails() {
        FakeHttpClient defaultClient = new FakeHttpClient(new SSLHandshakeException("PKIX path building failed"));
        FakeHttpClient additionalTrustedRootsClient = new FakeHttpClient("""
                <html>
                  <body>
                    <article>
                      <p>Enterprise teams can now evaluate OpenAI agent execution patterns in production workflows.</p>
                    </article>
                  </body>
                </html>
                """);
        ArticleContentService service = new ArticleContentService(defaultClient, additionalTrustedRootsClient);

        String content = service.fetchArticleContent("https://example.com/article");

        assertThat(content).isEqualTo("Enterprise teams can now evaluate OpenAI agent execution patterns in production workflows.");
        assertThat(defaultClient.requestCount()).isEqualTo(1);
        assertThat(additionalTrustedRootsClient.requestCount()).isEqualTo(1);
    }

    private static class FakeHttpClient extends HttpClient {

        private final String responseBody;
        private final IOException exception;
        private final AtomicInteger requestCount = new AtomicInteger();

        FakeHttpClient(String responseBody) {
            this.responseBody = responseBody;
            this.exception = null;
        }

        FakeHttpClient(IOException exception) {
            this.responseBody = null;
            this.exception = exception;
        }

        int requestCount() {
            return requestCount.get();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException {
            requestCount.incrementAndGet();
            if (exception != null) {
                throw exception;
            }
            @SuppressWarnings("unchecked")
            T body = (T) responseBody;
            return new FakeHttpResponse<>(request, body);
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
            return Redirect.NORMAL;
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
            return HttpClient.Version.HTTP_2;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WebSocket.Builder newWebSocketBuilder() {
            throw new UnsupportedOperationException();
        }
    }

    private record FakeHttpResponse<T>(HttpRequest request, T body) implements HttpResponse<T> {
        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (name, value) -> true);
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_2;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
