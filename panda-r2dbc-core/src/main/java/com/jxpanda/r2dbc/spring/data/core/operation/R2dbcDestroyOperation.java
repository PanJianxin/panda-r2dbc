package com.jxpanda.r2dbc.spring.data.core.operation;

/**
 * 物理删除，提供在开启逻辑删除的情况下，还需要使用物理删除的场景使用
 */
public interface R2dbcDestroyOperation {

    <T> R2dbcDestroy<T> destroy(Class<T> domainType);

    interface R2dbcDestroy<T> extends R2dbcDeleteOperation.R2dbcDelete<T> {

    }

}
