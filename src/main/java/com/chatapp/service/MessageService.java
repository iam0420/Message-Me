package com.chatapp.service;

import com.chatapp.dto.response.MessageResponse;
import com.chatapp.model.Message;
import com.chatapp.model.enums.MessageStatus;
import com.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatService chatService;

    @Transactional
    public MessageResponse updateMessageStatus(Long messageId, MessageStatus status) {
        Message msg = messageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("Message not found"));
        msg.setStatus(status);
        if (status == MessageStatus.DELIVERED) msg.setDeliveredAt(LocalDateTime.now());
        else if (status == MessageStatus.READ) msg.setReadAt(LocalDateTime.now());
        return chatService.mapToResponse(messageRepository.save(msg));
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message msg = messageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("Message not found"));
        if (!msg.getSender().getId().equals(userId)) throw new RuntimeException("Not authorized");
        msg.setDeleted(true); messageRepository.save(msg);
    }
}
