package com.jxpanda.r2dbc.spring.data.core.enhance.query.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.With;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 入参用的分页参数对象
 * 简单封装后转为PageRequest对象
 * 这么做的原因是，原始的PageRequest对象页码是从0开始计算的，与直觉上不符
 */
@Data
@Setter
@AllArgsConstructor
public class PageParameter implements Page {


    private int pageNumber;
    private int pageSize;
    private boolean needCount;
    @With
    private Sort sort;

    public PageParameter(int pageNumber, int pageSize) {
        this(pageNumber, pageSize, true, Sort.unsorted());
    }

    public PageParameter(int pageNumber, int pageSize, Sort sort) {
        this(pageNumber, pageSize, true, sort);
    }

    public int getPageNumber() {
        return Math.max(pageNumber, 1);
    }

    public static PageParameter of(int pageNumber, int pageSize) {
        return new PageParameter(pageNumber, pageSize);
    }

    public PageRequest buildRequest() {
        // 由于PageRequest的页码是从0开始计算的，所以这里需要减1
        return PageRequest.of(getPageNumber() - 1, getPageSize(), getSort());
    }

}
