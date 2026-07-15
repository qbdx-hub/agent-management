package com.agentmanagement.service;

import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.form.KnowledgeBaseCreateForm;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 知识库服务。
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 创建知识库：校验名称唯一 → 写入 knowledge_base 表。
     *
     * @param form      创建表单
     * @param userId    当前登录用户 ID
     * @param workspaceId 当前工作空间 ID
     * @return 新建知识库实体
     */
    KnowledgeBase create(KnowledgeBaseCreateForm form, Long userId, Long workspaceId);

    /**
     * 查询当前工作空间下的知识库列表。
     */
    List<KnowledgeBase> listByWorkspace(Long workspaceId);

    /**
     * 按 ID 查询知识库详情（校验归属）。
     *
     * @param id          知识库 ID
     * @param workspaceId 当前工作空间 ID
     * @return 知识库实体
     * @throws com.agentmanagement.common.BusinessException 知识库不存在或不属于当前工作空间
     */
    KnowledgeBase getByIdChecked(Long id, Long workspaceId);

    /**
     * 删除知识库：级联删除关联文档 → 删除知识库记录。
     *
     * @param id          知识库 ID
     * @param workspaceId 当前工作空间 ID（校验归属）
     */
    void deleteById(Long id, Long workspaceId);
}
