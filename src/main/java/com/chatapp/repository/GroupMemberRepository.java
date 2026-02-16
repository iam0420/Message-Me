package com.chatapp.repository;

import com.chatapp.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId")
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId")
    List<GroupMember> findByGroupId(Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
}
