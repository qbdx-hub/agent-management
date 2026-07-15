package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型定价参考表实体（对应 model_pricing 表）。
 * 各模型每 1K token 的输入/输出单价参考，用于成本估算。
 * 注意：input_price_per_1k / output_price_per_1k 含数字 1k，
 * 驼峰自动映射会错位（per1k ≠ per_1k），必须显式 @TableField 指定列名。
 */
@Data
@TableName("model_pricing")
public class ModelPricing implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 提供商 */
    private String provider;

    private String modelName;

    private String displayName;

    private Integer maxTokens;

    /** 输入价格（每 1K token） */
    @TableField("input_price_per_1k")
    private BigDecimal inputPricePer1k;

    /** 输出价格（每 1K token） */
    @TableField("output_price_per_1k")
    private BigDecimal outputPricePer1k;

    /** 是否可用：0-否 1-是 */
    private Integer enabled;

    private LocalDateTime createdAt;
}
