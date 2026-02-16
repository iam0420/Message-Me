package com.chatapp.dto.response;

import com.chatapp.model.enums.UserStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String profilePicture;
    private String about;
    private UserStatus status;
    private LocalDateTime lastSeen;
    private String phoneNumber;
}
