package com.chatapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service @Slf4j @RequiredArgsConstructor
public class OnlineStatusService {
    private static final String ONLINE_PREFIX = "user:online:";
    private final RedisService redisService;

    public void setUserOnline(Long userId) {
        redisService.setWithExpiry(ONLINE_PREFIX + userId, "true", 5, TimeUnit.MINUTES);
    }
    public void setUserOffline(Long userId) { redisService.delete(ONLINE_PREFIX + userId); }
    public boolean isUserOnline(Long userId) { return redisService.exists(ONLINE_PREFIX + userId); }
    public void refreshOnlineStatus(Long userId) { if (isUserOnline(userId)) setUserOnline(userId); }
}
