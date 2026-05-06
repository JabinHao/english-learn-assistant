package com.ailearn.service.learning;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

@Service
public class ArticleContentService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EnglishLearnAssistant/0.1)";
    private static final String CERTUM_TRUSTED_ROOT_CA = "/certs/certum-trusted-root-ca.pem";

    private final HttpClient httpClient;
    private final HttpClient additionalTrustedRootsHttpClient;

    public ArticleContentService() {
        this(defaultHttpClient(), additionalTrustedRootsHttpClient());
    }

    ArticleContentService(HttpClient httpClient) {
        this(httpClient, httpClient);
    }

    ArticleContentService(HttpClient httpClient, HttpClient additionalTrustedRootsHttpClient) {
        this.httpClient = httpClient;
        this.additionalTrustedRootsHttpClient = additionalTrustedRootsHttpClient;
    }

    public String fetchArticleContent(String url) {
        try {
            HttpRequest request = request(url);
            HttpResponse<String> response = send(httpClient, request);
            return extractArticleContent(response.body());
        } catch (IOException | InterruptedException exception) {
            if (causedBy(exception, SSLHandshakeException.class) && additionalTrustedRootsHttpClient != httpClient) {
                return fetchArticleContentWithAdditionalTrustedRoots(url, exception);
            }
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to fetch article content", exception);
        }
    }

    private String fetchArticleContentWithAdditionalTrustedRoots(String url, Exception primaryException) {
        try {
            HttpResponse<String> response = send(additionalTrustedRootsHttpClient, request(url));
            return extractArticleContent(response.body());
        } catch (IOException | InterruptedException exception) {
            primaryException.addSuppressed(exception);
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to fetch article content", primaryException);
        }
    }

    private HttpRequest request(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
    }

    private HttpResponse<String> send(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Article request failed with HTTP " + response.statusCode());
        }
        return response;
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
        for (String selector : List.of("article", ".wp-block-post-content", ".entry-content", ".post-content", "main", "[role=main]")) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                return element;
            }
        }
        return document.body();
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    private static HttpClient additionalTrustedRootsHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(REQUEST_TIMEOUT)
                .sslContext(additionalTrustedRootsSslContext())
                .build();
    }

    private static SSLContext additionalTrustedRootsSslContext() {
        try {
            X509TrustManager defaultTrustManager = trustManager(null);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            try (InputStream inputStream = ArticleContentService.class.getResourceAsStream(CERTUM_TRUSTED_ROOT_CA)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Missing certificate resource " + CERTUM_TRUSTED_ROOT_CA);
                }
                X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(inputStream);
                keyStore.setCertificateEntry("certum-trusted-root-ca", certificate);
            }

            X509TrustManager additionalTrustManager = trustManager(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new CompositeTrustManager(defaultTrustManager, additionalTrustManager)}, null);
            return sslContext;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize article fetch TLS trust", exception);
        }
    }

    private static X509TrustManager trustManager(KeyStore keyStore) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        return Arrays.stream(factory.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No X509 trust manager available"));
    }

    private boolean causedBy(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record CompositeTrustManager(
            X509TrustManager defaultTrustManager,
            X509TrustManager additionalTrustManager
    ) implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (java.security.cert.CertificateException exception) {
                additionalTrustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] defaultIssuers = defaultTrustManager.getAcceptedIssuers();
            X509Certificate[] additionalIssuers = additionalTrustManager.getAcceptedIssuers();
            X509Certificate[] issuers = Arrays.copyOf(defaultIssuers, defaultIssuers.length + additionalIssuers.length);
            System.arraycopy(additionalIssuers, 0, issuers, defaultIssuers.length, additionalIssuers.length);
            return issuers;
        }
    }
}
