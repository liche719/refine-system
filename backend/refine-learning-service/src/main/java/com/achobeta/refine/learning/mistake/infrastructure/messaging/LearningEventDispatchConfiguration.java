package com.achobeta.refine.learning.mistake.infrastructure.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 领域事件发布线程池。
 *
 * <p>发布要等 broker confirm，本身就是阻塞操作。如果放在请求线程的 afterCommit 里同步执行，
 * 单次请求最坏会被拖住「重试次数 × confirm 超时」，而且此时数据库连接还没归还，
 * broker 抖动时会连带把连接池占满。这里用容量有限、线程固定的池把发布与请求线程解耦。
 */
@Configuration
public class LearningEventDispatchConfiguration {

    @Bean(name = "learningEventExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor learningEventExecutor(
            @Value("${refine.events.dispatch.threads:2}") int threads,
            @Value("${refine.events.dispatch.queue-capacity:2000}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("learning-event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        // 队列满时直接拒绝并计数，不用 CallerRuns：否则又退化回阻塞请求线程
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
