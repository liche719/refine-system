package com.achobeta.refine.identity.account.infrastructure.security;

import com.achobeta.refine.common.api.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtRedisTokenAdapterTest {
    @Test
    void issuesAndRefreshesActiveToken() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(any())).thenReturn("user-1");
        JwtRedisTokenAdapter adapter = new JwtRedisTokenAdapter(redis,
                "test-secret-test-secret-test-secret-test-secret", Duration.ofMinutes(5), Duration.ofDays(1));

        var pair = adapter.issue("user-1");

        assertThat(adapter.refresh(pair.refreshToken()).newAccessToken()).isNotBlank();
    }

    @Test
    void rejectsRefreshTokenMissingFromRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        JwtRedisTokenAdapter adapter = new JwtRedisTokenAdapter(redis,
                "test-secret-test-secret-test-secret-test-secret", Duration.ofMinutes(5), Duration.ofDays(1));
        String token = adapter.issue("user-1").refreshToken();
        when(values.get(any())).thenReturn(null);

        assertThatThrownBy(() -> adapter.refresh(token)).isInstanceOf(AppException.class);
    }
}
