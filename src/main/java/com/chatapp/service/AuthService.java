package com.chatapp.service;

import com.chatapp.dto.request.*;
import com.chatapp.dto.response.AuthResponse;
import com.chatapp.exception.BadRequestException;
import com.chatapp.model.User;
import com.chatapp.model.enums.UserStatus;
import com.chatapp.repository.UserRepository;
import com.chatapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Slf4j @RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final OnlineStatusService onlineStatusService;

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) throw new BadRequestException("Username already taken");
        if (userRepository.existsByEmail(req.getEmail())) throw new BadRequestException("Email already registered");
        User user = User.builder().username(req.getUsername()).email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .displayName(req.getDisplayName() != null ? req.getDisplayName() : req.getUsername())
            .phoneNumber(req.getPhoneNumber()).status(UserStatus.ONLINE).about("Hey there! I'm using ChatApp").build();
        user = userRepository.save(user);
        String token = tokenProvider.generateTokenFromUserId(user.getId(), user.getUsername());
        onlineStatusService.setUserOnline(user.getId());
        return AuthResponse.builder().accessToken(token).tokenType("Bearer").userId(user.getId())
            .username(user.getUsername()).displayName(user.getDisplayName()).email(user.getEmail()).build();
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        String token = tokenProvider.generateToken(auth);
        User user = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new BadRequestException("User not found"));
        user.setStatus(UserStatus.ONLINE); userRepository.save(user);
        onlineStatusService.setUserOnline(user.getId());
        return AuthResponse.builder().accessToken(token).tokenType("Bearer").userId(user.getId())
            .username(user.getUsername()).displayName(user.getDisplayName()).email(user.getEmail()).build();
    }
}
