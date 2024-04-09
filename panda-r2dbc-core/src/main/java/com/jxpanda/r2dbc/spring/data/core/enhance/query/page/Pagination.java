package com.jxpanda.r2dbc.spring.data.core.enhance.query.page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 分页返回值包装
 *
 * @param pageNumber    当前页页码
 * @param nextNumber    下一页页码
 * @param hasNext       是否有下一页
 * @param pageSize      每页长度
 * @param totalPages    共有多少页
 * @param totalElements 总数据条数
 * @param records       数据列表
 */
public record Pagination<T>(int pageNumber,
                            int nextNumber,
                            boolean hasNext,
                            int pageSize,
                            long totalPages,
                            long totalElements,
                            List<T> records) {




    /**
     * 重设记录（一版用于替换列表中的泛型，例如：entity->vo）
     */
    public <E> Pagination<E> records(List<E> records) {
        return this.<E>newBuilder()
                .records(records)
                .build();
    }

    /**
     * 泛型映射
     */
    public <E> Pagination<E> map(Function<T, E> mapper) {
        return this.<E>newBuilder()
                .records(this.records().stream().map(mapper).toList())
                .build();
    }

    /**
     * 泛型转换(强转)
     */
    public <E> Pagination<E> cast(Class<E> clazz) {
        return map(clazz::cast);
    }


    /**
     * 对列表进行分页
     *
     * @param pageNumber 当前页码
     * @param pageSize    每页长度
     * @param data    数据列表
     * @return 计算结束后的分页对象
     */
    public static <E> Pagination<E> paging(int pageNumber, int pageSize, List<E> data) {

        // 总数据长度
        var total = data == null ? 0 : data.size();
        // 计算起始偏移值
        var startIndex = Math.max((pageNumber - 1) * pageSize, 0);
        // 计算结束偏移值
        var endIndex = Math.min(startIndex + pageSize, total);
        // 使用计算好的偏移值，内存分页
        var records = (data == null || startIndex > endIndex) ? new ArrayList<E>() : data.subList(startIndex, endIndex);

        // 创建对象，设置各值
        return Pagination.<E>builder(pageNumber, pageSize)
                .totalElements((long) total)
                .records(records)
                .build();
    }

    /**
     * 计算一共有多少页
     */
    private static long calculatePages(int pageSize, long totalElements) {
        return totalElements / pageSize + (totalElements % pageSize > 0 ? 1 : 0);
    }

    private <R> Builder<R> newBuilder() {
        return Pagination.<R>builder(this.pageNumber(), this.pageSize())
                .hasNext(this.hasNext())
                .totalElements(this.totalElements());
    }

    public static <R> Builder<R> builder(int pageNumber, int pageSize) {
        return new Builder<>(pageNumber, pageSize);
    }

    public static <R> Builder<R> offsetBuilder(int offset, int limit) {
        return builder(Page.calculateCurrent(offset, limit), limit);
    }

    public static final class Builder<T> {
        private int pageNumber;
        private int pageSize;
        private Boolean hasNext;
        private Long totalElements;
        private List<T> records;

        public Builder(int pageNumber, int pageSize) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
        }

        public Builder<T> pageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public Builder<T> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }

        public Builder<T> totalElements(Long total) {
            this.totalElements = total;
            return this;
        }

        public Builder<T> records(List<T> records) {
            this.records = records;
            return this;
        }

        public Pagination<T> build() {
            if (this.records == null) {
                this.records = new ArrayList<>();
            }
            if (this.totalElements == null) {
                this.totalElements = (long) this.records.size();
            }
            if (this.hasNext == null) {
                this.hasNext = this.totalElements > (long) this.pageNumber * this.pageSize;
            }
            int next = hasNext ? pageNumber + 1 : -1;
            long pages = calculatePages(this.pageSize, this.totalElements);
            return new Pagination<>(this.pageNumber, next, this.hasNext, this.pageSize, pages, this.totalElements, this.records);
        }


    }

}
