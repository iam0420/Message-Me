package com.chatapp.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @AllArgsConstructor
public class GroupResponse {
    private Long id;
    private String name;
    private String description;
    private String groupPicture;
    private Long createdById;
    private String createdByName;
    private List<UserResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
}
