package com.agentmanagement.configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件配置（MP 3.5.x 用 MybatisPlusInterceptor，非已移除的 PaginationInterceptor）。
 * 不注册则 IPage/Page 查询不带 LIMIT，分页失效。
 */
@Configuration
public class PageConfiguration {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pageInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页上限，防恶意大页
        pageInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pageInterceptor);
        return interceptor;
    }
}
