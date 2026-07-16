package com.agentmanagement.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解：标注在 Controller 写方法上，由 {@link com.agentmanagement.common.aspect.AuditLogAspect}
 * 切面统一拦截并写入 audit_log 表。
 *
 * <pre>
 * &#64;AuditLog(action = "agent.create", label = "创建 Agent", resourceType = "agent")
 * public Result&lt;AgentVO&gt; create(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 操作类型，如 agent.create / tool.delete / knowledge.create / user.login */
    String action();

    /** 操作显示名，如 创建 Agent / 删除工具 */
    String label();

    /** 资源类型，如 agent / tool / workflow / knowledge_base / document / user */
    String resourceType();

    /**
     * 资源 id 的 SpEL 表达式（可选），如 "#id"、"#docId"；为空时自动从返回值/参数反射提取。
     * 用于删除等返回 Void 且有多个同类型参数的方法，精确定位资源 id。
     */
    String resourceIdExpr() default "";

    /**
     * 资源名称的 SpEL 表达式（可选），如 "#form.name"；为空时自动反射提取。
     */
    String resourceNameExpr() default "";
}
