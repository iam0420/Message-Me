package com.chatapp.dto.request;

import lombok.Data;

@Data
public class TypingIndicatorRequest {
    private Long receiverId;
    private Long groupId;
    private boolean typing;
}
