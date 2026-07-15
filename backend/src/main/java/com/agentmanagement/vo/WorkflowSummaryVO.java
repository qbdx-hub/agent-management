package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流列表项 VO。
 */
@Data
public class WorkflowSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String status;

    private Integer nodeCount;

    private Long createdBy;

    private String creatorName;

    private LocalDateTime updatedAt;
}
