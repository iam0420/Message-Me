package com.chatapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Fallback in-memory store if Redis is down
    private final Map<String, Object> fallbackStore = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            redisAvailable = true;
        } catch (Exception e) {
            handleRedisError("set", e);
            fallbackStore.put(key, value);
        }
    }

    public void setWithExpiry(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            redisAvailable = true;
        } catch (Exception e) {
            handleRedisError("setWithExpiry", e);
            fallbackStore.put(key, value);
            // Auto-remove from fallback after timeout
            scheduleRemoval(key, unit.toMillis(timeout));
        }
    }

    public Object get(String key) {
        try {
            Object val = redisTemplate.opsForValue().get(key);
            redisAvailable = true;
            return val;
        } catch (Exception e) {
            handleRedisError("get", e);
            return fallbackStore.get(key);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            redisAvailable = true;
        } catch (Exception e) {
            handleRedisError("delete", e);
        }
        fallbackStore.remove(key);
    }

    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            redisAvailable = true;
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            handleRedisError("exists", e);
            return fallbackStore.containsKey(key);
        }
    }

    private void handleRedisError(String operation, Exception e) {
        if (redisAvailable) {
            log.warn("Redis unavailable for '{}', using fallback: {}", operation, e.getMessage());
            redisAvailable = false;
        }
    }

    private void scheduleRemoval(String key, long delayMs) {
        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                fallbackStore.remove(key);
            }
        }, delayMs);
    }
}