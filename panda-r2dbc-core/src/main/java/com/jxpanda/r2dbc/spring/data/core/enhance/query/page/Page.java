package com.jxpanda.r2dbc.spring.data.core.enhance.query.page;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Page {


    /**
     * 当前页
     */
    long getCurrent();

    /**
     * 设置页码
     */
    void setCurrent(long current);

    /**
     * 每页长度
     */
    int getSize();

    /**
     * 设置页长
     */
    void setSize(int size);


    /**
     * 数据总量，使用了queryCount()后会返回，默认返回-1
     */
    default long getTotal() {
        return -1L;
    }

    /**
     * 是否做一次count查询，以获取总数据量
     */
    default boolean isQueryCount() {
        return true;
    }

    /**
     * 返回分页的偏移值，对应Query中的offset
     */
    default long getOffset() {
        long current = getCurrent();
        if (current <= 1L) {
            return 0L;
        }
        return Math.max((current - 1) * getSize(), 0L);
    }

    /**
     * 对应Query类的limit值，默认取值是size
     */
    default int getLimit() {
        return getSize();
    }

}
