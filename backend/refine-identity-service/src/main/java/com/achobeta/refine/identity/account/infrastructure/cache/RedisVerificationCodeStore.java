package com.achobeta.refine.identity.account.infrastructure.cache;

import com.achobeta.refine.identity.account.application.port.VerificationCodeStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisVerificationCodeStore implements VerificationCodeStore {
    private static final String CODE_PREFIX = "identity:email-code:";
    private static final String ATTEMPT_PREFIX = "identity:email-code:attempts:";
    private static final String SEND_PREFIX = "identity:email-code:send:";

    private final StringRedisTemplate redis;

    public RedisVerificationCodeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(String account, String code, Duration ttl) {
        redis.opsForValue().set(CODE_PREFIX + account, code, ttl);
    }

    @Override
    public String find(String account) {
        return redis.opsForValue().get(CODE_PREFIX + account);
    }

    @Override
    public void delete(String account) {
        redis.delete(CODE_PREFIX + account);
    }

    @Override
    public long recordFailedAttempt(String account, Duration window) {
        String key = ATTEMPT_PREFIX + account;
        Long attempts = redis.opsForValue().increment(key);
        if (attempts == null) {
            return 0;
        }
        if (attempts == 1L) {
            redis.expire(key, window);
        }
        return attempts;
    }

    @Override
    public void clearFailedAttempts(String account) {
        redis.delete(ATTEMPT_PREFIX + account);
    }

    @Override
    public boolean acquireSendSlot(String account, Duration cooldown) {
        Boolean acquired = redis.opsForValue().setIfAbsent(SEND_PREFIX + account, "1", cooldown);
        return Boolean.TRUE.equals(acquired);
    }
}
