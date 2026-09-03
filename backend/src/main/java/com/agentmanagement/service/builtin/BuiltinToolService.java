package com.agentmanagement.service.builtin;

import com.agentmanagement.entity.Tool;

import java.util.Map;

/**
 * 内置工具（builtin 类型）执行服务：文件读写编辑、目录列举、内容搜索、命令执行、网页抓取与搜索。
 * 与 API 工具（ToolService.testTool 发 HTTP）并列的第二类工具面，让 Agent 能作用于真实世界。
 * 默认执行上下文是会话级沙箱目录（agent.sandbox.root/session-{sessionId}），路径越界一律拒绝；
 * 用户在聊天框显式授权「沙箱外运行」后，文件/命令工具切换到服务器真实文件系统与工作目录。
 */
public interface BuiltinToolService {

    /**
     * 按工具名分派执行（会话沙箱内）。
     *
     * @param tool        市场中的工具行（type=builtin，name 必须与内置处理器一一对应）
     * @param params      模型生成的调用参数
     * @param agentId     归因：发起调用的 Agent
     * @param sessionId   归因：所在会话（同时决定沙箱目录）
     * @param workspaceId 归因：工作空间
     */
    BuiltinToolResult execute(Tool tool, Map<String, Object> params,
                              Long agentId, Long sessionId, Long workspaceId);

    /**
     * 按工具名分派执行，可授权沙箱外运行。
     *
     * @param outsideSandbox true 时文件/命令工具作用于服务器真实文件系统（支持绝对路径），
     *                       命令工作目录为服务器进程目录；false 时等价于无参重载（会话沙箱内）
     */
    BuiltinToolResult execute(Tool tool, Map<String, Object> params,
                              Long agentId, Long sessionId, Long workspaceId, boolean outsideSandbox);
}
