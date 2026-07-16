package com.agentmanagement.common.aspect;

import com.agentmanagement.common.Result;
import com.agentmanagement.common.annotation.AuditLog;
import com.agentmanagement.entity.User;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.AuditLogService;
import com.agentmanagement.vo.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审计日志切面：拦截所有标注 {@link AuditLog} 的 Controller 方法，
 * 统一从 SecurityContext 取操作者、从请求取 IP/UA、从返回值/参数反射提取资源 id 与名称，
 * 写入 audit_log。审计写入失败不阻断业务。
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserMapper userMapper;

    /** userId -> 显示名 缓存，避免每次审计都查库 */
    private final ConcurrentHashMap<Long, String> userNameCache = new ConcurrentHashMap<Long, String>();

    @Around(value = "@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Object result = null;
        boolean success = true;
        try {
            result = pjp.proceed();
        } catch (Throwable e) {
            success = false;
            safeRecord(pjp, auditLog, null, false);
            throw e;
        }
        safeRecord(pjp, auditLog, result, success);
        return result;
    }

    private void safeRecord(ProceedingJoinPoint pjp, AuditLog auditLog, Object result, boolean success) {
        try {
            Long userId = SecurityUtils.currentUserIdSafe();
            Long workspaceId = SecurityUtils.currentWorkspaceIdSafe();
            String userName = resolveUserName(userId, result);
            String[] resource = resolveResource(result, pjp.getArgs());
            Long resourceId = parseLong(resource[0]);
            String resourceName = resource[1];

            HttpServletRequest request = currentRequest();
            String ip = request != null ? getClientIp(request) : null;
            String userAgent = request != null ? request.getHeader("User-Agent") : null;

            String detail = auditLog.label() + (resourceName != null ? ": " + resourceName : "");

            auditLogService.log(workspaceId, userId, userName,
                    auditLog.action(), auditLog.label(),
                    auditLog.resourceType(), resourceId, resourceName,
                    detail, success ? "success" : "failure",
                    ip, userAgent);
        } catch (Exception e) {
            // 审计失败绝不影响业务
            log.error("审计日志记录失败: action={}", auditLog.action(), e);
        }
    }

    /** 操作者显示名：优先缓存，其次查库；login 成功时从返回值兜底 */
    private String resolveUserName(Long userId, Object result) {
        if (userId != null) {
            String cached = userNameCache.get(userId);
            if (cached != null) {
                return cached;
            }
            User user = userMapper.selectById(userId);
            if (user != null) {
                String name = user.getNickname() != null ? user.getNickname() : user.getUsername();
                if (name != null) {
                    userNameCache.put(userId, name);
                }
                return name;
            }
        }
        if (result instanceof Result) {
            Object data = ((Result<?>) result).getData();
            if (data instanceof LoginVO) {
                return displayNameOfUser(((LoginVO) data).getUser());
            }
        }
        return null;
    }

    private String displayNameOfUser(Object user) {
        if (user == null) {
            return null;
        }
        String nick = readGetter(user, "nickname");
        return nick != null ? nick : readGetter(user, "username");
    }

    /**
     * 反射提取资源 id 与名称：
     * 1) login 返回 LoginVO -> user.id / nickname
     * 2) Result.data -> getId / getDisplayName|getName|getTitle|getUsername
     * 3) 兜底：参数中的 Long 作为 id；参数对象（form）的名称类字段作为名称
     */
    private String[] resolveResource(Object result, Object[] args) {
        String id = null;
        String name = null;

        if (result instanceof Result) {
            Object data = ((Result<?>) result).getData();
            if (data instanceof LoginVO) {
                Object user = ((LoginVO) data).getUser();
                if (user != null) {
                    id = readGetter(user, "id");
                    name = displayNameOfUser(user);
                }
            } else if (data != null) {
                id = readGetter(data, "id");
                name = readName(data);
            }
        }

        if (id == null) {
            for (Object arg : args) {
                if (arg instanceof Long) {
                    id = String.valueOf(arg);
                    break;
                }
            }
        }
        if (name == null) {
            for (Object arg : args) {
                if (arg == null || arg instanceof Number || arg instanceof CharSequence || arg instanceof Map) {
                    continue;
                }
                String n = readName(arg);
                if (n != null) {
                    name = n;
                    break;
                }
            }
        }
        return new String[]{id, name};
    }

    /** 尝试读取资源显示名：displayName 优先（工具取中文显示名），其次 name/title/username */
    private String readName(Object obj) {
        if (obj == null) {
            return null;
        }
        String[] fields = {"displayName", "name", "title", "username"};
        for (String fn : fields) {
            String v = readGetter(obj, fn);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /** 反射调用 getter（getXxx / isXxx），统一返回字符串 */
    private String readGetter(Object obj, String field) {
        if (obj == null || field == null || field.isEmpty()) {
            return null;
        }
        String upper = Character.toUpperCase(field.charAt(0)) + field.substring(1);
        String[] prefixes = {"get", "is"};
        for (String prefix : prefixes) {
            try {
                Method m = obj.getClass().getMethod(prefix + upper);
                Object v = m.invoke(obj);
                if (v != null) {
                    return String.valueOf(v);
                }
            } catch (NoSuchMethodException ignored) {
                // 尝试下一个前缀
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Long parseLong(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
