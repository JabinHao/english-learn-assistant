package com.ailearn.service.learning;

import com.ailearn.entity.LlmPipelineCacheEntry;
import com.ailearn.repository.LlmPipelineCacheRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class LlmPipelineCacheService {

    private static final Logger log = LoggerFactory.getLogger(LlmPipelineCacheService.class);

    private final ObjectMapper objectMapper;
    private final LlmPipelineCacheRepository repository;

    public LlmPipelineCacheService(ObjectMapper objectMapper, LlmPipelineCacheRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    public <T> Optional<T> read(String operation, String prompt, TypeReference<T> typeReference) {
        String cacheKey = cacheKey(operation, prompt);
        return repository.findByCacheKey(cacheKey)
                .flatMap(entry -> parseCachedResult(operation, cacheKey, entry.getResultJson(), typeReference));
    }

    public void write(String operation, String prompt, Object result) {
        String cacheKey = cacheKey(operation, prompt);
        try {
            LlmPipelineCacheEntry entry = new LlmPipelineCacheEntry();
            entry.setCacheKey(cacheKey);
            entry.setOperation(operation);
            entry.setPromptHash(sha256(prompt));
            entry.setResultJson(objectMapper.writeValueAsString(result));
            repository.save(entry);
            log.info("llm.cache.write operation={} cacheKey={}", operation, shortKey(cacheKey));
        } catch (DataIntegrityViolationException exception) {
            log.info("llm.cache.write_conflict operation={} cacheKey={}", operation, shortKey(cacheKey));
        } catch (JsonProcessingException exception) {
            log.warn("llm.cache.write_failed operation={} cacheKey={} error={}", operation, shortKey(cacheKey), exception.getMessage());
        }
    }

    private <T> Optional<T> parseCachedResult(
            String operation,
            String cacheKey,
            String resultJson,
            TypeReference<T> typeReference
    ) {
        try {
            T result = objectMapper.readValue(resultJson, typeReference);
            log.info("llm.cache.hit operation={} cacheKey={}", operation, shortKey(cacheKey));
            return Optional.of(result);
        } catch (JsonProcessingException exception) {
            log.warn("llm.cache.read_failed operation={} cacheKey={} error={}", operation, shortKey(cacheKey), exception.getMessage());
            return Optional.empty();
        }
    }

    private String cacheKey(String operation, String prompt) {
        return sha256(operation + "\n" + prompt);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String shortKey(String cacheKey) {
        return cacheKey.substring(0, Math.min(12, cacheKey.length()));
    }
}
