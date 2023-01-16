package com.jxpanda.r2dbc.spring.data.core.operation.support;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;

public class R2dbcSaveOperationSupport extends R2dbcOperationSupport {
    public R2dbcSaveOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }


    private final static class R2dbcSaveSupport<T> extends R2dbcSupport<T, T> {
        R2dbcSaveSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<T> returnType, Query query, SqlIdentifier tableName) {
            super(template, domainType, returnType, query, tableName);
        }


    }


}
