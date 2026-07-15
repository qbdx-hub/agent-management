package com.agentmanagement.service;

import com.agentmanagement.entity.Document;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务。
 */
public interface DocumentService extends IService<Document> {

    /**
     * 上传文档：保存文件 → 写入 document 表 → 更新知识库统计。
     *
     * @param kbId    知识库 ID
     * @param file    上传的文件
     * @param userId  上传者 ID
     * @return 文档实体
     */
    Document upload(Long kbId, MultipartFile file, Long userId);

    /**
     * 查询知识库下的文档列表。
     */
    List<Document> listByKnowledgeBase(Long kbId);

    /**
     * 删除文档：删除记录 → 更新知识库统计。
     *
     * @param docId    文档 ID
     * @param kbId     知识库 ID（校验归属）
     */
    void deleteById(Long docId, Long kbId);
}
