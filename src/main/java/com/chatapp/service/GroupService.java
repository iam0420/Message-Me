package com.chatapp.service;

import com.chatapp.dto.request.*;
import com.chatapp.dto.response.*;
import com.chatapp.exception.*;
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
public class GroupService {
    private final GroupChatRepository groupChatRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final ChatService chatService;

    @Transactional
    public GroupResponse createGroup(Long creatorId, GroupCreateRequest req) {
        User creator = userService.getUserById(creatorId);
        GroupChat group = GroupChat.builder().name(req.getName()).description(req.getDescription()).createdBy(creator).build();
        group = groupChatRepository.save(group);
        groupMemberRepository.save(GroupMember.builder().group(group).user(creator).isAdmin(true).build());
        for (Long mid : req.getMemberIds()) {
            if (!mid.equals(creatorId)) {
                User member = userService.getUserById(mid);
                groupMemberRepository.save(GroupMember.builder().group(group).user(member).isAdmin(false).build());
            }
        }
        return mapToResponse(group);
    }

    @Transactional
    public MessageResponse sendGroupMessage(Long senderId, ChatMessageRequest req) {
        User sender = userService.getUserById(senderId);
        GroupChat group = groupChatRepository.findById(req.getGroupId())
            .orElseThrow(() -> new ResourceNotFoundException("Group","id",req.getGroupId()));
        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), senderId))
            throw new BadRequestException("You are not a member of this group");
        Message msg = Message.builder().sender(sender).group(group).content(req.getContent())
            .type(req.getType()).status(MessageStatus.SENT).fileUrl(req.getFileUrl())
            .fileName(req.getFileName()).fileSize(req.getFileSize()).build();
        msg = messageRepository.save(msg);
        group.setLastMessageAt(LocalDateTime.now()); groupChatRepository.save(group);
        return chatService.mapToResponse(msg);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getGroupMessages(Long groupId, int page, int size) {
        Page<Message> msgs = messageRepository.findByGroupId(groupId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
        return msgs.getContent().stream().map(chatService::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(Long userId) {
        return groupChatRepository.findByUserId(userId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void addMember(Long groupId, Long userId, Long addedBy) {
        groupChatRepository.findById(groupId).orElseThrow(() -> new ResourceNotFoundException("Group","id",groupId));
        GroupMember admin = groupMemberRepository.findByGroupIdAndUserId(groupId, addedBy)
            .orElseThrow(() -> new BadRequestException("Not a member"));
        if (!admin.getIsAdmin()) throw new BadRequestException("Only admins can add");
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) throw new BadRequestException("Already a member");
        User user = userService.getUserById(userId);
        GroupChat group = groupChatRepository.findById(groupId).get();
        groupMemberRepository.save(GroupMember.builder().group(group).user(user).isAdmin(false).build());
    }

    @Transactional
    public void removeMember(Long groupId, Long userId, Long removedBy) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new BadRequestException("Not a member"));
        groupMemberRepository.delete(member);
    }

    public List<Long> getGroupMemberIds(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId).stream().map(gm -> gm.getUser().getId()).collect(Collectors.toList());
    }

    private GroupResponse mapToResponse(GroupChat g) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(g.getId());
        return GroupResponse.builder().id(g.getId()).name(g.getName()).description(g.getDescription())
            .groupPicture(g.getGroupPicture()).createdById(g.getCreatedBy().getId())
            .createdByName(g.getCreatedBy().getDisplayName())
            .members(members.stream().map(m -> userService.mapToResponse(m.getUser())).collect(Collectors.toList()))
            .createdAt(g.getCreatedAt()).lastMessageAt(g.getLastMessageAt()).build();
    }
}
