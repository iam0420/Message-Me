package com.chatapp.service;

import com.chatapp.dto.request.ChatMessageRequest;
import com.chatapp.dto.response.MessageResponse;
import com.chatapp.model.*;
import com.chatapp.model.enums.MessageStatus;
import com.chatapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service @Slf4j @RequiredArgsConstructor
public class ChatService {
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserService userService;
    private final OnlineStatusService onlineStatusService;

    @Transactional
    public MessageResponse sendMessage(Long senderId, ChatMessageRequest req) {
        User sender = userService.getUserById(senderId);
        User receiver = userService.getUserById(req.getReceiverId());
        ChatRoom chatRoom = getOrCreateChatRoom(sender, receiver);
        Message msg = Message.builder().sender(sender).receiver(receiver).chatRoom(chatRoom)
            .content(req.getContent()).type(req.getType()).fileUrl(req.getFileUrl())
            .fileName(req.getFileName()).fileSize(req.getFileSize())
            .replyToMessageId(req.getReplyToMessageId()).status(MessageStatus.SENT).build();
        if (onlineStatusService.isUserOnline(receiver.getId())) {
            msg.setStatus(MessageStatus.DELIVERED); msg.setDeliveredAt(LocalDateTime.now());
        }
        msg = messageRepository.save(msg);
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);
        return mapToResponse(msg);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getChatHistory(Long u1, Long u2, int page, int size) {
        String chatRoomId = generateChatRoomId(u1, u2);
        Page<Message> msgs = messageRepository.findByChatRoomId(chatRoomId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
        return msgs.getContent().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(Long userId, Long otherUserId) {
        String chatRoomId = generateChatRoomId(userId, otherUserId);
        messageRepository.markAsRead(chatRoomId, userId, MessageStatus.READ);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getUserChatRooms(Long userId) { return chatRoomRepository.findByUserId(userId); }

    public long getUnreadCount(Long userId) { return messageRepository.countUnreadMessages(userId); }

    private ChatRoom getOrCreateChatRoom(User u1, User u2) {
        String id = generateChatRoomId(u1.getId(), u2.getId());
        return chatRoomRepository.findByChatRoomId(id).orElseGet(() -> {
            ChatRoom r = ChatRoom.builder().chatRoomId(id)
                .user1(u1.getId() < u2.getId() ? u1 : u2)
                .user2(u1.getId() < u2.getId() ? u2 : u1).build();
            return chatRoomRepository.save(r);
        });
    }

    public String generateChatRoomId(Long u1, Long u2) {
        return Math.min(u1,u2) + "_" + Math.max(u1,u2);
    }

    public MessageResponse mapToResponse(Message m) {
        return MessageResponse.builder().id(m.getId()).senderId(m.getSender().getId())
            .senderName(m.getSender().getDisplayName()).senderAvatar(m.getSender().getProfilePicture())
            .receiverId(m.getReceiver() != null ? m.getReceiver().getId() : null)
            .groupId(m.getGroup() != null ? m.getGroup().getId() : null)
            .content(m.getContent()).type(m.getType()).status(m.getStatus())
            .fileUrl(m.getFileUrl()).fileName(m.getFileName()).fileSize(m.getFileSize())
            .timestamp(m.getTimestamp()).deliveredAt(m.getDeliveredAt()).readAt(m.getReadAt())
            .replyToMessageId(m.getReplyToMessageId()).build();
    }
}
