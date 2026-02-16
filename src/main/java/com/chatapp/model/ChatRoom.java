package com.chatapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String chatRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastMessageAt;
}
