package com.chatapp.controller;

import com.chatapp.dto.response.*;
import com.chatapp.model.ChatRoom;
import com.chatapp.security.UserPrincipal;
import com.chatapp.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/chat") @RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final UserService userService;

    @GetMapping("/history/{userId}")
    public ResponseEntity<ApiResponse> history(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long userId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ResponseEntity.ok(ApiResponse.success("History", chatService.getChatHistory(p.getId(), userId, page, size)));
    }
    @PostMapping("/read/{userId}")
    public ResponseEntity<ApiResponse> markRead(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long userId) {
        chatService.markMessagesAsRead(p.getId(), userId);
        return ResponseEntity.ok(ApiResponse.success("Marked as read"));
    }
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse> conversations(@AuthenticationPrincipal UserPrincipal p) {
        List<ChatRoom> rooms = chatService.getUserChatRooms(p.getId());
        List<Map<String, Object>> convos = rooms.stream().map(r -> {
            Map<String, Object> c = new HashMap<>();
            Long otherId = r.getUser1().getId().equals(p.getId()) ? r.getUser2().getId() : r.getUser1().getId();
            c.put("chatRoomId", r.getChatRoomId()); c.put("user", userService.getUserProfile(otherId));
            c.put("lastMessageAt", r.getLastMessageAt()); return c;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Conversations", convos));
    }
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse> unread(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success("Count", chatService.getUnreadCount(p.getId())));
    }
}
