package com.chatapp.controller;

import com.chatapp.dto.response.*;
import com.chatapp.security.UserPrincipal;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/users") @RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> me(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success("Current user", userService.getUserProfile(p.getId())));
    }
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User", userService.getUserProfile(userId)));
    }
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success("Results", userService.searchUsers(keyword)));
    }
    @GetMapping
    public ResponseEntity<ApiResponse> all(@AuthenticationPrincipal UserPrincipal p) {
        List<UserResponse> users = userService.getAllUsers();
        users.removeIf(u -> u.getId().equals(p.getId()));
        return ResponseEntity.ok(ApiResponse.success("Users", users));
    }
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required=false) String displayName, @RequestParam(required=false) String about,
            @RequestParam(required=false) String profilePicture) {
        return ResponseEntity.ok(ApiResponse.success("Updated", userService.updateProfile(p.getId(), displayName, about, profilePicture)));
    }
    @GetMapping("/{userId}/online")
    public ResponseEntity<ApiResponse> isOnline(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Status", userService.isUserOnline(userId)));
    }
}
