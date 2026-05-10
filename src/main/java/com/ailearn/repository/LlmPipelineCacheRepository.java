package com.ailearn.repository;

import com.ailearn.entity.LlmPipelineCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LlmPipelineCacheRepository extends JpaRepository<LlmPipelineCacheEntry, Long> {

    Optional<LlmPipelineCacheEntry> findByCacheKey(String cacheKey);
}
