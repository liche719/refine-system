package com.achobeta.refine.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 显式声明 TransactionTemplate。
 *
 * <p>{@code LearningAnalysisService#consume} 需要「远程调用在事务外、数据库写在短事务内」，
 * 因此依赖编程式事务。虽然 Spring Boot 在只有一个事务管理器时会自动装配，但这里显式声明，
 * 避免以后有人再引入第二个数据源/事务管理器时，应用在启动期才暴露缺少 bean 的问题。
 */
@Configuration
public class TransactionConfiguration {

    @Bean
    @ConditionalOnMissingBean(TransactionTemplate.class)
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
