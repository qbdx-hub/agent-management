package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.User;
import com.agentmanagement.entity.Workflow;
import com.agentmanagement.entity.WorkflowEdge;
import com.agentmanagement.entity.WorkflowNode;
import com.agentmanagement.form.WorkflowCreateForm;
import com.agentmanagement.form.WorkflowQueryForm;
import com.agentmanagement.form.WorkflowSaveForm;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.mapper.WorkflowEdgeMapper;
import com.agentmanagement.mapper.WorkflowMapper;
import com.agentmanagement.mapper.WorkflowNodeMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.WorkflowService;
import com.agentmanagement.vo.WorkflowSummaryVO;
import com.agentmanagement.vo.WorkflowVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    private WorkflowNodeMapper workflowNodeMapper;

    @Autowired
    private WorkflowEdgeMapper workflowEdgeMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult<WorkflowSummaryVO> pageWorkflows(WorkflowQueryForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Page<Workflow> page = new Page<Workflow>(form.getPage(), form.getPageSize());
        LambdaQueryWrapper<Workflow> qw = new LambdaQueryWrapper<Workflow>();
        qw.eq(Workflow::getWorkspaceId, workspaceId);
        if (StringUtils.hasText(form.getKeyword())) {
            qw.like(Workflow::getName, form.getKeyword());
        }
        qw.eq(StringUtils.hasText(form.getStatus()), Workflow::getStatus, form.getStatus());
        qw.orderByDesc(Workflow::getUpdatedAt);

        Page<Workflow> result = workflowMapper.selectPage(page, qw);
        Map<Long, String> creatorNameMap = loadUserNames(
                result.getRecords().stream().map(Workflow::getCreatedBy).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<WorkflowSummaryVO> list = new ArrayList<WorkflowSummaryVO>();
        for (Workflow wf : result.getRecords()) {
            WorkflowSummaryVO vo = new WorkflowSummaryVO();
            vo.setId(wf.getId());
            vo.setName(wf.getName());
            vo.setDescription(wf.getDescription());
            vo.setStatus(wf.getStatus());
            vo.setNodeCount(countNodes(wf.getId()));
            vo.setCreatedBy(wf.getCreatedBy());
            vo.setCreatorName(creatorNameMap.get(wf.getCreatedBy()));
            vo.setUpdatedAt(wf.getUpdatedAt());
            list.add(vo);
        }
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public WorkflowVO getWorkflowDetail(Long id) {
        return toDetailVO(requireWorkflowInWorkspace(id));
    }

    @Override
    public WorkflowVO createWorkflow(WorkflowCreateForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long currentUserId = SecurityUtils.currentUserId();

        Workflow wf = new Workflow();
        wf.setWorkspaceId(workspaceId);
        wf.setCreatedBy(currentUserId);
        wf.setName(form.getName());
        wf.setDescription(form.getDescription());
        wf.setStatus("draft");
        wf.setTriggerType("manual");
        workflowMapper.insert(wf);

        return toDetailVO(workflowMapper.selectById(wf.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowVO saveWorkflow(Long id, WorkflowSaveForm form) {
        Workflow wf = requireWorkflowInWorkspace(id);

        // 1. 更新工作流主表
        Workflow update = new Workflow();
        update.setId(wf.getId());
        update.setName(form.getName());
        update.setDescription(form.getDescription());
        update.setStatus(StringUtils.hasText(form.getStatus()) ? form.getStatus() : wf.getStatus());
        workflowMapper.updateById(update);

        // 2. 全量替换节点：先删后插（落 position_x / position_y）
        workflowNodeMapper.delete(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getWorkflowId, wf.getId()));
        if (form.getNodes() != null) {
            for (WorkflowSaveForm.NodeForm nf : form.getNodes()) {
                WorkflowNode node = new WorkflowNode();
                node.setWorkflowId(wf.getId());
                node.setNodeId(nf.getNodeId());
                node.setType(nf.getType());
                node.setLabel(nf.getLabel());
                node.setAgentId(nf.getAgentId());
                node.setToolId(nf.getToolId());
                node.setConfig(nf.getConfig());
                node.setPositionX(nf.getPositionX());
                node.setPositionY(nf.getPositionY());
                workflowNodeMapper.insert(node);
            }
        }

        // 3. 全量替换边
        workflowEdgeMapper.delete(new LambdaQueryWrapper<WorkflowEdge>()
                .eq(WorkflowEdge::getWorkflowId, wf.getId()));
        if (form.getEdges() != null) {
            for (WorkflowSaveForm.EdgeForm ef : form.getEdges()) {
                WorkflowEdge edge = new WorkflowEdge();
                edge.setWorkflowId(wf.getId());
                edge.setEdgeId(ef.getEdgeId());
                edge.setSourceNodeId(ef.getSourceNodeId());
                edge.setTargetNodeId(ef.getTargetNodeId());
                edge.setLabel(ef.getLabel());
                edge.setCondition(ef.getCondition());
                workflowEdgeMapper.insert(edge);
            }
        }

        return toDetailVO(workflowMapper.selectById(wf.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWorkflow(Long id) {
        Workflow wf = requireWorkflowInWorkspace(id);
        workflowEdgeMapper.delete(new LambdaQueryWrapper<WorkflowEdge>()
                .eq(WorkflowEdge::getWorkflowId, wf.getId()));
        workflowNodeMapper.delete(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getWorkflowId, wf.getId()));
        workflowMapper.deleteById(wf.getId());
    }

    // ==================== 私有辅助 ====================

    private Workflow requireWorkflowInWorkspace(Long id) {
        Workflow wf = workflowMapper.selectById(id);
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        if (wf == null || !workspaceId.equals(wf.getWorkspaceId())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return wf;
    }

    private Integer countNodes(Long workflowId) {
        Long count = workflowNodeMapper.selectCount(
                new LambdaQueryWrapper<WorkflowNode>().eq(WorkflowNode::getWorkflowId, workflowId));
        return count == null ? 0 : count.intValue();
    }

    private WorkflowVO toDetailVO(Workflow wf) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(wf.getId());
        vo.setName(wf.getName());
        vo.setDescription(wf.getDescription());
        vo.setStatus(wf.getStatus());
        vo.setTriggerType(wf.getTriggerType());
        vo.setTriggerConfig(wf.getTriggerConfig());
        vo.setCreatedBy(wf.getCreatedBy());
        vo.setCreatorName(loadUserName(wf.getCreatedBy()));
        vo.setCreatedAt(wf.getCreatedAt());
        vo.setUpdatedAt(wf.getUpdatedAt());

        List<WorkflowNode> nodes = workflowNodeMapper.selectList(
                new LambdaQueryWrapper<WorkflowNode>().eq(WorkflowNode::getWorkflowId, wf.getId()));
        List<WorkflowVO.NodeVO> nodeVOs = new ArrayList<WorkflowVO.NodeVO>();
        for (WorkflowNode n : nodes) {
            WorkflowVO.NodeVO nv = new WorkflowVO.NodeVO();
            nv.setId(n.getId());
            nv.setNodeId(n.getNodeId());
            nv.setType(n.getType());
            nv.setLabel(n.getLabel());
            nv.setAgentId(n.getAgentId());
            nv.setToolId(n.getToolId());
            nv.setConfig(n.getConfig());
            nv.setPositionX(n.getPositionX());
            nv.setPositionY(n.getPositionY());
            nodeVOs.add(nv);
        }
        vo.setNodes(nodeVOs);

        List<WorkflowEdge> edges = workflowEdgeMapper.selectList(
                new LambdaQueryWrapper<WorkflowEdge>().eq(WorkflowEdge::getWorkflowId, wf.getId()));
        List<WorkflowVO.EdgeVO> edgeVOs = new ArrayList<WorkflowVO.EdgeVO>();
        for (WorkflowEdge e : edges) {
            WorkflowVO.EdgeVO ev = new WorkflowVO.EdgeVO();
            ev.setId(e.getId());
            ev.setEdgeId(e.getEdgeId());
            ev.setSourceNodeId(e.getSourceNodeId());
            ev.setTargetNodeId(e.getTargetNodeId());
            ev.setLabel(e.getLabel());
            ev.setCondition(e.getCondition());
            edgeVOs.add(ev);
        }
        vo.setEdges(edgeVOs);

        return vo;
    }

    private String loadUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }

    private Map<Long, String> loadUserNames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
    }
}
