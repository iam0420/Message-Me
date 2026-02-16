package com.chatapp.dto.response;

import lombok.*;

@Data @Builder @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String username;
    private String displayName;
    private String email;
}
