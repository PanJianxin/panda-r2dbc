package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain.Sorting;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 *
 */
@Data
@NoArgsConstructor
public final class Sorter {
    private String field;
    private Sorting sorting;

    /**
     * @param field   排序字段，用哪个字段排序
     * @param sorting 排序，两个取值，ASC（正序）、DESC（倒序）
     *                默认正序排序
     */
    public Sorter(String field, Sorting sorting) {
        this.field = field;
        this.sorting = sorting;
    }

    public Sort.Order execute() {
        return getSorting().execute(getField());
    }

    public Sorting getSorting() {
        return sorting == null ? Sorting.ASC : sorting;
    }
}
