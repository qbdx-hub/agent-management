package com.agentmanagement.form;

import lombok.Data;

import java.util.Map;

/**
 * PUT /auth/preferences —— 用户偏好全量提交。
 * 结构由前端定义（默认模型/温度/最大输出/回复风格/通知开关），后端只做 JSON 透传存储，
 * 不做字段级校验（个人使用场景，宽松约定见《移动端接口文档》§1.5）。
 */
@Data
public class PreferencesForm {

    private Map<String, Object> preferences;
}
