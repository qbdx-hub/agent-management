package com.agentmanagement.mapper;

import com.agentmanagement.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户 Mapper（靠启动类 @MapperScan 扫描，不加 @Mapper）。
 */
public interface UserMapper extends BaseMapper<User> {
}
