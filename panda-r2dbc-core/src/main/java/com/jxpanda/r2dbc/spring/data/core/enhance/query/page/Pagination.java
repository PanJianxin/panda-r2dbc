package com.jxpanda.r2dbc.spring.data.core.enhance.query.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Pagination<T> extends PageImpl<T> {

    public Pagination(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public Pagination(List<T> content) {
        this(content, PageRequest.of(1, content.size()), content.size());
    }

    public static Pageable of(int pageNumber, int pageSize) {
        return Request.of(pageNumber, pageSize).buildPageable();
    }


    @NonNull
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <U> Pagination<U> map(@NonNull Function<? super T, ? extends U> converter) {
        return ((Pagination) super.map(converter));
    }


    /**
     * 对列表进行分页
     *
     * @param pageNumber 当前页码
     * @param pageSize   每页长度
     * @param data       数据列表
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
        return new Pagination<>(records, PageRequest.of(pageNumber, pageSize), total);
    }

    /**
     * 计算一共有多少页
     */
    private static long calculatePages(int pageSize, long totalElements) {
        return totalElements / pageSize + (totalElements % pageSize > 0 ? 1 : 0);
    }

    public boolean getHasPrevious() {
        return super.hasPrevious();
    }

    public int getPrevious() {
        return Math.max(super.getNumber() - 1, 0);
    }

    public boolean getHasNext() {
        return super.hasNext();
    }

    public int getNext() {
        return Math.min(super.getNumber() + 1, super.getTotalPages());
    }

    @Override
    public int getTotalPages() {
        return super.getTotalPages();
    }

    @NonNull
    @Override
    @JsonIgnore
    public Pageable getPageable() {
        return super.getPageable();
    }

    @NonNull
    @Override
    @JsonIgnore
    public Sort getSort() {
        return super.getSort();
    }

    @Override
    @JsonIgnore
    public int getNumberOfElements() {
        return super.getNumberOfElements();
    }

    @Override
    @JsonProperty("pageSize")
    public int getSize() {
        return super.getSize();
    }

    @Override
    @JsonProperty("pageNumber")
    public int getNumber() {
        return super.getNumber() + 1;
    }

    @Override
    @JsonIgnore
    public boolean isFirst() {
        return super.isFirst();
    }

    @Override
    @JsonIgnore
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    @JsonIgnore
    public boolean isLast() {
        return super.isLast();
    }


    public record Request(int pageNumber, int pageSize) {

        public static Request of(int pageNumber, int pageSize) {
            return new Request(pageNumber, pageSize);
        }

        @JsonIgnore
        public Pageable buildPageable() {
            return PageRequest.of(pageNumber - 1, pageSize);
        }

        public static Request defaultPage() {
            return new Request(1, 10);
        }
    }

}
