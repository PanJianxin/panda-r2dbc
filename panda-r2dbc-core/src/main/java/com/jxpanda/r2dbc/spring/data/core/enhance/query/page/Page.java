package com.jxpanda.r2dbc.spring.data.core.enhance.query.page;

public interface Page {


    /**
     * 当前页
     */
    int getPageNumber();

    /**
     * 设置页码
     */
    void setPageNumber(int pageNum);

    /**
     * 每页长度
     */
    int getPageSize();

    /**
     * 设置页长
     */
    void setPageSize(int size);


    /**
     * 数据总量，使用了queryCount()后会返回，默认返回-1
     */
    default long getTotal() {
        return -1L;
    }

    /**
     * 是否做一次count查询，以获取总数据量
     */
    default boolean isNeedCount() {
        return true;
    }

    /**
     * 返回分页的偏移值，对应Query中的offset
     */
    default long getOffset() {
        return calculateOffset(getPageNumber(), getPageSize());
    }

    /**
     * 对应Query类的limit值，默认取值是size
     */
    default int getLimit() {
        return getPageSize();
    }

    static int calculateCurrent(int offset, int limit) {
        return offset / limit + 1;
    }

    static long calculateOffset(int pageNumber, int size) {
        if (pageNumber < 1L) {
            throw new RuntimeException("pageNumber must granter than 1");
        }
        return Math.max((pageNumber - 1) * size, 0);
    }

}
