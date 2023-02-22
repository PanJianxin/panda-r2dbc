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
     * @param current 当前页码
     * @param size    每页长度
     * @param data    数据列表
     * @return 计算结束后的分页对象
     */
    public static <E> Pagination<E> paging(int current, int size, List<E> data) {

        // 总数据长度
        var total = data == null ? 0 : data.size();
        // 计算起始偏移值
        var startIndex = Math.max((current - 1) * size, 0);
        // 计算结束偏移值
        var endIndex = Math.min(startIndex + size, total);
        // 使用计算好的偏移值，内存分页
        var records = (data == null || startIndex > endIndex) ? new ArrayList<E>() : data.subList(startIndex, endIndex);

        // 创建对象，设置各值
        return Pagination.<E>builder(current, size)
                .total((long) total)
                .records(records)
                .build();
    }

    /**
     * 计算一共有多少页
     */
    private static long calculatePages(int size, long total) {
        return total / size + (total % size > 0 ? 1 : 0);
    }

    private <R> Builder<R> newBuilder() {
        return Pagination.<R>builder(this.current(), this.size())
                .hasNext(this.hasNext())
                .total(this.total());
    }

    public static <R> Builder<R> builder(long current, int size) {
        return new Builder<>(current, size);
    }

    public static <R> Builder<R> offsetBuilder(long offset, int limit) {
        return builder(Page.calculateCurrent(offset, limit), limit);
    }

    public static final class Builder<T> {
        private final long current;
        private final int size;
        private Boolean hasNext;
        private Long total;
        private List<T> records;

        public Builder(long current, int size) {
            this.current = current;
            this.size = size;
        }

        public Builder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }

        public Builder<T> total(Long total) {
            this.total = total;
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
            if (this.total == null) {
                this.total = (long) this.records.size();
            }
            if (this.hasNext == null) {
                this.hasNext = this.total > this.current * this.size;
            }
            long next = hasNext ? current + 1 : -1;
            long pages = calculatePages(this.size, this.total);
            return new Pagination<>(this.current, next, this.hasNext, this.size, pages, this.total, this.records);
        }


    }

}
