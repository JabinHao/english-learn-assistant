package com.ailearn.service.learning;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ParagraphSplitService {

    public List<String> split(String articleContent) {
        if (articleContent == null || articleContent.isBlank()) {
            return List.of();
        }

        return Arrays.stream(articleContent.trim().split("\\n\\s*\\n"))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .toList();
    }
}
