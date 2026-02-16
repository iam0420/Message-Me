package com.chatapp.dto.response;

import com.chatapp.model.enums.MessageStatus;
import com.chatapp.model.enums.MessageType;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long receiverId;
    private Long groupId;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private LocalDateTime timestamp;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private String replyToMessageId;
}
