package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Document;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.mapper.DocumentMapper;
import com.agentmanagement.mapper.KnowledgeBaseMapper;
import com.agentmanagement.service.DocumentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 文档服务实现。
 */
@Slf4j
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    /** 文件上传根目录 */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 允许的文件扩展名 */
    private static final String[] ALLOWED_EXTENSIONS = {
            "pdf", "md", "txt", "json", "js", "ts", "jsx", "tsx",
            "py", "java", "go", "rs", "c", "cpp", "h", "hpp",
            "css", "scss", "less", "html", "xml", "yaml", "yml",
            "sh", "bat", "sql"
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document upload(Long kbId, MultipartFile file, Long userId) {
        // 1. 校验知识库存在
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }

        // 2. 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }

        // 3. 校验文件格式
        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);
        if (!isAllowedExtension(extension)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件格式: " + extension);
        }

        // 4. 保存文件到磁盘
        String savedPath = saveFile(file, kbId);

        // 5. 写入 document 表
        Document doc = new Document();
        doc.setKnowledgeBaseId(kbId);
        doc.setName(originalName);
        doc.setFileType(extension);
        doc.setFileSize(file.getSize());
        doc.setFileUrl(savedPath);
        doc.setChunkCount(0);
        doc.setTotalTokens(0L);
        doc.setStatus("pending");
        doc.setUploadedBy(userId);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        baseMapper.insert(doc);

        // 6. 更新知识库文档计数
        updateKbDocumentCount(kbId);

        log.info("文档上传成功: id={}, name={}, kbId={}", doc.getId(), originalName, kbId);
        return doc;
    }

    @Override
    public List<Document> listByKnowledgeBase(Long kbId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, kbId)
                        .orderByDesc(Document::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long docId, Long kbId) {
        Document doc = baseMapper.selectOne(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getId, docId)
                        .eq(Document::getKnowledgeBaseId, kbId));
        if (doc == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "文档不存在");
        }

        // 删除磁盘文件
        deleteFile(doc.getFileUrl());

        // 删除记录
        baseMapper.deleteById(docId);

        // 更新知识库文档计数
        updateKbDocumentCount(kbId);

        log.info("文档删除成功: id={}, name={}", docId, doc.getName());
    }

    // ==================== 内部方法 ====================

    /**
     * 保存文件到磁盘，返回相对路径
     * 目录结构: uploads/{kbId}/{yyyyMMdd}/{uuid}.{ext}
     */
    private String saveFile(MultipartFile file, Long kbId) {
        String ext = getFileExtension(file.getOriginalFilename());
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String relativePath = kbId + "/" + dateStr + "/" + uuid + "." + ext;

        File dest = new File(uploadDir, relativePath);
        // 确保父目录存在
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件保存失败: {}", relativePath, e);
            throw new BusinessException(500, "文件保存失败");
        }

        return relativePath;
    }

    /**
     * 删除磁盘文件
     */
    private void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return;
        File file = new File(uploadDir, relativePath);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 更新知识库文档计数
     */
    private void updateKbDocumentCount(Long kbId) {
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<Document>().eq(Document::getKnowledgeBaseId, kbId));
        KnowledgeBase update = new KnowledgeBase();
        update.setId(kbId);
        update.setDocumentCount(count.intValue());
        update.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(update);
    }

    /**
     * 获取文件扩展名（小写，不含点号）
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 校验扩展名是否在白名单
     */
    private boolean isAllowedExtension(String ext) {
        if (ext == null || ext.isEmpty()) return false;
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(ext)) return true;
        }
        return false;
    }
}
