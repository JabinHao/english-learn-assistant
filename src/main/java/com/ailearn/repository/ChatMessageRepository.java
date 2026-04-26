package com.ailearn.repository;

import com.ailearn.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);
}
