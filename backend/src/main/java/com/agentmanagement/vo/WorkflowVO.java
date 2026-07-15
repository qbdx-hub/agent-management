package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流详情 VO（编排画布加载用，含 nodes / edges）。
 */
@Data
public class WorkflowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String status;

    private String triggerType;

    private Map<String, Object> triggerConfig;

    private Long createdBy;

    private String creatorName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<NodeVO> nodes;

    private List<EdgeVO> edges;

    @Data
    public static class NodeVO {
        private Long id;
        /** 节点业务标识（前端 Vue Flow node.id） */
        private String nodeId;
        private String type;
        private String label;
        private Long agentId;
        private Long toolId;
        private Map<String, Object> config;
        private Double positionX;
        private Double positionY;
    }

    @Data
    public static class EdgeVO {
        private Long id;
        private String edgeId;
        private String sourceNodeId;
        private String targetNodeId;
        private String label;
        private Map<String, Object> condition;
    }
}
