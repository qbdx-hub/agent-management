package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Document;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.form.KnowledgeBaseCreateForm;
import com.agentmanagement.mapper.DocumentMapper;
import com.agentmanagement.mapper.KnowledgeBaseMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.KnowledgeBaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库服务实现。
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    @Autowired
    private DocumentMapper documentMapper;

    /** 文件上传根目录 */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

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
        Long userId = SecurityUtils.currentUserId();
        return baseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getWorkspaceId, workspaceId)
                        .eq(KnowledgeBase::getCreatedBy, userId)
                        .orderByDesc(KnowledgeBase::getUpdatedAt));
    }

    @Override
    public KnowledgeBase getByIdChecked(Long id, Long workspaceId) {
        Long userId = SecurityUtils.currentUserId();
        KnowledgeBase kb = baseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getId, id)
                        .eq(KnowledgeBase::getWorkspaceId, workspaceId)
                        .eq(KnowledgeBase::getCreatedBy, userId));
        if (kb == null) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }
        return kb;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id, Long workspaceId) {
        // 1. 校验知识库存在且归属正确
        KnowledgeBase kb = getByIdChecked(id, workspaceId);

        // 2. 删除关联的文档记录和磁盘文件
        List<Document> documents = documentMapper.selectList(
                new LambdaQueryWrapper<Document>().eq(Document::getKnowledgeBaseId, id));
        for (Document doc : documents) {
            // 删除磁盘文件
            if (doc.getFileUrl() != null && !doc.getFileUrl().isEmpty()) {
                File file = new File(uploadDir, doc.getFileUrl());
                if (file.exists()) {
                    file.delete();
                }
            }
            // 删除文档记录
            documentMapper.deleteById(doc.getId());
        }

        // 3. 删除知识库记录
        baseMapper.deleteById(id);

        // 4. 删除磁盘上传目录
        File kbDir = new File(uploadDir, String.valueOf(id));
        if (kbDir.exists()) {
            deleteDirectory(kbDir);
        }

        log.info("知识库删除成功: id={}, name={}, workspaceId={}", id, kb.getName(), workspaceId);
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }
}
