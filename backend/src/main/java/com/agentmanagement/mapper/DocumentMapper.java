package com.agentmanagement.mapper;

import com.agentmanagement.entity.Document;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 文档 Mapper（靠启动类 @MapperScan 扫描，不加 @Mapper）。
 */
public interface DocumentMapper extends BaseMapper<Document> {
}
