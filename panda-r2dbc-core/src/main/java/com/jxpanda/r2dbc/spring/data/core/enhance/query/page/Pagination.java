package com.jxpanda.r2dbc.spring.data.core.enhance.query.page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 分页返回值包装
 *
 * @param current 当前页页码
 * @param next    下一页页码
 * @param hasNext 是否有下一页
 * @param size    每页长度
 * @param pages   共有多少页
 * @param total   总数据条数
 * @param records 数据列表
 */
public record Pagination<T>(long current,
                            long next,
                            boolean hasNext,
                            int size,
                            long pages,
                            long total,
                            List<T> records) {

    public Pagination(long current, int size, long total, List<T> records) {
        this(current, total > current * size, size, total, records);
    }

    public Pagination(long current, boolean hasNext, int size, long total, List<T> records) {
        this(current, hasNext ? current + 1 : -1, hasNext, size, calculatePages(size, total), total, records);
    }

    public Pagination<T> hasNext(boolean hasNext) {
        return new Pagination<>(this.current(), this.next(), hasNext, this.size(), this.pages(), this.total(), this.records());
    }

    /**
     * 泛型映射
     */
    public <R> Pagination<R> map(Function<T, R> mapper) {
        return new Pagination<>(this.current(), this.next(), this.hasNext(), this.size(), this.pages(), this.total(), this.records().stream().map(mapper).toList());
    }

    /**
     * 泛型转换
     */
    public <R> Pagination<R> cast(Class<R> clazz) {
        return map(clazz::cast);
    }


    /**
     * 对列表进行分页
     *
     * @param current 当前页码
     * @param size    每页长度
     * @param data    数据列表
     * @return 计算结束后的分页对象
     */
    public static <E> Pagination<E> paging(int current, int size, List<E> data) {

        // 总数据长度
        var total = data.size();
        // 计算起始偏移值
        var startIndex = Math.max((current - 1) * size, 0);
        // 计算结束偏移值
        var endIndex = Math.min(startIndex + size, total);
        // 使用计算好的偏移值，内存分页
        var records = startIndex > endIndex ? new ArrayList<E>() : data.subList(startIndex, endIndex);

        // 创建对象，设置各值
        return new Pagination<>(current, size, total, records);
    }


    /**
     * 计算一共有多少页
     */
    private static long calculatePages(int size, long total) {
        return total / size + (total % size > 0 ? 1 : 0);
    }


}
