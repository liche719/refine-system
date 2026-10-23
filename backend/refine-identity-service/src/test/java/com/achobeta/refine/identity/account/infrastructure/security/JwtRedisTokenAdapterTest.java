package com.achobeta.refine.identity.account.infrastructure.security;

import com.achobeta.refine.common.api.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtRedisTokenAdapterTest {
    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";
    private static final String TOKEN_VERSION_PREFIX = "identity:token-version:";

    @Test
    void issuesAndRefreshesActiveToken() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(any())).thenReturn("user-1");
        JwtRedisTokenAdapter adapter = new JwtRedisTokenAdapter(redis, SECRET,
                Duration.ofMinutes(5), Duration.ofDays(1));

        var pair = adapter.issue("user-1");

        assertThat(adapter.refresh(pair.refreshToken()).newAccessToken()).isNotBlank();
    }

    @Test
    void rejectsRefreshTokenMissingFromRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        JwtRedisTokenAdapter adapter = new JwtRedisTokenAdapter(redis, SECRET,
                Duration.ofMinutes(5), Duration.ofDays(1));
        String token = adapter.issue("user-1").refreshToken();
        when(values.get(any())).thenReturn(null);

        assertThatThrownBy(() -> adapter.refresh(token)).isInstanceOf(AppException.class);
    }

    @Test
    void rejectsRefreshTokenAfterAllTokensAreRevoked() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AtomicLong version = new AtomicLong(0);
        when(values.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return key.startsWith(TOKEN_VERSION_PREFIX) ? String.valueOf(version.get()) : "user-1";
        });
        JwtRedisTokenAdapter adapter = new JwtRedisTokenAdapter(redis, SECRET,
                Duration.ofMinutes(5), Duration.ofDays(1));
        String refreshToken = adapter.issue("user-1").refreshToken();
        assertThat(adapter.refresh(refreshToken).newAccessToken()).isNotBlank();

        // 模拟改密：版本自增，旧 refresh token 里的 ver 对不上
        version.incrementAndGet();
        adapter.revokeAll("user-1");

        assertThatThrownBy(() -> adapter.refresh(refreshToken))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getCode()).isEqualTo(401));
    }

    @Test
    void acceptsTokensIssuedAfterRevocation() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        AtomicLong version = new AtomicLong(3);
        when(values.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return key.startsWith(TOKEN_VERSION_PREFIX) ? String.valueOf(version.get()) : "user-1";
        });
        JwtRedisTokenAdapter adapter = new JwtRedisTokenAdapter(redis, SECRET,
                Duration.ofMinutes(5), Duration.ofDays(1));

        String refreshToken = adapter.issue("user-1").refreshToken();

        assertThat(adapter.refresh(refreshToken).newAccessToken()).isNotBlank();
    }
}
