package com.agentmanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agent 管理系统后端启动类。
 * 根包 com.agentmanagement，保证 @SpringBootApplication 默认扫描整个根包。
 * @MapperScan 必须显式指到 com.agentmanagement.mapper，否则 MyBatis-Plus 不注入 Mapper。
 */
@SpringBootApplication
@MapperScan("com.agentmanagement.mapper")
public class AgentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentManagementApplication.class, args);
    }
}
