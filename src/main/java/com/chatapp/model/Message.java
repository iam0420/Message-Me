package com.chatapp.model;

import com.chatapp.model.enums.MessageStatus;
import com.chatapp.model.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_msg_sender", columnList = "sender_id"),
    @Index(name = "idx_msg_receiver", columnList = "receiver_id"),
    @Index(name = "idx_msg_chatroom", columnList = "chat_room_id"),
    @Index(name = "idx_msg_group", columnList = "group_id"),
    @Index(name = "idx_msg_ts", columnList = "timestamp")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupChat group;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(length = 500)
    private String fileUrl;

    @Column(length = 100)
    private String fileName;

    private Long fileSize;

    @CreationTimestamp
    private LocalDateTime timestamp;

    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(length = 500)
    private String replyToMessageId;
}
