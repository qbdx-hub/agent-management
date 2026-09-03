package com.agentmanagement.service;

import com.agentmanagement.form.ApiKeyCreateForm;
import com.agentmanagement.vo.ApiKeyVO;

import java.util.List;

/**
 * API 密钥服务（移动端「API 密钥管理」屏）。
 */
public interface ApiKeyService {

    /** 当前用户的密钥列表（只含掩码，按创建时间倒序） */
    List<ApiKeyVO> listByCurrentUser();

    /**
     * 新建密钥：明文仅在本返回值中出现一次，服务端只落 SHA-256 摘要。
     * 每用户上限 5 个。
     */
    ApiKeyVO create(ApiKeyCreateForm form);

    /** 启用/停用（仅本人密钥可操作） */
    void updateStatus(Long id, boolean enabled);

    /** 删除（仅本人密钥可操作） */
    void delete(Long id);
}
