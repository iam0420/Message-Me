package com.chatapp.controller;

import com.chatapp.dto.request.*;
import com.chatapp.dto.response.MessageResponse;
import com.chatapp.model.enums.*;
import com.chatapp.security.UserPrincipal;
import com.chatapp.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.*;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller @Slf4j @RequiredArgsConstructor
public class WebSocketChatController {
    private final ChatService chatService;
    private final GroupService groupService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final OnlineStatusService onlineStatusService;
    private final MessageService messageService;
    private static final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest req, SimpMessageHeaderAccessor ha) {
        Principal p = ha.getUser(); if (p == null) return;
        UserPrincipal up = getUP(p); Long senderId = up.getId();
        if (req.getGroupId() != null) {
            MessageResponse res = groupService.sendGroupMessage(senderId, req);
            List<Long> memberIds = groupService.getGroupMemberIds(req.getGroupId());
            notificationService.sendGroupMessage(req.getGroupId(), res, memberIds);
        } else {
            MessageResponse res = chatService.sendMessage(senderId, req);
            notificationService.sendPrivateMessage(req.getReceiverId(), res);
            notificationService.sendPrivateMessage(senderId, res);
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicatorRequest req, SimpMessageHeaderAccessor ha) {
        Principal p = ha.getUser(); if (p == null) return;
        UserPrincipal up = getUP(p);
        if (req.getGroupId() != null) {
            List<Long> ids = groupService.getGroupMemberIds(req.getGroupId());
            for (Long id : ids) if (!id.equals(up.getId()))
                notificationService.sendTypingIndicator(id, up.getId(), up.getDisplayName(), req.isTyping());
        } else if (req.getReceiverId() != null) {
            notificationService.sendTypingIndicator(req.getReceiverId(), up.getId(), up.getDisplayName(), req.isTyping());
        }
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor ha) {
        if (ha.getUser() == null) return;
        Long messageId = Long.parseLong(payload.get("messageId").toString());
        MessageResponse updated = messageService.updateMessageStatus(messageId, MessageStatus.READ);
        notificationService.sendMessageStatusUpdate(updated.getSenderId(), messageId, "READ");
    }

    @MessageMapping("/chat.delivered")
    public void markAsDelivered(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor ha) {
        if (ha.getUser() == null) return;
        Long messageId = Long.parseLong(payload.get("messageId").toString());
        MessageResponse updated = messageService.updateMessageStatus(messageId, MessageStatus.DELIVERED);
        notificationService.sendMessageStatusUpdate(updated.getSenderId(), messageId, "DELIVERED");
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        try {
            if (event.getUser() != null) {
                UserPrincipal up = getUP(event.getUser());
                String sid = event.getMessage().getHeaders().get("simpSessionId", String.class);
                sessionUserMap.put(sid, up.getId());
                onlineStatusService.setUserOnline(up.getId());
                userService.updateUserStatus(up.getId(), UserStatus.ONLINE);
                notificationService.sendOnlineStatus(up.getId(), true);
                log.info("WS connected: {}", up.getUsername());
            }
        } catch (Exception e) { log.error("Connect error: {}", e.getMessage()); }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        try {
            Long userId = sessionUserMap.remove(event.getSessionId());
            if (userId != null) {
                onlineStatusService.setUserOffline(userId);
                userService.updateUserStatus(userId, UserStatus.OFFLINE);
                notificationService.sendOnlineStatus(userId, false);
                log.info("WS disconnected: {}", userId);
            }
        } catch (Exception e) { log.error("Disconnect error: {}", e.getMessage()); }
    }

    private UserPrincipal getUP(Principal p) {
        return (UserPrincipal) ((UsernamePasswordAuthenticationToken) p).getPrincipal();
    }
}
