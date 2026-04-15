package com.arguing.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RateLimitService {

    private static final int GUEST_DAILY_LIMIT = 5;

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 检查游客用户是否超过每日限制。
     * 如果未超限则递增计数并返回 true，否则返回 false。
     */
    public boolean tryAcquire(Long userId) {
        String key = buildKey(userId);
        String countStr = redisTemplate.opsForValue().get(key);
        int currentCount = (countStr != null) ? Integer.parseInt(countStr) : 0;

        if (currentCount >= GUEST_DAILY_LIMIT) {
            return false;
        }

        // 递增计数，如果 key 不存在则从 1 开始
        Long newCount = redisTemplate.opsForValue().increment(key);

        // 如果是第一次创建 key，设置 TTL 到当天结束
        if (newCount != null && newCount == 1) {
            Duration ttl = Duration.between(LocalDateTime.now(),
                    LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT));
            redisTemplate.expire(key, ttl);
        }

        return true;
    }

    /**
     * 获取当前用户今日已使用次数。
     */
    public int getUsedCount(Long userId) {
        String key = buildKey(userId);
        String countStr = redisTemplate.opsForValue().get(key);
        return (countStr != null) ? Integer.parseInt(countStr) : 0;
    }

    /**
     * 获取剩余可用次数。
     */
    public int getRemainingCount(Long userId) {
        return GUEST_DAILY_LIMIT - getUsedCount(userId);
    }

    private String buildKey(Long userId) {
        return "rate:" + userId + ":" + LocalDate.now();
    }
}
