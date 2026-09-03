package com.agentmanagement.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置。
 * 文档向量化（processDocument）是 IO 密集型且依赖外部 embedding API，
 * 需要限制并发防止打爆上游接口；不配置时 @Async 会回退 SimpleAsyncTaskExecutor（每任务新建线程，无上限）。
 */
@Configuration
public class AsyncConfig {

    @Bean("docProcessExecutor")
    public Executor docProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-process-");
        // 队列满时由调用线程执行，起到天然背压作用，不丢任务
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /** 对话 SSE 推流线程池：单次任务持续 1-5 分钟，需与容器线程隔离且有并发上限（原实现裸 new Thread 无上限，高并发会线程耗尽） */
    @Bean("chatExecutor")
    public Executor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("chat-sse-");
        // 池满时由调用线程执行（降级为同步推流），不拒绝任务
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 工作流执行线程池：一次运行可能串行调用多次 AI/HTTP，耗时可达分钟级，
     * 独立线程池避免占满 chatExecutor；审批恢复（approveRun）也走该池。
     */
    @Bean("workflowExecutor")
    public Executor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("workflow-exec-");
        // 池满时由调用线程同步执行，不丢运行任务
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
