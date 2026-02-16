package com.chatapp.service;

import com.chatapp.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @Slf4j @RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    @Async public void sendPrivateMessage(Long receiverId, MessageResponse msg) {
        messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/messages", msg);
    }
    @Async public void sendGroupMessage(Long groupId, MessageResponse msg, List<Long> memberIds) {
        for (Long mid : memberIds) {
            if (!mid.equals(msg.getSenderId()))
                messagingTemplate.convertAndSendToUser(mid.toString(), "/queue/messages", msg);
        }
    }
    @Async public void sendTypingIndicator(Long receiverId, Long senderId, String senderName, boolean typing) {
        Map<String, Object> p = new HashMap<>();
        p.put("senderId", senderId); p.put("senderName", senderName); p.put("typing", typing);
        messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/typing", p);
    }
    @Async public void sendOnlineStatus(Long userId, boolean online) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId); p.put("online", online);
        messagingTemplate.convertAndSend("/topic/online-status", p);
    }
    @Async public void sendMessageStatusUpdate(Long userId, Long messageId, String status) {
        Map<String, Object> p = new HashMap<>();
        p.put("messageId", messageId); p.put("status", status);
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/message-status", p);
    }
}
