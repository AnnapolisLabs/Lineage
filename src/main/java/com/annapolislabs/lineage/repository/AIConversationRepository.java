package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.AIConversation;
import com.annapolislabs.lineage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AIConversation entity
 */
@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, UUID> {

    Optional<AIConversation> findByChatIdAndUser(String chatId, User user);

    List<AIConversation> findByUserOrderByUpdatedAtDesc(User user);

    @Modifying
    @Transactional
    void deleteByChatIdAndUser(String chatId, User user);
}
