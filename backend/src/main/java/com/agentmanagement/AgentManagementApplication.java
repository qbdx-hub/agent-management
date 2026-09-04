package com.agentmanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 灵枢agent后端启动类。
 * 根包 com.agentmanagement，保证 @SpringBootApplication 默认扫描整个根包。
 * @MapperScan 必须显式指到 com.agentmanagement.mapper，否则 MyBatis-Plus 不注入 Mapper。
 * @EnableAsync 使 @Async（如文档向量化处理）真正异步执行，不阻塞 HTTP 请求线程。
 * @EnableScheduling 使告警评估等 @Scheduled 定时任务生效。
 */
@SpringBootApplication
@MapperScan("com.agentmanagement.mapper")
@EnableAsync
@EnableScheduling
public class AgentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentManagementApplication.class, args);
    }
}
