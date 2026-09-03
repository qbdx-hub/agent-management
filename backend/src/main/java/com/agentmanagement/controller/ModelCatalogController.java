package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.entity.ModelPricing;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.agentmanagement.mapper.ModelPricingMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型目录接口 —— 返回系统真实接入（model_pricing 表中启用）的模型列表。
 * 供移动端「模型偏好」「新建 Agent」选择模型使用。
 */
@RestController
@RequestMapping("/models")
public class ModelCatalogController {

    @Autowired
    private ModelPricingMapper modelPricingMapper;

    /** GET /models —— 启用中的模型目录（按 provider、id 排序） */
    @GetMapping
    public Result<List<ModelPricing>> list() {
        List<ModelPricing> list = modelPricingMapper.selectList(
                new LambdaQueryWrapper<ModelPricing>()
                        .eq(ModelPricing::getEnabled, 1)
                        .orderByAsc(ModelPricing::getProvider)
                        .orderByAsc(ModelPricing::getId));
        return Result.success(list);
    }
}
