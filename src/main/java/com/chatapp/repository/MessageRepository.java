package com.chatapp.repository;

import com.chatapp.model.Message;
import com.chatapp.model.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE m.chatRoom.chatRoomId = :chatRoomId AND m.deleted = false ORDER BY m.timestamp DESC")
    Page<Message> findByChatRoomId(String chatRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId AND m.deleted = false ORDER BY m.timestamp DESC")
    Page<Message> findByGroupId(Long groupId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.status = :status, m.deliveredAt = CURRENT_TIMESTAMP WHERE m.receiver.id = :receiverId AND m.status = com.chatapp.model.enums.MessageStatus.SENT")
    int markAsDelivered(Long receiverId, MessageStatus status);

    @Modifying
    @Query("UPDATE Message m SET m.status = :status, m.readAt = CURRENT_TIMESTAMP WHERE m.chatRoom.chatRoomId = :chatRoomId AND m.receiver.id = :userId AND m.status <> com.chatapp.model.enums.MessageStatus.READ")
    int markAsRead(String chatRoomId, Long userId, MessageStatus status);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.status <> com.chatapp.model.enums.MessageStatus.READ AND m.deleted = false")
    long countUnreadMessages(Long userId);
}
