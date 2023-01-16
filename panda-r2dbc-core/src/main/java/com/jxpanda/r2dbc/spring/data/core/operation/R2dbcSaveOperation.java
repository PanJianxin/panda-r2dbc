package com.jxpanda.r2dbc.spring.data.core.operation;

/**
 * 保存操作
 * 逻辑上有主键则更新，没有主键则插入
 */
public interface R2dbcSaveOperation {

    interface R2dbcSave<T> {

    }

}
