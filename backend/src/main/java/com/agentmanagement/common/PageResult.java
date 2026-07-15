package com.agentmanagement.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结构：{ list, total, page, pageSize }。
 * 字段名与前端 frontend/src/types/common.ts 的 PaginatedData&lt;T&gt; 严格对齐。
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;

    private Long total;

    private Long page;

    private Long pageSize;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total, Long page, Long pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    /** list 已转成 VO 后，由原始总数与分页参数构造 */
    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResult<T>(list, total, page, pageSize);
    }

    /** 由 MyBatis-Plus 的 IPage 直接构造（list 已转成 VO） */
    public static <T> PageResult<T> of(IPage<?> pageResult, List<T> list) {
        return new PageResult<T>(list, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }
}
