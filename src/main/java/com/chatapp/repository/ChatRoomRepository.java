package com.chatapp.repository;

import com.chatapp.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByChatRoomId(String chatRoomId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.user1.id = :userId OR cr.user2.id = :userId ORDER BY cr.lastMessageAt DESC NULLS LAST")
    List<ChatRoom> findByUserId(Long userId);

    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.user1.id = :u1 AND cr.user2.id = :u2) OR (cr.user1.id = :u2 AND cr.user2.id = :u1)")
    Optional<ChatRoom> findByUsers(Long u1, Long u2);
}
