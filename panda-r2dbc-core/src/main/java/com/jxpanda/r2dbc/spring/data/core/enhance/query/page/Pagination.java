package com.jxpanda.r2dbc.spring.data.core.enhance.query.page;

import java.util.List;

/**
 * 分页返回值包装
 */
public record Pagination<T>(long current, int size, long total, List<T> records) {

    public static <T> Pagination<T> with(long offset, int limit, long total, List<T> records) {
        long current = offset / limit + 1;
        return new Pagination<>(current, limit, total, records);
    }
}
