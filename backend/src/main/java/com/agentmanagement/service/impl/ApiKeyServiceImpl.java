package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.ApiKey;
import com.agentmanagement.form.ApiKeyCreateForm;
import com.agentmanagement.mapper.ApiKeyMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.ApiKeyService;
import com.agentmanagement.vo.ApiKeyVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API 密钥服务实现。
 * 安全约定：明文不落库（只存 SHA-256 摘要），列表只返回掩码；
 * 密钥不可查询明文，泄露场景 = 删除后新建。
 */
@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    /** 每用户密钥上限（与移动端 UI 文案一致） */
    private static final int MAX_KEYS_PER_USER = 5;
    private static final String KEY_PREFIX = "sk-my-";

    @Autowired
    private ApiKeyMapper apiKeyMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public List<ApiKeyVO> listByCurrentUser() {
        Long userId = SecurityUtils.currentUserId();
        List<ApiKey> list = apiKeyMapper.selectList(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getUserId, userId)
                .orderByDesc(ApiKey::getCreatedAt));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ApiKeyVO create(ApiKeyCreateForm form) {
        Long userId = SecurityUtils.currentUserId();
        Long count = apiKeyMapper.selectCount(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getUserId, userId));
        if (count >= MAX_KEYS_PER_USER) {
            throw new BusinessException(ResultCode.API_KEY_LIMIT);
        }

        // 16 字节随机 → 32 位 hex，拼出 sk-my-xxxx... 明文
        byte[] raw = new byte[16];
        secureRandom.nextBytes(raw);
        StringBuilder hex = new StringBuilder(32);
        for (byte b : raw) {
            hex.append(String.format("%02x", b));
        }
        String plain = KEY_PREFIX + hex;
        String mask = KEY_PREFIX + "****-****-" + hex.substring(28);

        ApiKey entity = new ApiKey();
        entity.setUserId(userId);
        entity.setName(form.getName().trim());
        entity.setKeyHash(sha256Hex(plain));
        entity.setMask(mask);
        entity.setStatus("active");
        entity.setCreatedAt(LocalDateTime.now());
        apiKeyMapper.insert(entity);

        ApiKeyVO vo = toVO(entity);
        vo.setKey(plain); // 明文仅此一次
        return vo;
    }

    @Override
    public void updateStatus(Long id, boolean enabled) {
        ApiKey entity = requireOwned(id);
        entity.setStatus(enabled ? "active" : "disabled");
        apiKeyMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        requireOwned(id);
        apiKeyMapper.deleteById(id);
    }

    /** 取本人密钥，不存在/越权统一报 API_KEY_NOT_FOUND（不暴露存在性） */
    private ApiKey requireOwned(Long id) {
        Long userId = SecurityUtils.currentUserId();
        ApiKey entity = apiKeyMapper.selectById(id);
        if (entity == null || !entity.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.API_KEY_NOT_FOUND);
        }
        return entity;
    }

    private ApiKeyVO toVO(ApiKey entity) {
        ApiKeyVO vo = new ApiKeyVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setMask(entity.getMask());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setLastUsedAt(entity.getLastUsedAt());
        return vo;
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
