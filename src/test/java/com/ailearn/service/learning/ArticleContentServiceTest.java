package com.ailearn.service.learning;

import org.junit.jupiter.api.Test;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

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
    void fetchArticleContent_shouldFallbackToReaderWhenDirectFetchIsBlocked() {
        RecordingHttpClient httpClient = new RecordingHttpClient(
                response(403, """
                        <html><body>Cloudflare challenge</body></html>
                        """),
                response(200, """
                        Title: Spring Isn’t Dead

                        URL Source: https://medium.com/@niketl16/spring-isnt-dead

                        For the last three years, Java teams were told to use Python for LLMs.

                        That take aged badly.
                        """)
        );
        ArticleContentService service = new ArticleContentService(httpClient);

        String content = service.fetchArticleContent("https://medium.com/@niketl16/spring-isnt-dead");

        assertThat(httpClient.requestedUris())
                .containsExactly(
                        URI.create("https://medium.com/@niketl16/spring-isnt-dead"),
                        URI.create("https://r.jina.ai/https://medium.com/@niketl16/spring-isnt-dead")
                );
        assertThat(httpClient.requests())
                .allSatisfy(request -> assertThat(request.timeout()).contains(Duration.ofSeconds(30)));
        assertThat(content).contains("For the last three years, Java teams were told to use Python for LLMs.");
        assertThat(content).doesNotContain("Cloudflare challenge");
    }

    @Test
    void fetchArticle_shouldReturnReaderTitleAndContent() {
        RecordingHttpClient httpClient = new RecordingHttpClient(
                response(403, "<html><body>Cloudflare challenge</body></html>"),
                response(200, """
                        Title: Spring Isn’t Dead

                        URL Source: https://medium.com/@niketl16/spring-isnt-dead

                        For the last three years, Java teams were told to use Python for LLMs.

                        That take aged badly.
                        """)
        );
        ArticleContentService service = new ArticleContentService(httpClient);

        ArticleContentService.FetchedArticle article = service.fetchArticle("https://medium.com/@niketl16/spring-isnt-dead");

        assertThat(article.title()).isEqualTo("Spring Isn’t Dead");
        assertThat(article.content()).contains("For the last three years, Java teams were told to use Python for LLMs.");
        assertThat(article.content()).doesNotContain("Title:");
    }

    private static HttpResponse<String> response(int statusCode, String body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(java.util.Map.of(), (name, value) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_2;
            }
        };
    }

    private static class RecordingHttpClient extends HttpClient {

        private final List<HttpResponse<String>> responses;
        private final List<HttpRequest> requests = new ArrayList<>();
        private final List<URI> requestedUris = new ArrayList<>();

        private RecordingHttpClient(HttpResponse<String>... responses) {
            this.responses = new ArrayList<>(List.of(responses));
        }

        List<URI> requestedUris() {
            return requestedUris;
        }

        List<HttpRequest> requests() {
            return requests;
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
        public Version version() {
            return Version.HTTP_2;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            requestedUris.add(request.uri());
            requests.add(request);
            return (HttpResponse<T>) responses.removeFirst();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
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
    }
}
