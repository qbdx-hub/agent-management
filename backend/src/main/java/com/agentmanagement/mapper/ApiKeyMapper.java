package com.agentmanagement.mapper;

import com.agentmanagement.entity.ApiKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 密钥表 Mapper。
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {
}
