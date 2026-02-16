package com.chatapp.service;

import com.chatapp.dto.response.UserResponse;
import com.chatapp.exception.ResourceNotFoundException;
import com.chatapp.model.User;
import com.chatapp.model.enums.UserStatus;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OnlineStatusService onlineStatusService;

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
    }
    public UserResponse getUserProfile(Long userId) { return mapToResponse(getUserById(userId)); }
    public List<UserResponse> searchUsers(String kw) {
        return userRepository.searchUsers(kw).stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User u = getUserById(userId);
        u.setStatus(status);
        if (status == UserStatus.OFFLINE) u.setLastSeen(LocalDateTime.now());
        userRepository.save(u);
        if (status == UserStatus.ONLINE) onlineStatusService.setUserOnline(userId);
        else onlineStatusService.setUserOffline(userId);
    }
    @Transactional
    public UserResponse updateProfile(Long userId, String dn, String about, String pp) {
        User u = getUserById(userId);
        if (dn != null) u.setDisplayName(dn);
        if (about != null) u.setAbout(about);
        if (pp != null) u.setProfilePicture(pp);
        return mapToResponse(userRepository.save(u));
    }
    public boolean isUserOnline(Long userId) { return onlineStatusService.isUserOnline(userId); }

    public UserResponse mapToResponse(User u) {
        boolean online = onlineStatusService.isUserOnline(u.getId());
        return UserResponse.builder().id(u.getId()).username(u.getUsername()).email(u.getEmail())
            .displayName(u.getDisplayName()).profilePicture(u.getProfilePicture()).about(u.getAbout())
            .status(online ? UserStatus.ONLINE : UserStatus.OFFLINE).lastSeen(u.getLastSeen())
            .phoneNumber(u.getPhoneNumber()).build();
    }
}
