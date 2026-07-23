package com.achobeta.refine.identity.account.infrastructure.cache;

import com.achobeta.refine.identity.account.application.port.VerificationCodeStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisVerificationCodeStore implements VerificationCodeStore {
    private static final String PREFIX = "identity:email-code:";
    private final StringRedisTemplate redis;
    public RedisVerificationCodeStore(StringRedisTemplate redis) { this.redis = redis; }
    @Override public void save(String account, String code, Duration ttl) { redis.opsForValue().set(PREFIX + account, code, ttl); }
    @Override public String find(String account) { return redis.opsForValue().get(PREFIX + account); }
    @Override public void delete(String account) { redis.delete(PREFIX + account); }
}
