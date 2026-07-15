package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.form.KnowledgeBaseCreateForm;
import com.agentmanagement.mapper.KnowledgeBaseMapper;
import com.agentmanagement.service.KnowledgeBaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库服务实现。
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    @Override
    public KnowledgeBase create(KnowledgeBaseCreateForm form, Long userId, Long workspaceId) {
        // 1. 名称唯一校验（同工作空间内）
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getWorkspaceId, workspaceId)
                        .eq(KnowledgeBase::getName, form.getName()));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.KB_NAME_EXISTS);
        }

        // 2. 组装实体
        KnowledgeBase kb = new KnowledgeBase();
        kb.setWorkspaceId(workspaceId);
        kb.setName(form.getName());
        kb.setDescription(form.getDescription());
        kb.setType(form.getType() != null ? form.getType() : "vector");
        kb.setEmbeddingModel(form.getEmbeddingModel());
        kb.setDocumentCount(0);
        kb.setTotalTokens(0L);
        kb.setStatus("active");
        kb.setConfig(form.getConfig());
        kb.setCreatedBy(userId);
        kb.setCreatedAt(LocalDateTime.now());
        kb.setUpdatedAt(LocalDateTime.now());

        // 3. 写入
        baseMapper.insert(kb);
        log.info("知识库创建成功: id={}, name={}, workspaceId={}", kb.getId(), kb.getName(), workspaceId);
        return kb;
    }

    @Override
    public List<KnowledgeBase> listByWorkspace(Long workspaceId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getWorkspaceId, workspaceId)
                        .orderByDesc(KnowledgeBase::getUpdatedAt));
    }

    @Override
    public KnowledgeBase getByIdChecked(Long id, Long workspaceId) {
        KnowledgeBase kb = baseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getId, id)
                        .eq(KnowledgeBase::getWorkspaceId, workspaceId));
        if (kb == null) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }
        return kb;
    }
}
