package com.chatapp.repository;

import com.chatapp.model.GroupChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    @Query("SELECT g FROM GroupChat g JOIN g.members m WHERE m.user.id = :userId ORDER BY g.lastMessageAt DESC NULLS LAST")
    List<GroupChat> findByUserId(Long userId);
}
