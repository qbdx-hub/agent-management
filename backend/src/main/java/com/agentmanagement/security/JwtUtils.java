package com.agentmanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具（jjwt 0.11.5 API）。
 * 三连坑：parserBuilder()（非 parser()）、Keys.hmacShaKeyFor()（非 secretKeyFor()）、
 *         signWith(Key, SignatureAlgorithm)（二参重载，非废弃的单参 signWith(Key)）。
 */
@Slf4j
@Component
public class JwtUtils {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtUtils(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 token：subject 放 userId，自定义 claim 放 username */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + properties.getExpiration());
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).get("username", String.class);
    }

    /** 取过期时间，供登录响应 expiresAt 使用 */
    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    /** 校验 token 是否有效（签名正确且未过期） */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("token 校验失败: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
