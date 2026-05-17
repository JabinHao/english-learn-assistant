package com.ailearn.service.learning;

import com.ailearn.entity.ArticleParagraphEntity;
import com.ailearn.entity.LearningArticleEntity;
import com.ailearn.entity.VocabularyItemEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Service
public class LearningArticleExportService {

    private final Path exportDirectory;

    public LearningArticleExportService() {
        this(Path.of("data", "exports", "articles"));
    }

    LearningArticleExportService(Path exportDirectory) {
        this.exportDirectory = exportDirectory;
    }

    public Path export(
            LearningArticleEntity article,
            List<ArticleParagraphEntity> paragraphs,
            List<VocabularyItemEntity> vocabularyItems
    ) throws IOException {
        Files.createDirectories(exportDirectory);
        Path destination = exportDirectory.resolve(fileName(article));
        Files.writeString(destination, render(article, paragraphs, vocabularyItems), StandardCharsets.UTF_8);
        return destination;
    }

    private String fileName(LearningArticleEntity article) {
        return article.getId() + "-" + slug(article.getTitle()) + ".html";
    }

    private String slug(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "article" : normalized;
    }

    private String render(
            LearningArticleEntity article,
            List<ArticleParagraphEntity> paragraphs,
            List<VocabularyItemEntity> vocabularyItems
    ) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>""")
                .append(escape(article.getTitle()))
                .append("""
                </title>
                  <style>
                    body { margin: 0; font-family: Arial, sans-serif; color: #1f2937; background: #f8fafc; }
                    main { max-width: 920px; margin: 0 auto; padding: 32px 20px 48px; }
                    h1 { font-size: 32px; margin: 0 0 12px; }
                    .meta { color: #475569; margin-bottom: 28px; }
                    .paragraph { padding: 18px 0; border-top: 1px solid #cbd5e1; }
                    .paragraph:first-of-type { border-top: 0; }
                    .english { font-size: 18px; line-height: 1.7; margin: 0 0 10px; }
                    .chinese { font-size: 17px; line-height: 1.8; color: #334155; margin: 0; }
                    h2 { margin-top: 36px; }
                    table { width: 100%; border-collapse: collapse; background: #fff; }
                    th, td { border: 1px solid #cbd5e1; padding: 10px; text-align: left; vertical-align: top; }
                    th { background: #e2e8f0; }
                    a { color: #0369a1; }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>""")
                .append(escape(article.getTitle()))
                .append("""
                </h1>
                    <p class="meta">""")
                .append(escape(article.getSource()))
                .append(" · <a href=\"")
                .append(escape(article.getUrl()))
                .append("\">")
                .append(escape(article.getUrl()))
                .append("""
                </a></p>
                """);

        for (ArticleParagraphEntity paragraph : paragraphs) {
            html.append("""
                    <section class="paragraph">
                      <p class="english">""")
                    .append(escape(paragraph.getEnglishText()))
                    .append("""
                    </p>
                      <p class="chinese">""")
                    .append(escape(paragraph.getChineseText()))
                    .append("""
                    </p>
                    </section>
                    """);
        }

        html.append("""
                    <h2>Vocabulary</h2>
                    <table>
                      <thead>
                        <tr>
                          <th>Word</th>
                          <th>Type</th>
                          <th>IPA</th>
                          <th>English</th>
                          <th>Chinese</th>
                          <th>Source sentence</th>
                        </tr>
                      </thead>
                      <tbody>
                """);
        for (VocabularyItemEntity item : vocabularyItems) {
            html.append("""
                        <tr>
                          <td>""").append(escape(item.getWord())).append("""
                    </td>
                          <td>""").append(escape(item.getType())).append("""
                    </td>
                          <td>""").append(escape(item.getIpa())).append("""
                    </td>
                          <td>""").append(escape(item.getEnglishDefinition())).append("""
                    </td>
                          <td>""").append(escape(item.getChineseDefinition())).append("""
                    </td>
                          <td>""").append(escape(item.getSourceSentence())).append("""
                    </td>
                        </tr>
                    """);
        }
        html.append("""
                      </tbody>
                    </table>
                  </main>
                </body>
                </html>
                """);
        return html.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
