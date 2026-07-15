package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * 工作流画布整体保存表单（对应前端编排画布的「保存」）。
 * nodes / edges 为全量覆盖：后端先按 workflow_id 删旧、再插新，保证画布与库一致。
 */
@Data
public class WorkflowSaveForm {

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 100, message = "工作流名称不能超过100字")
    private String name;

    @Size(max = 500, message = "描述不能超过500字")
    private String description;

    /** 状态：draft/active/archived */
    private String status;

    private List<NodeForm> nodes;

    private List<EdgeForm> edges;

    /** 画布节点（对应前端 Vue Flow 节点） */
    @Data
    public static class NodeForm {
        /** 节点业务标识（前端生成，Vue Flow node.id） */
        private String nodeId;
        /** 类型：start/agent/tool/condition/end/approval */
        private String type;
        private String label;
        private Long agentId;
        private Long toolId;
        private Map<String, Object> config;
        private Double positionX;
        private Double positionY;
    }

    /** 画布连线（对应前端 Vue Flow 边） */
    @Data
    public static class EdgeForm {
        private String edgeId;
        private String sourceNodeId;
        private String targetNodeId;
        private String label;
        private Map<String, Object> condition;
    }
}
