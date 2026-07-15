package com.agentmanagement.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（对应 application.yml 的 jwt.* 段）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥，HS256 要求 >= 32 字节，否则 Keys.hmacShaKeyFor 抛 WeakKeyException */
    private String secret = "agent_management_default_jwt_secret_key_at_least_32_bytes";

    /** 有效期，毫秒，默认 24h */
    private long expiration = 86400000L;

    /** 请求头名称 */
    private String header = "Authorization";

    /** token 前缀（含尾随空格） */
    private String prefix = "Bearer ";
}
