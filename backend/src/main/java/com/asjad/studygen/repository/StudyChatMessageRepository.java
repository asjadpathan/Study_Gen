package com.asjad.studygen.repository;

import com.asjad.studygen.entity.StudyChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyChatMessageRepository extends JpaRepository<StudyChatMessage, Long> {
    List<StudyChatMessage> findByUserIdAndModuleIdOrderByCreatedAtAsc(Long userId, Long moduleId);
    List<StudyChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);
    List<StudyChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
