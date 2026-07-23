package com.achobeta.refine.ai.learning.infrastructure;

import com.achobeta.refine.common.security.IdentityHeaders;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class LearningClientConfiguration {
    @Bean
    RequestInterceptor internalTokenInterceptor(@Value("${refine.security.internal-token}") String token) {
        return template -> template.header(IdentityHeaders.INTERNAL_TOKEN, token);
    }
}
