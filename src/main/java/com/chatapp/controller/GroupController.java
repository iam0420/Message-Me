package com.chatapp.controller;

import com.chatapp.dto.request.GroupCreateRequest;
import com.chatapp.dto.response.ApiResponse;
import com.chatapp.security.UserPrincipal;
import com.chatapp.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/groups") @RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<ApiResponse> create(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody GroupCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Group created", groupService.createGroup(p.getId(), req)));
    }
    @GetMapping
    public ResponseEntity<ApiResponse> groups(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(ApiResponse.success("Groups", groupService.getUserGroups(p.getId())));
    }
    @GetMapping("/{groupId}/messages")
    public ResponseEntity<ApiResponse> messages(@PathVariable Long groupId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return ResponseEntity.ok(ApiResponse.success("Messages", groupService.getGroupMessages(groupId, page, size)));
    }
    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse> addMember(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long groupId, @PathVariable Long userId) {
        groupService.addMember(groupId, userId, p.getId());
        return ResponseEntity.ok(ApiResponse.success("Member added"));
    }
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<ApiResponse> removeMember(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(groupId, userId, p.getId());
        return ResponseEntity.ok(ApiResponse.success("Member removed"));
    }
}
