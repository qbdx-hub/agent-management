package com.agentmanagement.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器：从 Authorization 头解析 token，成功则把用户身份塞入 SecurityContext。
 * 解析失败不抛异常、清空上下文放行，由后续 anyRequest().authenticated() + RestAuthenticationEntryPoint
 * 统一返回 401。这样 /auth/login（permitAll）即使带了坏 token 也能正常进入。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, JwtProperties jwtProperties) {
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(jwtProperties.getHeader());
        // 无 token 或非 "Bearer " 开头，直接放行
        if (!StringUtils.hasText(header) || !header.startsWith(jwtProperties.getPrefix())) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(jwtProperties.getPrefix().length()).trim();
        try {
            if (jwtUtils.validateToken(token)) {
                Long userId = jwtUtils.getUserIdFromToken(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.debug("JWT 解析失败，保持未认证: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
