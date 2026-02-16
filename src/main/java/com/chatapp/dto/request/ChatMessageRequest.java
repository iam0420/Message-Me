package com.chatapp.dto.request;

import com.chatapp.model.enums.MessageType;
import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long receiverId;
    private Long groupId;
    private String content;
    private MessageType type = MessageType.TEXT;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String replyToMessageId;
}
